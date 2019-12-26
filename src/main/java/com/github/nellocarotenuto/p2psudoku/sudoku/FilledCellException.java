package com.github.nellocarotenuto.p2psudoku.sudoku;

/**
 * Models an exception thrown when attempting to fill a cell that already has a value.
 */
public class FilledCellException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "This cell is already filled";

    public FilledCellException() {
        super(DEFAULT_MESSAGE);
    }

    public FilledCellException(String message) {
        super(message);
    }

}
