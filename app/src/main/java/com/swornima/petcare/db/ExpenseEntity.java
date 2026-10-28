package com.swornima.petcare.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses")
public class ExpenseEntity {

    @PrimaryKey(autoGenerate = true)
    public long rowId;

    public String id;
    public String userId;
    public String petId;
    public String petName;
    public String category;
    public String description;
    public String notes;
    public double amount;
    public long dateMillis;
}
