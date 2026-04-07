package com.arijit.budgettracker.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CategoryDao {
    @Insert
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories ORDER BY createdAt DESC, name ASC")
    suspend fun getCategoriesByType(): List<Category>

    @Query("SELECT * FROM categories ORDER BY createdAt DESC, name ASC")
    suspend fun getAllCategories(): List<Category>
}
