package com.healthcare.home.exceptions;

public class BedNotAvailableException extends RuntimeException {
    /**
     * BedNotAvailableException
     *
     * @param exMsg
     */
    public BedNotAvailableException(String exMsg) {
        super(exMsg);
    }
}
