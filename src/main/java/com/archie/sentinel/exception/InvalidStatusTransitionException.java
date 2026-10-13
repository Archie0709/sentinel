package com.archie.sentinel.exception;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String from, String to) {
        super("Cannot move incident from " + from + " to " + to);
    }
}
