package com.example.worksmartnothard.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull public String name;         // Όνομα πελάτη
    @NonNull public String phone;        // Κινητό
    @NonNull public String afm;          // ΑΦΜ
    @NonNull public String description;  // Περιγραφή εκκρεμότητας
    @NonNull public String dateCreated;  // Ημερομηνία
    public boolean done;

    public Task(@NonNull String name,
                @NonNull String phone,
                @NonNull String afm,
                @NonNull String description,
                boolean done,
                @NonNull String dateCreated) {
        this.name = name;
        this.phone = phone;
        this.afm = afm;
        this.description = description;
        this.done = done;
        this.dateCreated = dateCreated;
    }
}
