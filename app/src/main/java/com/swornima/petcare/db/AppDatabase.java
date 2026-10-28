package com.swornima.petcare.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {UserEntity.class, PetEntity.class, CareTaskEntity.class, ExpenseEntity.class, PlaceEntity.class}, version = 8, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract UserDao userDao();

    public abstract PetDao petDao();

    public abstract CareTaskDao careTaskDao();

    public abstract ExpenseDao expenseDao();

    public abstract PlaceDao placeDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "petcare.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
