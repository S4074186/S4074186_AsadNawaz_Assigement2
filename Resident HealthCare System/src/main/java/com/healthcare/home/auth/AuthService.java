package com.healthcare.home.auth;

import com.healthcare.home.staff.Staff;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

public final class AuthService {
    private AuthService() { }

    public static void authorizeOrThrow(Staff staff, AuthAccess access) throws SecurityException {
        if (staff == null) throw new SecurityException("No staff provided");
        if (!staff.hasAccess(access)) {
            throw new SecurityException("Not authorized for action: " + access);
        }
        if (!isRosteredNow(staff)) {
            throw new SecurityException("Staff not rostered for current day time");
        }
    }

    private static boolean isRosteredNow(Staff staff) {
        try {
            Method method1 = staff.getClass().getMethod("isRosteredAt", java.time.LocalDateTime.class);
            return (boolean) method1.invoke(staff, LocalDateTime.now());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) { }
        try {
            Method method2 = staff.getClass().getMethod("isRosteredNow");
            return (boolean) method2.invoke(staff);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) { }
        return true;
    }
}
