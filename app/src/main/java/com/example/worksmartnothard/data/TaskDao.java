package com.example.worksmartnothard.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY dateCreated DESC")
    List<Task> getAllTasks();

    @Insert
    long insertTask(Task task);

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    Task getTaskById(int id);

    @Update
    void updateTask(Task task);

    @Delete
    void deleteTask(Task task);
}
