package com.healthcare.home.exceptions;

public class UnAuthorizationException extends RuntimeException {
    /**
     * UnAuthorizationException
     *
     * @param exMsg
     */
    public UnAuthorizationException(String exMsg) {
        super(exMsg);
    }
}
