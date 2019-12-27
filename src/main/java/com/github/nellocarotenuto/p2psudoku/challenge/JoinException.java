package com.github.nellocarotenuto.p2psudoku.challenge;

/**
 * Models an exception thrown when the user cannot join a game.
 */
public class JoinException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Unable to join game: something went wrong";

    public JoinException() {
        super(DEFAULT_MESSAGE);
    }

    public JoinException(String message) {
        super(message);
    }

}
