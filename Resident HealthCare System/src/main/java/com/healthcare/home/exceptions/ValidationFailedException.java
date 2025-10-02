package com.healthcare.home.exceptions;

public class ValidationFailedException extends RuntimeException {
    public ValidationFailedException(String exMsg) {
        super(exMsg);
    }
}
