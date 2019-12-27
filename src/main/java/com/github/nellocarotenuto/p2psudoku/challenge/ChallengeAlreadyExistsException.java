package com.github.nellocarotenuto.p2psudoku.challenge;

/**
 * Models an exception to be thrown when attempting to create a challenge when one with the same name already exists.
 */
public class ChallengeAlreadyExistsException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "A challenge with this name already exists";

    public ChallengeAlreadyExistsException() {
        super(DEFAULT_MESSAGE);
    }

    public ChallengeAlreadyExistsException(String message) {
        super(message);
    }

}
