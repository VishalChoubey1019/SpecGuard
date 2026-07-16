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
     * Executes an external command (e.g. oasdiff) and returns its stdout.
     *
     * @param command command and arguments
     * @return stdout produced by the process
     * @throws RuntimeException if the command fails to start, times out, exits
     *         non-zero, or the waiting thread is interrupted.
     */
    public String run(List<String> command) {
        log.debug("Running external command: {}", String.join(" ", command));

        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(false)
                    .start();

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException(
                        "Command timed out after " + TIMEOUT_SECONDS + "s: " + command);
            }

            String stdout = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);

            String stderr = new String(
                    process.getErrorStream().readAllBytes(),
                    StandardCharsets.UTF_8);

            int exitCode = process.exitValue();

            log.debug("Command exited with code {}", exitCode);

            if (!stdout.isBlank()) {
                log.debug("Command stdout:\n{}", stdout);
            }

            if (!stderr.isBlank()) {
                log.debug("Command stderr:\n{}", stderr);
            }

            if (exitCode != 0) {
                throw new RuntimeException(
                    "External command failed with exit code "
                            + exitCode
                            + "\nCommand: "
                            + String.join(" ", command)
                            + "\nStderr:\n"
                            + stderr);
            }

            return stdout;

        } catch (IOException e) {
            throw new RuntimeException("Failed to start command: " + command, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Interrupted while waiting on command: " + command, e);
        }
    }
}