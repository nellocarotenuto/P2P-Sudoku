package com.github.nellocarotenuto.p2psudoku.challenge;

/**
 * Models an exception thrown when the user cannot join a game.
 */
public class ChallengeNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Unable to join game: something went wrong";

    public ChallengeNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public ChallengeNotFoundException(String message) {
        super(message);
    }

}
