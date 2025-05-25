package com.example.worksmartnothard;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface GoalDao {

    @Insert
    void insertGoal(Goal goal);

    @Update
    void updateGoal(Goal goal);

    @Query("SELECT * FROM goals WHERE month = :month AND year = :year")
    List<Goal> getGoalsForMonth(int year, int month);

    @Query("SELECT * FROM goals WHERE category = :category AND month = :month AND year = :year LIMIT 1")
    Goal findGoal(String category, int month, int year);
}
