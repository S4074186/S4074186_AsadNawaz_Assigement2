package com.healthcare.home.staff;

import com.healthcare.home.entities.Role;

public class Manager extends Staff {
    /**
     * Manager Constructor
     *
     * @param name
     * @param username
     * @param password
     */
    public Manager(String name, String username, String password) {
        super(name, Role.MANAGER, username, password);
    }
}
