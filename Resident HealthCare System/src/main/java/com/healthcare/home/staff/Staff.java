package com.healthcare.home.staff;

import java.io.Serializable;

import com.healthcare.home.entity.Role;
import com.healthcare.home.auth.Access;

public abstract class Staff implements Serializable {
    private final String id;
    private String name;
    private final Role role;
    private final String username;
    private String passwordHash;

    protected Staff(String id, String name, Role role, String username, String passwordHash) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getId() {
        return id;
    }
    public boolean has(Access access) {
        switch (role) {
            case MANAGER:
                return access == Access.ADD_STAFF ||
                        access == Access.UPDATE_STAFF ||
                        access == Access.VIEW_RESIDENT ||
                        access == Access.ADD_RESIDENT;

            case DOCTOR:
                return access == Access.VIEW_RESIDENT ||
                        access == Access.WRITE_PRESCRIPTION;

            case NURSE:
                return access == Access.VIEW_RESIDENT ||
                        access == Access.ADMINISTER_MEDICATION;
        }
        return false;
    }
}
