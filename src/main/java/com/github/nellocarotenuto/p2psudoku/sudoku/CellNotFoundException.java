package com.github.nellocarotenuto.p2psudoku.sudoku;

/**
 * Models an exception thrown when attempting to modify a cell outside the board.
 */
public class CellNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "This cell doesn't exist";

    public CellNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public CellNotFoundException(String message) {
        super(message);
    }

}
