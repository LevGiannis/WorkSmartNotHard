package com.example.worksmartnothard.util;

import com.example.worksmartnothard.data.DailyEntry;
import com.example.worksmartnothard.model.CategoryProgress;

import java.util.List;

public class BonusCalculator {

    private static final String CATEGORY_VODAFONE_HOME = "Vodafone Home W/F";

    /**
     * Υπολογισμός bonus για ΜΙΑ εγγραφή DailyEntry
     * με βάση category + homeSubtype + count.
     */
    public static double computeBonusForEntry(DailyEntry entry) {
        if (entry == null) return 0.0;

        String category = entry.category != null ? entry.category.trim() : "";
        String subtype  = entry.homeSubtype != null ? entry.homeSubtype.trim() : "";
        double count    = entry.count;

        if (count <= 0) return 0.0;

        // 🔹 Ραντεβού: ειδική μεταχείριση, γίνεται πιο κάτω συνολικά
        if ("Ραντεβού".equals(category)) {
            return 0.0;
        }

        // 🔹 Vodafone Home W/F – bonus από υποκατηγορία
        if (CATEGORY_VODAFONE_HOME.equals(category)) {
            return computeVodafoneHomeBonus(subtype, count);
        }

        // 🔹 Όλες οι υπόλοιπες κατηγορίες
        return computeBonusForCategoryName(category, count);
    }

    /**
     * Bonus για «απλές» κατηγορίες (όχι Vodafone Home W/F).
     */
    private static double computeBonusForCategoryName(String name, double count) {
        switch (name) {
            case "PortIN mobile":
                return count * 7.0;

            case "Exprepay":
                return count * 3.0;

            case "Migration FTTH":
                return count * 12.0;

            case "Post2post":
                return count * 3.0;

            case "Ec2post":
                return count * 3.0;

            case "First":
                return count * 3.0;

            case "New Connection":
                return count * 7.0;

            case "TV":
                return count * 7.0;

            case "Migration VDSL":
                return count * 4.0;

            case "Συσκευές":
                return 0.0;

            default:
                return 0.0;
        }
    }

    /**
     * Bonus για ΥΠΟΚΑΤΗΓΟΡΙΕΣ Vodafone Home W/F.
     */
    private static double computeVodafoneHomeBonus(String subtype, double count) {
        if (subtype == null || count <= 0) return 0.0;

        switch (subtype) {
            case "ADSL 24":
                return count * 5.0;

            case "ADSL 24 TRIPLE":
                return count * 20.0;

            case "VDSL":
                return count * 12.0;

            case "VDSL TRIPLE":
                return count * 30.0;

            case "DOUBLE 300/500/1000":
                return count * 20.0;

            case "TRIPLE 300/500/1000":
                return count * 35.0;

            case "FWA":
                return count * 10.0;

            default:
                return 0.0;
        }
    }

    /**
     * Υπολογισμός συνολικού bonus για έναν μήνα
     * από λίστα DailyEntry.
     *
     * – Όλες οι κατηγορίες: computeBonusForEntry(...)
     * – Ραντεβού: στο τέλος με 0.10 / 0.15 / 0.20
     */
    public static double computeBonusForMonth(List<DailyEntry> entries) {
        if (entries == null || entries.isEmpty()) return 0.0;

        double total = 0.0;
        double totalAppointmentsAmount = 0.0;

        for (DailyEntry e : entries) {
            if (e == null) continue;

            String category = e.category != null ? e.category.trim() : "";

            if ("Ραντεβού".equals(category)) {
                // count = ποσό €
                totalAppointmentsAmount += e.count;
            } else {
                total += computeBonusForEntry(e);
            }
        }

        if (totalAppointmentsAmount > 0) {
            total += computeAppointmentsBonus(totalAppointmentsAmount);
        }

        return total;
    }

    /**
     * Bonus για ΡΑΝΤΕΒΟΥ:
     *  - 0   έως < 900€   → 10%
     *  - 900 έως < 1200€  → 15%
     *  - ≥ 1200€          → 20%
     */
    public static double computeAppointmentsBonus(double totalAmount) {
        if (totalAmount <= 0) return 0.0;

        if (totalAmount < 900.0) {
            return totalAmount * 0.10;
        } else if (totalAmount < 1200.0) {
            return totalAmount * 0.15;
        } else {
            return totalAmount * 0.20;
        }
    }

    /**
     * Υπολογισμός bonus από CategoryProgress
     * (χρησιμοποιείται ΜΟΝΟ για απλές κατηγορίες).
     */
    public static double calculateBonusForCategory(CategoryProgress p) {
        if (p == null || p.category == null) return 0.0;

        String category = p.category.trim();

        // Για Vodafone Home W/F δεν μπορούμε από μόνο το achieved,
        // οπότε χρησιμοποιούμε computeBonusForMonth(...) από DailyEntry
        if (CATEGORY_VODAFONE_HOME.equals(category)) {
            return 0.0;
        }

        return computeBonusForCategoryName(category, p.achieved);
    }
}
