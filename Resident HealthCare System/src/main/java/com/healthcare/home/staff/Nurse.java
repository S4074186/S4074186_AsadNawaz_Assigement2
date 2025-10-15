package com.healthcare.home.staff;

import com.healthcare.home.entities.Role;

public class Nurse extends Staff {
    /**
     * Nurse Constructor
     *
     * @param name
     * @param username
     * @param password
     */
    public Nurse(String name, String username, String password) {
        super(name, Role.NURSE, username, password);
    }
}
