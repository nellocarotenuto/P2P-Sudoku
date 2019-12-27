package com.github.nellocarotenuto.p2psudoku.challenge;

/**
 * Models an exception to be thrown when a login attempt with an invalid nickname is made.
 */
public class InvalidNicknameException extends Exception {

    private static final String DEFAULT_MESSAGE = "This nickname isn't allowed.";

    public InvalidNicknameException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidNicknameException(String message) {
        super(message);
    }

}
