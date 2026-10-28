package com.swornima.petcare.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert
    void insert(ExpenseEntity expense);

    @Delete
    void delete(ExpenseEntity expense);

    @Query("DELETE FROM expenses WHERE id = :expenseId AND userId = :userId")
    void deleteById(String expenseId, String userId);

    @Query("DELETE FROM expenses WHERE userId = :userId")
    void deleteAllForUser(String userId);

    @Query("UPDATE expenses SET petId = :petId, petName = :petName, category = :category, " +
            "description = :description, notes = :notes, amount = :amount WHERE id = :expenseId AND userId = :userId")
    void updateById(String expenseId, String userId, String petId, String petName,
                    String category, String description, String notes, double amount);

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY dateMillis DESC")
    List<ExpenseEntity> getAllForUser(String userId);

    @Query("SELECT * FROM expenses WHERE id = :expenseId AND userId = :userId LIMIT 1")
    ExpenseEntity getById(String expenseId, String userId);
}
