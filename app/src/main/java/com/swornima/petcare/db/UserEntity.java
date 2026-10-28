package com.swornima.petcare.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * A locally stored account. Replaces the Firebase Auth user record; the
 * password is never stored in plain text (see PasswordHasher).
 */
@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey
    @NonNull
    public String uid = "";

    public String email = "";
    public String passwordHash = "";
    public String firstName = "";
    public String lastName = "";
    public String fullName = "";
    public String photoUri = "";
}
