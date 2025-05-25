package com.example.worksmartnothard.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {DailyEntry.class, Goal.class, Task.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;

    public abstract DailyEntryDao dailyEntryDao();
    public abstract GoalDao goalDao();
    public abstract TaskDao taskDao();  // 👈 Προσθήκη

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "sales_tracker_db"
                    )
                    .fallbackToDestructiveMigration() // Σβήνει και ξαναφτιάχνει τη βάση όταν αλλάζει schema
                    .build();
        }
        return INSTANCE;
    }
}
