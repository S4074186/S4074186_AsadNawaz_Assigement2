package com.healthcare.home.exceptions;

public class BedNotAvailableException extends RuntimeException {
    public BedNotAvailableException(String exMsg) {
        super(exMsg);
    }
}
