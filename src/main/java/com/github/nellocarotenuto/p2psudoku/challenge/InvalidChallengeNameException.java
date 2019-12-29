package com.github.nellocarotenuto.p2psudoku.challenge;

/**
 * Models an exception to be thrown when attempting to create a game with an invalid name.
 */
public class InvalidChallengeNameException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "This game name isn't allowed.";

    public InvalidChallengeNameException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidChallengeNameException(String message) {
        super(message);
    }

}
