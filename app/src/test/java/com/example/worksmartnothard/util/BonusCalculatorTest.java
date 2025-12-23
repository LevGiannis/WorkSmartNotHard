package com.example.worksmartnothard.util;

import static org.junit.Assert.assertEquals;

import com.example.worksmartnothard.data.DailyEntry;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BonusCalculatorTest {

    @Test
    public void emptyEntries_returnZero() {
        assertEquals(0.0, BonusCalculator.calculateDailyBonus(Collections.emptyList()), 0.0001);
        assertEquals(0.0, BonusCalculator.calculateMonthlyBonus(Collections.emptyList()), 0.0001);
    }

    @Test
    public void perCategoryMultipliers_areApplied() {
        List<DailyEntry> entries = Arrays.asList(
                new DailyEntry("PortIN mobile", "2025-12-01", 2, null), // 2 * 7 = 14
                new DailyEntry("Exprepay", "2025-12-01", 3, null), // 3 * 3 = 9
                new DailyEntry("Migration FTTH", "2025-12-01", 1, null), // 1 * 12 = 12
                new DailyEntry("TV", "2025-12-01", 1, null) // 1 * 7 = 7
        );

        double expected = 14 + 9 + 12 + 7;
        assertEquals(expected, BonusCalculator.calculateDailyBonus(entries), 0.0001);
    }

    @Test
    public void vodafoneHome_subtypeBonus_isApplied() {
        List<DailyEntry> entries = Arrays.asList(
                new DailyEntry("Vodafone Home W/F", "2025-12-01", 1, "ADSL 24"), // 1 * 5
                new DailyEntry("Vodafone Home W/F", "2025-12-01", 2, "VDSL TRIPLE"), // 2 * 30
                new DailyEntry("Vodafone Home W/F", "2025-12-01", 1, "TRIPLE 300/500/1000") // 1 * 35
        );

        double expected = 1 * 5.0 + 2 * 30.0 + 1 * 35.0;
        assertEquals(expected, BonusCalculator.calculateDailyBonus(entries), 0.0001);
    }

    @Test
    public void appointments_scale_isAppliedOnTotalAmount() {
        // total 1000 => 15%
        List<DailyEntry> entries = Arrays.asList(
                new DailyEntry("Ραντεβού", "2025-12-01", 600, null),
                new DailyEntry("Ραντεβού", "2025-12-01", 400, null));

        double expected = 1000.0 * 0.15;
        assertEquals(expected, BonusCalculator.calculateDailyBonus(entries), 0.0001);
    }

    @Test
    public void dailyAndMonthly_areSameLogic_givenSameEntries() {
        List<DailyEntry> entries = Arrays.asList(
                new DailyEntry("PortIN mobile", "2025-12-01", 1, null),
                new DailyEntry("Ραντεβού", "2025-12-01", 100, null));

        assertEquals(
                BonusCalculator.calculateMonthlyBonus(entries),
                BonusCalculator.calculateDailyBonus(entries),
                0.0001);
    }
}
