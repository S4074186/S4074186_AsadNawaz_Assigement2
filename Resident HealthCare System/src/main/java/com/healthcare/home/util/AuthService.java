package com.healthcare.home.util;

import com.healthcare.home.staff.Staff;
import com.healthcare.home.auth.Access;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

public final class AuthService {
    private AuthService() { }

    public static void authorizeOrThrow(Staff staff, Access required) throws SecurityException {
        if (staff == null) throw new SecurityException("No staff provided");
        if (!staff.has(required)) {
            throw new SecurityException("Not authorized for action: " + required);
        }
        if (!isRosteredNow(staff)) {
            throw new SecurityException("Staff not rostered for current day time");
        }
    }

    private static boolean isRosteredNow(Staff staff) {
        try {
            // try common method names using reflection to avoid tight coupling
            Method m1 = staff.getClass().getMethod("isRosteredAt", java.time.LocalDateTime.class);
            return (boolean) m1.invoke(staff, LocalDateTime.now());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) { }
        try {
            Method m2 = staff.getClass().getMethod("isRosteredNow");
            return (boolean) m2.invoke(staff);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) { }
        // fallback to true if no roster check exists on model
        return true;
    }
}
