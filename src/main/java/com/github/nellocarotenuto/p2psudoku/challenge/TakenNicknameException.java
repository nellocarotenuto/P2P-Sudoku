package com.github.nellocarotenuto.p2psudoku.challenge;

/**
 * Models an exception to be thrown when a players tries to pick a name already taken.
 */
public class TakenNicknameException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "This nickname is already taken.";

    public TakenNicknameException() {
        super(DEFAULT_MESSAGE);
    }

    public TakenNicknameException(String message) {
        super(message);
    }

}
