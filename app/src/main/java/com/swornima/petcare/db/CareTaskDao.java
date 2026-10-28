package com.swornima.petcare.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CareTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CareTaskEntity task);

    @Update
    void update(CareTaskEntity task);

    @Delete
    void delete(CareTaskEntity task);

    @Query("DELETE FROM care_tasks WHERE id = :taskId AND userId = :userId")
    void deleteById(String taskId, String userId);

    @Query("DELETE FROM care_tasks WHERE userId = :userId")
    void deleteAllForUser(String userId);

    @Query("SELECT * FROM care_tasks WHERE userId = :userId")
    List<CareTaskEntity> getAllForUser(String userId);

    @Query("SELECT * FROM care_tasks WHERE id = :taskId AND userId = :userId LIMIT 1")
    CareTaskEntity getById(String taskId, String userId);
}
