package com.swornima.petcare.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room row for a pet profile. The care task checklist is stored as a JSON
 * array string (see Converters) rather than a separate table, matching the
 * embedded-list shape the app already uses (Pet.careTasks).
 */
@Entity(tableName = "pets")
public class PetEntity {

    @PrimaryKey
    @NonNull
    public String id = "";

    public String userId;
    public String name;
    public String breed;
    public String age;
    public String weight;
    public String dietaryPreferences;
    public String allergies;
    public String favouriteToys;
    public String notes;
    public String imageUrl;
    public String photoGalleryJson;
    public String careTasksJson;

    public String vaccinationHistory;
    public String medicationRecords;
    public String vetVisitNotes;
    public String nextVaccinationDate;
    public String treatmentNotes;

    public double latitude;
    public double longitude;
    public String locationName;
    public String locationType;
}
