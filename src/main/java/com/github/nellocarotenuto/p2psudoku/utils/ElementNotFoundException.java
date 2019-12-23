package com.github.nellocarotenuto.p2psudoku.utils;

/**
 * Models an exception thrown when attempting to retrieve an element but none exists with its key.
 */
public class ElementNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "No element with this key could be found.";

    public ElementNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public ElementNotFoundException(String message) {
        super(message);
    }

}
