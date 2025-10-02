package com.healthcare.home.staff;

import com.healthcare.home.entity.Role;

public class Doctor extends Staff {
    public Doctor(String id, String name, String username, String passwordHash) {
        super(id, name, Role.DOCTOR, username, passwordHash);
    }
}
