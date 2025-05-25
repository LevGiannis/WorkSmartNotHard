package com.example.worksmartnothard.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DailyEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDailyEntry(DailyEntry entry);

    // Για την αρχική οθόνη (πρόοδος ανά κατηγορία στον τρέχοντα μήνα)
    @Query("SELECT * FROM daily_entries WHERE category = :category AND date LIKE :yearMonth || '%'")
    List<DailyEntry> getEntriesForCategoryInMonth(String category, String yearMonth);

    // Για προβολή όλων των καταχωρίσεων μιας ημέρας (ιστορικό)
    @Query("SELECT * FROM daily_entries WHERE date = :specificDate")
    List<DailyEntry> getEntriesForDate(String specificDate);
}
