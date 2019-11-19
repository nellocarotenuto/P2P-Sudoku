package com.github.nellocarotenuto.p2psudoku.sudoku;

/**
 * Models an exception to be thrown when referring to an unexisting cell.
 */
public class UnexistingCellException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "This cell doesn't exist.";

    public UnexistingCellException() {
        super(DEFAULT_MESSAGE);
    }

}
