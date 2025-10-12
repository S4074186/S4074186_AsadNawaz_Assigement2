package com.healthcare.home.scheduler;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;

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

        // 1 shifts to each staff member allowed per day
        long shiftCountToday = dailyRoster.get(staff.getId()).stream()
                .filter(s -> s.getStart().equals(shift.getStart()))
                .count();
        if (shiftCountToday >= 1)
            throw new RosterUnfollowedException("Too many shifts for " + staff.getId() + " on " + shift.getStart());
        dailyRoster.get(staff.getId()).add(shift);
    }

    public boolean isAvailableOnDuty(Staff staff, LocalDateTime time) {
        return dailyRoster.getOrDefault(staff.getId(), List.of()).stream()
                .anyMatch(s -> !time.isBefore(s.getStart()) && !time.isAfter(s.getEnd()));
    }

    public Map<String, List<Shift>> getDailyRoster() {
        return Collections.unmodifiableMap(dailyRoster);
    }

}
