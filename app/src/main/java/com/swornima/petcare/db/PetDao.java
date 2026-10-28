package com.swornima.petcare.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(PetEntity pet);

    @Delete
    void delete(PetEntity pet);

    @Query("DELETE FROM pets WHERE id = :petId AND userId = :userId")
    void deleteById(String petId, String userId);

    @Query("DELETE FROM pets WHERE userId = :userId")
    void deleteAllForUser(String userId);

    @Query("SELECT * FROM pets WHERE userId = :userId")
    List<PetEntity> getAllForUser(String userId);

    @Query("SELECT * FROM pets WHERE userId = :userId LIMIT :limit")
    List<PetEntity> getRecentForUser(String userId, int limit);

    @Query("SELECT * FROM pets WHERE id = :petId AND userId = :userId LIMIT 1")
    PetEntity getById(String petId, String userId);

    @Query("SELECT COUNT(*) FROM pets WHERE userId = :userId")
    int countForUser(String userId);
}
