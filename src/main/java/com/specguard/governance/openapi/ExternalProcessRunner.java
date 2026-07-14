package com.specguard.governance.openapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs an external CLI tool and captures its stdout. Used to wrap {@code oasdiff} rather than
 * reimplementing OpenAPI diffing
 */
@Component
@Slf4j
public class ExternalProcessRunner {

    private static final long TIMEOUT_SECONDS = 30;

    /**
     * @param command the command and its arguments, e.g. {@code ["oasdiff", "breaking", ...]}
     * @return everything the process wrote to stdout
     * @throws RuntimeException if the process can't be started, times out, or the underlying
     *                          thread is interrupted while waiting on it. Not a custom type:
     *                          nothing in this codebase catches process failures differently
     *                          from any other analyzer failure — see safeAnalyzeFile in
     *                          OpenApiDiffAnalyzer, which catches RuntimeException generically.
     */
    public String run(List<String> command) {
        log.debug("Running external command: {}", String.join(" ", command));
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(false)
                    .start();

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Command timed out after " + TIMEOUT_SECONDS + "s: " + command);
            }

            if (!stderr.isBlank()) {
                log.debug("Command stderr: {}", stderr);
            }

            return stdout;
        } catch (IOException e) {
            throw new RuntimeException("Failed to start command: " + command, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting on command: " + command, e);
        }
    }
}