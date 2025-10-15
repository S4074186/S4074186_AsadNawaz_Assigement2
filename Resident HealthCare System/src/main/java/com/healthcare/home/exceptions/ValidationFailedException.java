package com.healthcare.home.exceptions;

public class ValidationFailedException extends RuntimeException {
    /**
     * ValidationFailedException
     *
     * @param exMsg
     */
    public ValidationFailedException(String exMsg) {
        super(exMsg);
    }
}
