package com.healthcare.home.scheduler;

// assuming your main class is named Home

import java.io.Serializable;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import com.healthcare.home.core.ResidentHealthCareHome;
import com.healthcare.home.entities.Shift;
import com.healthcare.home.exceptions.RosterUnfollowedException;
import com.healthcare.home.staff.Nurse;
import com.healthcare.home.staff.Staff;

public class Scheduler implements Serializable {
    private final EnumMap<DayOfWeek, List<ShiftAssignment>> map = new EnumMap<>(DayOfWeek.class);
    private final Map<String, List<Shift>> dailyRoster = new HashMap<>();
    private static ScheduledExecutorService scheduler;

//    public List<ShiftAssignment> getAssignments(DayOfWeek d) {
//        return map.getOrDefault(d, Collections.emptyList());
//    }

    public static record ShiftAssignment(Shift shift, Staff staff) implements Serializable {}

    public void assigningShiftToStaff(Staff staff, Shift shift) {
        dailyRoster.computeIfAbsent(staff.getId(), k -> new ArrayList<>());

        long shiftCountToday = dailyRoster.get(staff.getId()).stream()
                .filter(s -> s.start().toLocalDate().equals(shift.start().toLocalDate()))
                .count();

        if (shiftCountToday >= 2 && staff instanceof Nurse) {
            throw new RosterUnfollowedException("Too many shifts for nurse " + staff.getId());
        }

        dailyRoster.get(staff.getId()).add(shift);

        // ✅ also add to the EnumMap for day-based compliance check
        DayOfWeek day = shift.start().getDayOfWeek();
        map.computeIfAbsent(day, k -> new ArrayList<>()).add(new ShiftAssignment(shift, staff));
    }


    public boolean isAvailableOnDuty(Staff staff, LocalDateTime time) {
        return dailyRoster.getOrDefault(staff.getId(), List.of()).stream()
                .anyMatch(s -> !time.isBefore(s.start()) && !time.isAfter(s.end()));
    }

    public Map<String, List<Shift>> getDailyRoster() {
        return Collections.unmodifiableMap(dailyRoster);
    }

    // -----------------------------
    // Scheduler to call checkCompliance
    // -----------------------------
    public void startComplianceScheduler(ResidentHealthCareHome home) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        Runnable complianceTask = () -> {
            try {
                System.out.println("Running compliance check localDateTime " + LocalTime.now());
                home.checkingCompliance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        // Schedule the job for every required time
        int[] hours = {8, 10, 11, 14, 16, 22};
        for (int hour : hours) {
            long delay = getDelayUntilHour(hour);
            scheduler.scheduleAtFixedRate(complianceTask, delay, 24 * 60 * 60, TimeUnit.SECONDS);
        }
    }

    private long getDelayUntilHour(int hour) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime next = now.withHour(hour).withMinute(0).withSecond(0);
        if (now.isAfter(next)) {
            next = next.plusDays(1);
        }
        return java.time.Duration.between(now, next).getSeconds();
    }

//    private long computeInitialDelay(int targetHour) {
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime target = now.withHour(targetHour).withMinute(0).withSecond(0);
//
//        if (now.isAfter(target)) {
//            target = target.plusDays(1);
//        }
//
//        return Duration.between(now, target).getSeconds();
//    }

    public static void stopScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

}
