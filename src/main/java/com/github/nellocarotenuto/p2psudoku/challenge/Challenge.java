package com.github.nellocarotenuto.p2psudoku.challenge;

import com.github.nellocarotenuto.p2psudoku.sudoku.FixedCellException;
import com.github.nellocarotenuto.p2psudoku.sudoku.FilledCellException;
import com.github.nellocarotenuto.p2psudoku.sudoku.InvalidValueException;
import com.github.nellocarotenuto.p2psudoku.sudoku.Sudoku;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import org.javatuples.Triplet;

/**
 * Models a multiplayer Sudoku challenge.
 */
public class Challenge implements Serializable {

    public enum Status {
        WAITING, PLAYING, ENDED
    }

    private static final long serialVersionUID = 3711666168632123236L;

    public static final int WRONG_NUMBER_SCORE = -1;
    public static final int CORRECT_NUMBER_SCORE = 1;

    private String name;
    private Sudoku sudoku;
    private Integer[][] initialBoard;
    private Player owner;
    private Status status;
    private boolean listed;
    private HashMap<Player, Triplet<Integer[][], Integer, Boolean>> games;

    /**
     * Creates a new Sudoku challenge.
     *
     * @param owner the player that owns the challenge
     * @param name the challenge name
     * @param seed the seed for the Sudoku
     * @param listed true if the challenge is to be public, false otherwise
     */
    public Challenge(Player owner, String name, int seed, boolean listed) {
        // Set the game name
        this.name = name;

        // Generate a new Sudoku
        this.sudoku = new Sudoku(seed);

        // Set visibility
        this.listed = listed;

        // Save the initial board aside
        this.initialBoard = Arrays.stream(sudoku.getBoard()).map(Integer[]::clone).toArray(Integer[][]::new);

        // Prepare the hashmap to store users' boards and scores
        games = new HashMap<>();

        // Set the owner of the game
        this.owner = owner;

        // Set the status to waiting
        this.status = Status.WAITING;
    }

    /**
     * Gets the name of the challenge.
     *
     * @return the name of the challenge
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the owner of the game.
     *
     * @return the owner of the game
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * Sets the owner of the game.
     *
     * @param owner the player to be assigned as owner of the game
     */
    public void setOwner(Player owner) {
        this.owner = owner;
    }

    /**
     * Adds a player to the list of those playing the challenge.
     *
     * @param player the player to add
     */
    public void addPlayer(Player player) throws JoinException {
        if (games.containsKey(player)) {
            return;
        }

        Integer[][] board = Arrays.stream(initialBoard).map(Integer[]::clone).toArray(Integer[][]::new);
        games.put(player, new Triplet<>(board, 0, false));
    }

    /**
     * Removes a player from the list of those playing the challenge.
     *
     * @param player the player to remove
     */
    public void removePlayer(Player player) {
        games.remove(player);
    }

    /**
     * Gets the status of the challenge.
     *
     * @return Status.WAITING if the challenge is yet to be started,
     *         Status.PLAYING if the challenge has started but not completed
     *         Status.ENDED if the challenge is completed
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the status of the challenge.
     *
     * @param status the new status of the challenge
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Gets the Sudoku board with just the numbers placed by the user.
     *
     * @param player the user requesting the board
     *
     * @return the integer matrix for the user
     */
    public Integer[][] getBoard(Player player) {
        return games.get(player).getValue0();
    }

    /**
     * Gets the games for this challenge.
     *
     * @return the board, score and game status for each user playing this challenge
     */
    public HashMap<Player, Triplet<Integer[][], Integer, Boolean>> getGames() {
        return games;
    }

    /**
     * Gets the visibility of the challenge.
     *
     * @return true if the challenge is public, false otherwise
     */
    public boolean isListed() {
        return listed;
    }

    /**
     * Places a new number.
     *
     * @param player the user placing the number
     * @param row the row index (starting at 0) of the cell where to insert the number
     * @param column the column index (starting at 0) of the cell where to insert the number
     * @param number the solution number
     *
     * @throws FixedCellException if the cell selected is fixed
     * @throws InvalidValueException if the number doesn't fit into the specified cell
     */
    public void placeNumber(Player player, int row, int column, int number) throws FixedCellException,
                                                                                   InvalidValueException {
        Triplet<Integer[][], Integer, Boolean> game = games.get(player);

        Integer[][] board = game.getValue0();
        int score = game.getValue1();
        boolean completed = game.getValue2();

        if (completed) {
            return;
        }

        try {
            // Set the number in the global board
            sudoku.placeNumber(row, column, number);

            // Set the number into user's board
            board[row][column] = number;
            game = game.setAt0(board);

            // Increment user score
            score += CORRECT_NUMBER_SCORE;
            game = game.setAt1(score);
        } catch (FilledCellException e) {
            // Just set the number into user's board
            board[row][column] = number;
            game = game.setAt0(board);

            throw e;
        } catch (InvalidValueException e) {
            // Decrement user score
            score += WRONG_NUMBER_SCORE;
            game = game.setAt1(score);

            throw e;
        } finally {
            // Check for board completion
            completed = true;

            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board[i].length; j++) {
                    if (board[i][j] == Sudoku.EMPTY_VALUE) {
                        completed = false;
                        break;
                    }
                }

                if (!completed) {
                    break;
                }
            }

            game = game.setAt2(completed);

            // Update game info
            games.put(player, game);

            // Set the status of the whole challenge to ended if one player has completed the board
            if (completed) {
                this.status = Status.ENDED;
            }
        }
    }

    /**
     * Gets the public details of the challenge.
     *
     * @return the public details of the challenge
     */
    public Info getInfo() {
        return new Info(name, owner.getNickname(), status, games.keySet().size());
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        Challenge challenge = (Challenge) object;
        return name.equals(challenge.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Challenge{" +
                "\n\tname='" + name + "'," +
                "\n\tstatus=" + status + "'," +
                "\n\towner=" + owner.getNickname() + "'," +
                "\n\tgames=" + games.size() +
                "\n}";
    }

}
