package com.arijit.budgettracker.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CategoryDao {
    @Insert
    suspend fun insertCategory(category: Category)

    @Query("SELECT * FROM categories WHERE type = :type OR type = 'both' ORDER BY createdAt DESC, name ASC")
    suspend fun getCategoriesByType(type: String): List<Category>

    @Query("SELECT * FROM categories ORDER BY createdAt DESC, name ASC")
    suspend fun getAllCategories(): List<Category>
}
