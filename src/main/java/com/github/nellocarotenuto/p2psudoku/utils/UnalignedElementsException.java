package com.github.nellocarotenuto.p2psudoku.utils;

/**
 * Models an exception to be thrown when peers do not agree on the latest version of an element in the DHT.
 */
class UnalignedElementsException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Peers do not agree on the latest version of this element.";

    public UnalignedElementsException() {
        super(DEFAULT_MESSAGE);
    }

    public UnalignedElementsException(String message) {
        super(message);
    }

}
