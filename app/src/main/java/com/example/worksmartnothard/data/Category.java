package com.example.worksmartnothard.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories", indices = { @Index(value = { "name" }, unique = true) })
public class Category {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String name;

    public Category(@NonNull String name) {
        this.name = name;
    }
}
