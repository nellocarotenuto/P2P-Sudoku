package com.github.nellocarotenuto.p2psudoku.sudoku;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Models a cell of the Sudoku board.
 */
class Cell implements Serializable {

    private static final long serialVersionUID = 6973759943076793815L;

    private int value;
    private int correctValue;
    private boolean fixed;

    private Set<Group> groups;

    /**
     * Creates a new cell.
     */
    Cell() {
        groups = new HashSet<Group>();
        value = Sudoku.EMPTY_VALUE;
    }

    /**
     * Gets the current value of the cell.
     *
     * @return the current value of the cell
     */
    int getValue() {
        return value;
    }

    /**
     * Sets the value for the cell.
     *
     * @param value the value to set for the cell
     *
     * @throws InvalidNumberException if the value is not correct
     */
    void setValue(int value) throws InvalidNumberException {
        this.value = value;

        for (Group group : groups) {
            if (!group.isValid()) {
                this.value = Sudoku.EMPTY_VALUE;
                throw new InvalidNumberException("Group contains duplicates, constraints violated.");
            }
        }
    }

    /**
     * Tells whether the cell is fixed or not.
     *
     * @return true if the cell is fixed, false otherwise
     */
    boolean isFixed() {
        return fixed;
    }

    /**
     * Sets the fixed status for the cell.
     *
     * @param fixed true if the cell is to be marked as fixed, false otherwise
     */
    void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    /**
     * Adds the cell to a validation group and vice versa.
     *
     * @param group the group to add the cell to
     */
    void addGroup(Group group) {
        if (!groups.contains(group)) {
            groups.add(group);
            group.addCell(this);
        }
    }

    /**
     * Gets the correct value for the cell.
     *
     * @return the correct value for the cell
     */
    int getCorrectValue() {
        return correctValue;
    }

    /**
     * Sets the correct value for the cell.
     *
     * @param correctValue the correct value for the cell
     */
    void setCorrectValue(int correctValue) {
        this.correctValue = correctValue;
    }

}
