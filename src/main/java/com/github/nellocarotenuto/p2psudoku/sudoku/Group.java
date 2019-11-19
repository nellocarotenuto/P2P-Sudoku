package com.github.nellocarotenuto.p2psudoku.sudoku;

import java.util.*;

/**
 * Models a group of cells (like rows, columns and regions) of a Sudoku board.
 */
class Group {

    private Set<Cell> cells;

    Group() {
        cells = new HashSet<Cell>();
    }

    void addCell(Cell cell) {
        if (!cells.contains(cell)) {
            cells.add(cell);
            cell.addGroup(this);
        }
    }

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
