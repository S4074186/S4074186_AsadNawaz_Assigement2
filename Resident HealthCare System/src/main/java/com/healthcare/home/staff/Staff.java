package com.healthcare.home.staff;

import java.io.Serializable;

import com.healthcare.home.entity.Role;
import com.healthcare.home.auth.Access;

public abstract class Staff implements Serializable {
    private static long idCounter = 0;
    private final String id;
    private String name;
    private final Role role;
    private final String username;
    private String passwordHash;

    protected Staff(String name, Role role, String username, String passwordHash) {
        this.id = this.generateId();
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
        return switch (role) {
            case MANAGER -> access == Access.ADD_STAFF ||
                    access == Access.UPDATE_STAFF ||
                    access == Access.VIEW_RESIDENT ||
                    access == Access.ADD_RESIDENT ||
                    access == Access.DISCHARGE_RESIDENT ||
                    access == Access.SHIFT_ASSIGNMENT;
            case DOCTOR -> access == Access.VIEW_RESIDENT ||
                    access == Access.WRITE_PRESCRIPTION;
            case NURSE -> access == Access.VIEW_RESIDENT ||
                    access == Access.MOVE_RESIDENT ||
                    access == Access.ADMINISTER_MEDICATION ||
                    access == Access.UPDATE_PRESCRIPTION;
        };
    }

    private synchronized String generateId() {
        idCounter++;
        return String.format("STF-%03d", idCounter);
    }

    public static void setIdCounter(long lastId) {
        idCounter = lastId;
    }

    public static long getIdCounter() {
        return idCounter;
    }

}
