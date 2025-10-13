package com.healthcare.home.scheduler;

// assuming your main class is named Home

import java.io.Serializable;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.exceptions.RosterUnfollowedException;
import com.healthcare.home.staff.Staff;

public class Schedule implements Serializable {
    private final EnumMap<DayOfWeek, List<ShiftAssignment>> map = new EnumMap<>(DayOfWeek.class);
    private final Map<String, List<Shift>> dailyRoster = new HashMap<>();

    public List<ShiftAssignment> getAssignments(DayOfWeek d) {
        return map.getOrDefault(d, Collections.emptyList());
    }

    public static record ShiftAssignment(Shift shift, Staff staff) implements Serializable {}

    public void assigningShiftToStaff(Staff staff, Shift shift) {
        dailyRoster.computeIfAbsent(staff.getId(), k -> new ArrayList<>());

        long shiftCountToday = dailyRoster.get(staff.getId()).stream()
                .filter(s -> s.getStart().equals(shift.getStart()))
                .count();

        if (shiftCountToday >= 1) {
            System.err.println("Shift skipped for " + staff.getClass().getSimpleName() +
                    " (" + staff.getId() + ") at " + shift.getStart());
            throw new RosterUnfollowedException("Too many shifts for " + staff.getId() + " on " + shift.getStart());
        }

        dailyRoster.get(staff.getId()).add(shift);
    }

    public boolean isAvailableOnDuty(Staff staff, LocalDateTime time) {
        return dailyRoster.getOrDefault(staff.getId(), List.of()).stream()
                .anyMatch(s -> !time.isBefore(s.getStart()) && !time.isAfter(s.getEnd()));
    }

    public Map<String, List<Shift>> getDailyRoster() {
        return Collections.unmodifiableMap(dailyRoster);
    }

    // -----------------------------
    // Scheduler to call checkCompliance
    // -----------------------------
    public void startComplianceScheduler(HealthCareHome home) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        Runnable complianceTask = () -> {
            try {
                System.out.println("Running compliance check at " + LocalTime.now());
                home.checkCompliance();
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

    private long computeInitialDelay(int targetHour) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = now.withHour(targetHour).withMinute(0).withSecond(0);

        if (now.isAfter(target)) {
            target = target.plusDays(1);
        }

        return Duration.between(now, target).getSeconds();
    }

    private static ScheduledExecutorService scheduler;

    public static void stopScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

}
