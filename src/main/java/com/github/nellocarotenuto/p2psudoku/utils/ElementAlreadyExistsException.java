package com.github.nellocarotenuto.p2psudoku.utils;

/**
 * Models an exception thrown when attempting to create an element when another one with the same key already exists.
 */
public class ElementAlreadyExistsException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "An element with this key already exists.";

    public ElementAlreadyExistsException() {
        super(DEFAULT_MESSAGE);
    }

    public ElementAlreadyExistsException(String message) {
        super(message);
    }

}
