package com.github.nellocarotenuto.p2psudoku.challenge;

/**
 * Models an exception to be thrown when the user tries to perform an operation he doesn't have permissions for.
 */
public class UnauthorizedOperationException extends RuntimeException {

    private static final String DEFAULT_NAME = "You do not have the required permissions to perform this operation.";

    public UnauthorizedOperationException() {
        super(DEFAULT_NAME);
    }

    public UnauthorizedOperationException(String message) {
        super(message);
    }

}
