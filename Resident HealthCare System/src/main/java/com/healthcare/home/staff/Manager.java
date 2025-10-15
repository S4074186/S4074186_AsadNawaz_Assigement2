package com.healthcare.home.staff;

import com.healthcare.home.entities.Role;

public class Manager extends Staff {
    public Manager(String name, String username, String password) {
        super(name, Role.MANAGER, username, password);
    }
}
