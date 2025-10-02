package com.healthcare.home.exceptions;

public class UnAuthorizationException extends RuntimeException {
    public UnAuthorizationException(String exMsg) {
        super(exMsg);
    }
}
