package com.swornima.petcare.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room row for a reusable care task (Manage My Care Tasks screen). Separate
 * from the copies embedded inside PetEntity.careTasksJson, mirroring the
 * original two-collection Firestore layout (careTasks vs. pet.careTasks).
 */
@Entity(tableName = "care_tasks")
public class CareTaskEntity {

    @PrimaryKey
    @NonNull
    public String id = "";

    public String userId;
    public String name;
    public String category;
    public String supplies;
    public String notes;
    public String time;
    public String frequency;
    public String appointmentDate;
    public String groomingDate;
    public String vaccinationDueDate;
    public String medicationStartDate;
    public String medicationEndDate;
    public boolean completed;
}
