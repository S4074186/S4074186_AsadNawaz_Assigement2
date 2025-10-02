package com.healthcare.home.staff;

import com.healthcare.home.entity.Role;

public class Manager extends Staff {
    public Manager(String id, String name, String username, String passwordHash) {
        super(id, name, Role.MANAGER, username, passwordHash);
    }
}
