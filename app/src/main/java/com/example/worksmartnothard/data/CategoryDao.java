package com.example.worksmartnothard.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertCategory(Category category);

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    List<Category> getAllCategories();

    @Query("SELECT name FROM categories ORDER BY name COLLATE NOCASE")
    List<String> getAllNames();

    @Query("SELECT COUNT(*) FROM categories")
    int count();
}
