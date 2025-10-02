package com.healthcare.home.exceptions;

public class ResidentNotFoundException extends RuntimeException {
    public ResidentNotFoundException(String exMsg) {
        super(exMsg);
    }
}
