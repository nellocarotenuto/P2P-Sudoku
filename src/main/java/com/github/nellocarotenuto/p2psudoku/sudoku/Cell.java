package com.github.nellocarotenuto.p2psudoku.sudoku;

import java.util.HashSet;
import java.util.Set;

/**
 * Models a cell into of the Sudoku board.
 */
class Cell {

    static final int EMPTY = 0;

    private int value;
    private int correctValue;
    private boolean fixed;

    private Set<Group> groups;

    Cell() {
        groups = new HashSet<Group>();
        value = EMPTY;
    }

    int getValue() {
        return value;
    }

    void setValue(int value) throws ValidationException {
        this.value = value;

        for (Group group : groups) {
            if (!group.isValid()) {
                this.value = EMPTY;
                throw new ValidationException("Group contains duplicates, constraints violated.");
            }
        }
    }

    boolean isFixed() {
        return fixed;
    }

    void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    void addGroup(Group group) {
        if (!groups.contains(group)) {
            groups.add(group);
            group.addCell(this);
        }
    }

    int getCorrectValue() {
        return correctValue;
    }

    void setCorrectValue(int correctValue) {
        this.correctValue = correctValue;
    }

}
