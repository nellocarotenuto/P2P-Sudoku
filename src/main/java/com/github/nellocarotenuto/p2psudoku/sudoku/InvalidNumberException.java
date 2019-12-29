package com.github.nellocarotenuto.p2psudoku.sudoku;

/**
 * Models an exception to thrown when attempting to place a number that violates the constraints.
 */
public class InvalidNumberException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "This number violates Sudoku constraints.";

    public InvalidNumberException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidNumberException(String message) {
        super(message);
    }

}
