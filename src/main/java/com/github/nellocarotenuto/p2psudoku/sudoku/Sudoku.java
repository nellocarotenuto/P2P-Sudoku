package com.github.nellocarotenuto.p2psudoku.sudoku;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.javatuples.Pair;

/**
 * Models a Sudoku board.
 */
public class Sudoku implements Serializable {

    private static final long serialVersionUID = -8725979312700736292L;

    public static final int EMPTY_VALUE = 0;
    public static final int SIDE_SIZE = 9;
    public static final int REGION_SIZE = (int) Math.sqrt(SIDE_SIZE);

    private static final int BOARD_SIZE = (int) Math.pow(SIDE_SIZE, 2);

    private static final int MIN_CLUES = 26;
    private static final int MAX_CLUES = 34;

    private static final List<Integer> values = IntStream.rangeClosed(1, SIDE_SIZE).boxed().collect(Collectors.toList());

    private Cell[][] board;

    /**
     * Generates a new random board.
     *
     * @param seed the seed for the internal random number generator for deterministic generation, null for a random
     *             board
     */
    public Sudoku(Integer seed) {
        // Build the (empty) board
        board = new Cell[SIDE_SIZE][SIDE_SIZE];

        // Initialize the random number generator
        Random random = seed == null ? new Random() : new Random(seed);

        // Define and initialize cells and validation groups
        Group[] rows = new Group[SIDE_SIZE];
        Group[] columns = new Group[SIDE_SIZE];
        Group[][] regions = new Group[REGION_SIZE][REGION_SIZE];

        for (int position = 0; position < SIDE_SIZE; position++) {
            rows[position] = new Group();
            columns[position] = new Group();

            int row = position / REGION_SIZE;
            int column = position % REGION_SIZE;

            regions[row][column] = new Group();
        }

        for (int row = 0; row < SIDE_SIZE; row++) {
            for (int column = 0; column < SIDE_SIZE; column++) {
                Cell cell = new Cell();

                board[row][column] = cell;
                rows[row].addCell(cell);
                columns[column].addCell(cell);
                regions[row / REGION_SIZE][column / REGION_SIZE].addCell(cell);
            }
        }

        // Associate to each cell a randomly ordered list of candidate values
        List<Pair<Cell, List<Pair<Integer, Boolean>>>> cellsToFill = new ArrayList<>(BOARD_SIZE);

        for (int row = 0; row < SIDE_SIZE; row++) {
            for (int column = 0; column < SIDE_SIZE; column++) {
                Cell cell = board[row][column];

                List<Pair<Integer, Boolean>> candidates = new ArrayList<>(SIDE_SIZE);

                for (Integer value : values) {
                    candidates.add(new Pair<>(value, false));
                }

                Collections.shuffle(candidates, random);

                Pair<Cell, List<Pair<Integer, Boolean>>> cellValuesPair = new Pair<>(cell, candidates);
                cellsToFill.add(cellValuesPair);
            }
        }

        // Fill the cells one by one with the first candidate that fits and backtrack if no more options are available
        int cellIndex = 0;
        int candidateIndex;

        while (cellIndex < cellsToFill.size()) {
            Pair<Cell, List<Pair<Integer, Boolean>>> pair = cellsToFill.get(cellIndex);

            Cell cell = pair.getValue0();
            List<Pair<Integer, Boolean>> candidates = pair.getValue1();

            for (candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
                Pair<Integer, Boolean> candidate = candidates.get(candidateIndex);

                if (!candidate.getValue1()) {
                    candidates.set(candidateIndex, candidate.setAt1(true));

                    try {
                        cell.setValue(candidate.getValue0());
                        cellIndex++;
                        break;
                    } catch (InvalidValueException e) {
                        continue;
                    }
                }
            }

            if (candidateIndex == candidates.size()) {
                cell.setValue(EMPTY_VALUE);

                for (candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
                    candidates.set(candidateIndex, candidates.get(candidateIndex).setAt1(false));
                }

                cellIndex--;
            }
        }

        // Copy the generated board into the solution matrix
        for (int row = 0; row < SIDE_SIZE; row++) {
            for (int column = 0; column < SIDE_SIZE; column++) {
                board[row][column].setCorrectValue(board[row][column].getValue());
            }
        }

        // Remove values from random cells but keep at least one occurrence for each value
        List<Cell> cellsToClear = new ArrayList<>();

        for (int row = 0; row < SIDE_SIZE; row++) {
            for (int column = 0; column < SIDE_SIZE; column++) {
                cellsToClear.add(board[row][column]);
            }
        }

        Collections.shuffle(cellsToClear, random);

        int cluesToRemove = BOARD_SIZE - (MIN_CLUES + random.nextInt(MAX_CLUES - MIN_CLUES));
        HashMap<Integer, Integer> occurrencesRemoved = new HashMap<>();

        for (int i = 0; i < cluesToRemove; i++) {
            Cell cell = cellsToClear.get(i);
            int value = cell.getValue();
            int timesRemoved = occurrencesRemoved.getOrDefault(value, 0);

            if (timesRemoved < SIDE_SIZE - 1) {
                cell.setValue(EMPTY_VALUE);
                occurrencesRemoved.put(value, timesRemoved + 1);
            }
        }

        // Mark the clues remaining as fixed
        for (int row = 0; row < SIDE_SIZE; row++) {
            for (int column = 0; column < SIDE_SIZE; column++) {
                Cell cell = board[row][column];

                if (cell.getValue() != EMPTY_VALUE) {
                    cell.setFixed(true);
                }
            }
        }

    }

    /**
     * Returns the current board.
     *
     * @return the matrix representation of the current board
     */
    public Integer[][] getBoard() {
        Integer[][] board = new Integer[SIDE_SIZE][SIDE_SIZE];

        for (int row = 0; row < SIDE_SIZE; row++) {
            for (int column = 0; column < SIDE_SIZE; column++) {
                board[row][column] = this.board[row][column].getValue();
            }
        }

        return board;
    }

    /**
     * Places a number into the board.
     *
     * @param row the row index (starting at 0) of the cell where to insert the number
     * @param column the column index (starting at 0) of the cell where to insert the number
     * @param number the number to put into the cell
     *
     * @throws FilledCellException if the cell is already filled
     * @throws InvalidValueException if the value doesn't fit into the cell
     */
    public void placeNumber(int row, int column, int number) throws FilledCellException, InvalidValueException {
        if (row < 0 || row >= SIDE_SIZE || column < 0 || column >= SIDE_SIZE) {
            throw new RuntimeException("This cell doesn't exist");
        }

        Cell cell = board[row][column];

        if (cell.isFixed()) {
            throw new FixedCellException("Unable to place " + number + " at cell " + row + ", " + column +
                                         ": the cell is fixed.");
        }

        if (number != cell.getCorrectValue()) {
            throw new InvalidValueException("Unable to place " + number + " at cell " + row + ", " + column +
                                            ": constraints violated.");
        }

        if (cell.getValue() != EMPTY_VALUE) {
            throw new FilledCellException("Unable to place " + number + " at cell " + row + ", " + column +
                                          ": the cell has already a value.");
        }

        cell.setValue(number);
    }

    /**
     * Tells whether the board is complete or not.
     *
     * @return true if every cell has been filled, false otherwise
     */
    public boolean isComplete() {
        for (int row = 0; row < SIDE_SIZE; row++) {
            for (int column = 0; column < SIDE_SIZE; column++) {
                if (board[row][column].getValue() == EMPTY_VALUE) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder board = new StringBuilder();
        StringBuilder solution = new StringBuilder();

        for (int row = 0; row < SIDE_SIZE; row++) {
            if (row != 0) {
                board.append(String.format("\t%9s", ""));
                solution.append(String.format("\t%9s", ""));
            }

            for (int column = 0; column < SIDE_SIZE; column++) {
                board.append(String.format("%3d", this.board[row][column].getValue()));
                solution.append(String.format("%3d", this.board[row][column].getCorrectValue()));

                if ((column + 1) % REGION_SIZE == 0) {
                    board.append(String.format("%3s", " "));
                    solution.append(String.format("%3s", " "));
                }
            }

            board.append("\n");
            solution.append("\n");

            if ((row + 1) % REGION_SIZE == 0) {
                board.append("\n");

                if ((row + 1) != SIDE_SIZE) {
                    solution.append("\n");
                }
            }
        }

        return "Sudoku{\n" +
                "\tboard=   " + board +
                "\tsolution=" + solution +
                "}";
    }

}
