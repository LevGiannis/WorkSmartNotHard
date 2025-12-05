package com.example.worksmartnothard.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DailyEntryDao {

    @Insert
    void insertDailyEntry(DailyEntry entry);

    // Όλες οι εγγραφές για μία συγκεκριμένη ημερομηνία (YYYY-MM-DD)
    @Query("SELECT * FROM DailyEntry WHERE date = :date")
    List<DailyEntry> getEntriesForDate(String date);

    // Όλες οι εγγραφές μιας κατηγορίας σε συγκεκριμένο μήνα (YYYY-MM)
    @Query("SELECT * FROM DailyEntry " +
            "WHERE category = :category " +
            "AND substr(date, 1, 7) = :yearMonth")
    List<DailyEntry> getEntriesForCategoryInMonth(String category, String yearMonth);

    // Όλες οι εγγραφές Vodafone Home W/F για συγκεκριμένο μήνα (αν τυχόν το χρειαστούμε)
    @Query("SELECT * FROM DailyEntry " +
            "WHERE category = 'Vodafone Home W/F' " +
            "AND substr(date, 1, 7) = :yearMonth")
    List<DailyEntry> getVodafoneHomeEntriesForMonth(String yearMonth);

    // ✅ ΌΛΕΣ οι εγγραφές ενός μήνα (για υπολογισμό συνολικού bonus)
    @Query("SELECT * FROM DailyEntry WHERE substr(date, 1, 7) = :yearMonth")
    List<DailyEntry> getEntriesForMonth(String yearMonth);
}
