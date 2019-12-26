package com.github.nellocarotenuto.p2psudoku.sudoku;

/**
 * Models an exception thrown when attempting to fill a fixed cell.
 */
public class FixedCellException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "The value of this cell is fixed and cannot be changed.";

    public FixedCellException() {
        super(DEFAULT_MESSAGE);
    }

    public FixedCellException(String message) {
        super(message);
    }

}
