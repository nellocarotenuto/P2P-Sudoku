package com.github.nellocarotenuto.p2psudoku.sudoku;

import java.io.Serializable;
import java.util.*;

/**
 * Models a validation group of cells like rows, columns and regions of a Sudoku board.
 */
class Group implements Serializable {

    private static final long serialVersionUID = 7082108749608217566L;

    private Set<Cell> cells;

    /**
     * Creates a new group.
     */
    Group() {
        cells = new HashSet<Cell>();
    }

    /**
     * Adds a cell to the group and vice versa.
     *
     * @param cell the cell to add to the group
     */
    void addCell(Cell cell) {
        if (!cells.contains(cell)) {
            cells.add(cell);
            cell.addGroup(this);
        }
    }

    /**
     * Tells whether the status of the cells in the group is legal or not.
     *
     * @return true if the numbers placed in the cells respect the constraints, false otherwise
     */
    boolean isValid() {
        List<Integer> valuesList = new ArrayList<Integer>();

        for (Cell cell : cells) {
            int value = cell.getValue();

            if (value != 0) {
                valuesList.add(cell.getValue());
            }
        }

        Set<Integer> valuesSet = new HashSet<Integer>(valuesList);

        return valuesSet.size() == valuesList.size();
    }

}
