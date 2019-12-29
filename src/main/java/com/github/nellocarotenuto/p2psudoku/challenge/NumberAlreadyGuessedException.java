package com.github.nellocarotenuto.p2psudoku.challenge;

/**
 * Models an exception thrown when a player guesses a number that has already been placed by another player.
 */
public class NumberAlreadyGuessedException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "This number has already been guessed.";

    public NumberAlreadyGuessedException() {
        super(DEFAULT_MESSAGE);
    }

    public NumberAlreadyGuessedException(String message) {
        super(message);
    }

}
