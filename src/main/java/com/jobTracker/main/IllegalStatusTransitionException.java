package com.jobtracker.main;

public class IllegalStatusTransitionException extends RuntimeException {
    public IllegalStatusTransitionException(String errorMessage) {
        super(errorMessage);
    }
}
