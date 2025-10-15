package com.healthcare.home.exceptions;

public class RosterUnfollowedException extends RuntimeException {
    /**
     * RosterUnfollowedException
     *
     * @param exMsg
     */
    public RosterUnfollowedException(String exMsg) {
        super(exMsg);
    }
}
