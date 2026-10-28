package com.swornima.petcare.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserDao {

    @Insert
    void insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity findByEmail(String email);

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    UserEntity findByUid(String uid);

    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    int countByEmail(String email);
}
