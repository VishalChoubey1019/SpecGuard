package com.specguard.repo;


/** Wraps checked {@link java.io.IOException}s from the GitHub API client as unchecked. */
public class GithubApiException extends RuntimeException {
    public GithubApiException(String message, Throwable cause) {
        super(message, cause);
    }
}