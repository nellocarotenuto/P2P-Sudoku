package com.github.nellocarotenuto.p2psudoku.sudoku;

import org.javatuples.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Models a Sudoku board.
 */
public class Sudoku {

    private static final int SIDE_SIZE = 9;
    private static final int BOARD_SIZE = (int) Math.pow(SIDE_SIZE, 2);
    private static final int REGION_SIZE = (int) Math.sqrt(SIDE_SIZE);

    private static final int MIN_CLUES = 22;
    private static final int MAX_CLUES = 28;

    private static final List<Integer> values = IntStream.rangeClosed(1, SIDE_SIZE).boxed().collect(Collectors.toList());

    private Cell[][] board;

    private Group[] rows;
    private Group[] columns;
    private Group[][] regions;

    /**
     * Generates a new random board.
     */
    public Sudoku() {
        // Build the (empty) board
        board = new Cell[SIDE_SIZE][SIDE_SIZE];

        rows = new Group[SIDE_SIZE];
        columns = new Group[SIDE_SIZE];
        regions = new Group[REGION_SIZE][REGION_SIZE];

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

        // Generate a valid grid
        List<Pair<Cell, List<Pair<Integer, Boolean>>>> cellsToFill = new ArrayList<Pair<Cell, List<Pair<Integer, Boolean>>>>(BOARD_SIZE);

        for (int row = 0; row < SIDE_SIZE; row++) {
            for (int column = 0; column < SIDE_SIZE; column++) {
                Cell cell = board[row][column];

                List<Pair<Integer, Boolean>> candidates = new ArrayList<Pair<Integer, Boolean>>(SIDE_SIZE);

                for (Integer value : values) {
                    candidates.add(new Pair<Integer, Boolean>(value, false));
                }

                Collections.shuffle(candidates);

                Pair<Cell, List<Pair<Integer, Boolean>>> cellValuesPair = new Pair<Cell, List<Pair<Integer, Boolean>>>(cell, candidates);
                cellsToFill.add(cellValuesPair);
            }
        }

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
                    } catch (ValidationException e) {
                        continue;
                    }
                }
            }

            if (candidateIndex == candidates.size()) {
                cell.setValue(Cell.EMPTY);

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
        List<Cell> cellsToClear = new ArrayList<Cell>();

        for (int row = 0; row < SIDE_SIZE; row++) {
            for (int column = 0; column < SIDE_SIZE; column++) {
                cellsToClear.add(board[row][column]);
            }
        }

        Collections.shuffle(cellsToClear);

        Random random = new Random();
        int cluesToRemove = BOARD_SIZE - (MIN_CLUES + random.nextInt(MAX_CLUES - MIN_CLUES));
        HashMap<Integer, Integer> occurrencesRemoved = new HashMap<Integer, Integer>();

        for (int i = 0; i < cluesToRemove; i++) {
            Cell cell = cellsToClear.get(i);
            int value = cell.getValue();
            int timesRemoved = occurrencesRemoved.getOrDefault(value, Cell.EMPTY);

            if (timesRemoved < SIDE_SIZE - 1) {
                cell.setValue(Cell.EMPTY);
                occurrencesRemoved.put(value, timesRemoved + 1);
            }
        }

        // Mark the clues remaining as fixed
        for (int row = 0; row < SIDE_SIZE; row++) {
            for (int column = 0; column < SIDE_SIZE; column++) {
                Cell cell = board[row][column];

                if (cell.getValue() != Cell.EMPTY) {
                    cell.setFixed(true);
                }
            }
        }

    }

    public Integer[][] getBoard() {
        Integer[][] board = new Integer[SIDE_SIZE][SIDE_SIZE];

        for (int row = 0; row < SIDE_SIZE; row++) {
            for (int column = 0; column < SIDE_SIZE; column++) {
                board[row][column] = this.board[row][column].getValue();
            }
        }

        return board;
    }

    public void placeNumber(int row, int column, int number) throws UnexistingCellException, ValidationException {
        if (row < 1 || row > SIDE_SIZE || column < 1 || column > SIDE_SIZE) {
            throw new UnexistingCellException();
        }

        Cell cell = board[row - 1][column - 1];

        if (cell.isFixed()) {
            throw new ValidationException("Unable to place " + number + " at cell " + row + ", " + column + ": the cell is fixed.");
        }

        try {
            cell.setValue(number);
        } catch (ValidationException e) {
            throw new ValidationException("Unable to place " + number + " at cell " + row + ", " + column + ": constraints violated.");
        }
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
