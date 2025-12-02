package com.example.worksmartnothard.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DailyEntryDao {

    @Insert
    void insertDailyEntry(DailyEntry entry);

    @Query("SELECT * FROM DailyEntry WHERE date = :date")
    List<DailyEntry> getEntriesForDate(String date);

    @Query("SELECT * FROM DailyEntry " +
            "WHERE category = :category AND substr(date, 1, 7) = :yearMonth")
    List<DailyEntry> getEntriesForCategoryInMonth(String category, String yearMonth);

    @Query("SELECT * FROM DailyEntry WHERE substr(date, 1, 7) = :yearMonth")
    List<DailyEntry> getEntriesForMonth(String yearMonth);

    // (προαιρετικά – αν δεν το χρειάζεσαι πουθενά, μπορείς να το σβήσεις)
    @Query("SELECT * FROM DailyEntry " +
            "WHERE category = 'Vodafone Home W/F' AND substr(date, 1, 7) = :yearMonth")
    List<DailyEntry> getVodafoneHomeEntriesForMonth(String yearMonth);
}
