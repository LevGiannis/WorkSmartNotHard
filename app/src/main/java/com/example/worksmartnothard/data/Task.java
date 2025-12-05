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
    @NonNull public String dateCreated;  // Ημερομηνία δημιουργίας
    @NonNull public String type;         // Τύπος / κατηγορία (PortIN, Home κλπ)
    @NonNull public String dueDate;      // Προθεσμία (yyyy-MM-dd)
    public boolean done;

    public Task(@NonNull String name,
                @NonNull String phone,
                @NonNull String afm,
                @NonNull String description,
                @NonNull String dateCreated,
                @NonNull String type,
                @NonNull String dueDate,
                boolean done) {

        this.name = name;
        this.phone = phone;
        this.afm = afm;
        this.description = description;
        this.dateCreated = dateCreated;
        this.type = type;
        this.dueDate = dueDate;
        this.done = done;
    }
}
