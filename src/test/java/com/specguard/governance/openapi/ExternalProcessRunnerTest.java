package com.specguard.governance.openapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ExternalProcessRunnerTest {

    private ExternalProcessRunner runner;

    @BeforeEach
    void setUp() {
        runner = new ExternalProcessRunner();
    }

    @Test
    void run_returnsStdout_whenCommandSucceeds() {
        String output = runner.run(List.of(
                "sh",
                "-c",
                "echo hello"));

        assertThat(output.strip()).isEqualTo("hello");
    }

    @Test
    void run_preservesMultiLineOutput() {
        String output = runner.run(List.of(
                "sh",
                "-c",
                "printf 'one\\ntwo\\nthree'"));

        assertThat(output)
                .contains("one")
                .contains("two")
                .contains("three");
    }

    @Test
    void run_throwsWhenCommandDoesNotExist() {
        assertThatThrownBy(() ->
                runner.run(List.of("this-command-does-not-exist")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to start command");
    }

    @Test
    void run_throwsWhenCommandReturnsNonZeroExitCode() {
        assertThatThrownBy(() ->
                runner.run(List.of(
                        "sh",
                        "-c",
                        "echo boom >&2; exit 1")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("External command failed")
                .hasMessageContaining("boom");
    }

    @Test
    void run_returnsStdoutEvenWhenStdErrIsPresent_ifExitCodeIsZero() {
        String output = runner.run(List.of(
                "sh",
                "-c",
                "echo warning >&2; echo success"));

        assertThat(output.strip()).isEqualTo("success");
    }
}