package com.healthcare.home.entities;

import java.io.Serializable;
import java.time.LocalDateTime;

public record Shift(LocalDateTime start, LocalDateTime end) implements Serializable {
}
