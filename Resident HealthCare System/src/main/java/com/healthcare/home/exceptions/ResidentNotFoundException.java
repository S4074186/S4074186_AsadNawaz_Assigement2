package com.healthcare.home.exceptions;

public class ResidentNotFoundException extends RuntimeException {
    /**
     * ResidentNotFoundException
     *
     * @param exMsg
     */
    public ResidentNotFoundException(String exMsg) {
        super(exMsg);
    }
}
