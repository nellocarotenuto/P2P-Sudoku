package com.github.nellocarotenuto.p2psudoku.challenge;

/**
 * Models an exception thrown when attempting to perform actions on a challenge that has not started yet or has already
 * finished.
 */
public class ChallengeStatusException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Challenge has not started yet or already finished.";

    public ChallengeStatusException() {
        super(DEFAULT_MESSAGE);
    }

    public ChallengeStatusException(String message) {
        super(message);
    }

}
