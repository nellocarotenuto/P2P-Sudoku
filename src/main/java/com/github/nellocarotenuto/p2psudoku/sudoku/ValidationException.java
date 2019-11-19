package com.github.nellocarotenuto.p2psudoku.sudoku;

/**
 * Models an exception to be thrown when an attemp to place a number that violates the constraints is made.
 */
public class ValidationException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "This number violates Sudoku constraints.";

    public ValidationException() {
        super(DEFAULT_MESSAGE);
    }

    public ValidationException(String message) {
        super(message);
    }

}
