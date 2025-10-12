package com.healthcare.home.staff;

import com.healthcare.home.entity.Role;

public class Nurse extends Staff {
    public Nurse(String name, String username, String passwordHash) {
        super(name, Role.NURSE, username, passwordHash);
    }
}
