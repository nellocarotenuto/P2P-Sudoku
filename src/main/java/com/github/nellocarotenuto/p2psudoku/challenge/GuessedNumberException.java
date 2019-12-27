package com.github.nellocarotenuto.p2psudoku.challenge;

/**
 * Models an exception thrown when a player guesses a number that has already been placed by another player.
 */
public class GuessedNumberException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "This number has already been guessed.";

    public GuessedNumberException() {
        super(DEFAULT_MESSAGE);
    }

    public GuessedNumberException(String message) {
        super(message);
    }

}
