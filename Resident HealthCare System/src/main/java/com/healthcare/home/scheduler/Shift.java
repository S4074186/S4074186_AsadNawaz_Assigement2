package com.healthcare.home.scheduler;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;

public class Shift implements Serializable {
    private final LocalDateTime start;
    private final LocalDateTime end;

    public Shift(LocalDateTime start, LocalDateTime end) {
        this.start = start;
        this.end = end;
    }

    public ChronoLocalDateTime<?> getStart() {
        return start;
    }

    public ChronoLocalDateTime<?> getEnd() {
        return end;
    }

    public long hours() {
        return Duration.between(start, end).toHours();
    }
}
