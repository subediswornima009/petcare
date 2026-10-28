package com.swornima.petcare.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PlaceDao {

    @Insert
    void insert(PlaceEntity place);

    @Query("DELETE FROM places WHERE id = :placeId AND userId = :userId")
    void deleteById(String placeId, String userId);

    @Query("DELETE FROM places WHERE userId = :userId")
    void deleteAllForUser(String userId);

    @Query("UPDATE places SET name = :name, type = :type, phone = :phone, " +
            "openingHours = :openingHours, notes = :notes WHERE id = :placeId AND userId = :userId")
    void updateDetails(String placeId, String userId, String name, String type,
                       String phone, String openingHours, String notes);

    @Query("SELECT * FROM places WHERE userId = :userId ORDER BY createdAtMillis DESC")
    List<PlaceEntity> getAllForUser(String userId);
}
