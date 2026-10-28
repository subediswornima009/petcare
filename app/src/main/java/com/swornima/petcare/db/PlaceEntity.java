package com.swornima.petcare.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * A saved location (vet clinic, grooming salon, dog park, pet supply store,
 * or animal shelter) that the owner has searched for and saved, independent
 * of any single pet. Reuses the same location-type vocabulary as
 * Pet.locationType.
 */
@Entity(tableName = "places")
public class PlaceEntity {

    @PrimaryKey
    @NonNull
    public String id = "";

    public String userId;
    public String name;
    public String type;
    public String address;
    public String phone;
    public String openingHours;
    public String notes;
    public double latitude;
    public double longitude;
    public long createdAtMillis;
}
