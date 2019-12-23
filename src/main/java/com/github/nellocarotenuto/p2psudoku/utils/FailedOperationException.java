package com.github.nellocarotenuto.p2psudoku.utils;

/**
 * Models an exception thrown when something goes wrong when operating on a DHT.
 */
public class FailedOperationException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "DHT operation failed.";

    public FailedOperationException() {
        super(DEFAULT_MESSAGE);
    }

    public FailedOperationException(String message) {
        super(message);
    }

}
