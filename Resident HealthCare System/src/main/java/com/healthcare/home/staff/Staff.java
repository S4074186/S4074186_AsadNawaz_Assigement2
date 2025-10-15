package com.healthcare.home.staff;

import java.io.Serializable;

import com.healthcare.home.entities.Role;
import com.healthcare.home.auth.AuthAccess;
import lombok.Data;
import lombok.Setter;

@Data
public abstract class Staff implements Serializable {
    @Setter
    private static long idCounter = 0;
    private final String id;
    private String name;
    private final Role role;
    private final String username;
    private String password;

    protected Staff(String name, Role role, String username, String password) {
        this.id = this.generateId();
        this.name = name;
        this.role = role;
        this.username = username;
        this.password = password;
    }

    public boolean hasAccess(AuthAccess authAccess) {
        return switch (role) {
            case MANAGER -> authAccess == AuthAccess.ADD_STAFF ||
                    authAccess == AuthAccess.UPDATE_STAFF ||
                    authAccess == AuthAccess.VIEW_RESIDENT ||
                    authAccess == AuthAccess.ADD_RESIDENT ||
                    authAccess == AuthAccess.DISCHARGE_RESIDENT ||
                    authAccess == AuthAccess.SHIFT_ASSIGNMENT;
            case DOCTOR -> authAccess == AuthAccess.VIEW_RESIDENT ||
                    authAccess == AuthAccess.WRITE_PRESCRIPTION;
            case NURSE -> authAccess == AuthAccess.VIEW_RESIDENT ||
                    authAccess == AuthAccess.MOVE_RESIDENT ||
                    authAccess == AuthAccess.ADMINISTER_MEDICATION ||
                    authAccess == AuthAccess.UPDATE_PRESCRIPTION;
        };
    }

    private synchronized String generateId() {
        idCounter++;
        return String.format("STF-%03d", idCounter);
    }
}
