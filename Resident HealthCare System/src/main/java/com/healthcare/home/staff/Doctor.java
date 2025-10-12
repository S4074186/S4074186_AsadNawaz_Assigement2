package com.healthcare.home.staff;

import com.healthcare.home.entity.Role;

public class Doctor extends Staff {
    public Doctor(String name, String username, String passwordHash) {
        super(name, Role.DOCTOR, username, passwordHash);
    }
}
