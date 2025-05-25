package com.example.worksmartnothard.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "goals")
public class Goal {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String category;

    public int year;
    public int month;

    public double target; // Από int → double

    public Goal(@NonNull String category, int year, int month, double target) {
        this.category = category;
        this.year = year;
        this.month = month;
        this.target = target;
    }
}
