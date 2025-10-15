package com.healthcare.home.staff;

import com.healthcare.home.entities.Role;

public class Doctor extends Staff {
    public Doctor(String name, String username, String password) {
        super(name, Role.DOCTOR, username, password);
    }
}
