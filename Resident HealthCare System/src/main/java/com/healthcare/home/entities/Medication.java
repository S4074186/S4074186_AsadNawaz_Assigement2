package com.healthcare.home.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class Medication implements Serializable {
    private String prescriptionId;
    private String nurseId;
    private LocalDateTime at;
    private String dose;
}
