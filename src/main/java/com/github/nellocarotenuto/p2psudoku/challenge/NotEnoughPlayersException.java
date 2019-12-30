package com.github.nellocarotenuto.p2psudoku.challenge;

/**
 * Models an exception thrown when attempting to start a challenge with not enough players in it.
 */
public class NotEnoughPlayersException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "There are not enough players in the challenge to start.";

    public NotEnoughPlayersException() {
        super(DEFAULT_MESSAGE);
    }

    public NotEnoughPlayersException(String message) {
        super(message);
    }

}
