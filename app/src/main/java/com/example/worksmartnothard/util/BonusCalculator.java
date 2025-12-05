package com.example.worksmartnothard.util;

import com.example.worksmartnothard.data.DailyEntry;

import java.util.List;

public class BonusCalculator {

    /**
     * Υπολογισμός συνολικού bonus για έναν μήνα,
     * με βάση όλες τις ημερήσιες καταχωρήσεις (DailyEntry).
     */
    public static double calculateMonthlyBonus(List<DailyEntry> entries) {
        if (entries == null || entries.isEmpty()) return 0.0;

        double totalBonus = 0.0;

        // Σύνολο ποσού ραντεβού (σε €) για τον μήνα
        double appointmentsAmount = 0.0;

        // Vodafone Home W/F ανά υποτύπο
        double adsl24 = 0.0;
        double adsl24Triple = 0.0;
        double vdsl = 0.0;
        double vdslTriple = 0.0;
        double double300 = 0.0;
        double triple300 = 0.0;
        double fwa = 0.0;

        for (DailyEntry e : entries) {
            if (e == null || e.category == null) continue;

            String category = e.category.trim();
            double count = e.count;

            switch (category) {
                case "PortIN mobile":
                    totalBonus += count * 7.0;
                    break;

                case "Exprepay":
                    totalBonus += count * 3.0;
                    break;

                case "Migration FTTH":
                    totalBonus += count * 7.0;
                    break;

                case "Post2post":
                    totalBonus += count * 3.0;
                    break;

                case "Ec2post":
                    totalBonus += count * 3.0;
                    break;

                case "First":
                    totalBonus += count * 3.0;
                    break;

                case "New Connection":
                    totalBonus += count * 7.0;
                    break;

                case "TV":
                    totalBonus += count * 7.0;
                    break;

                case "Migration VDSL":
                    totalBonus += count * 4.0;
                    break;

                case "Συσκευές":
                    // Δεν έχει bonus
                    break;

                case "Ραντεβού":
                    // Ποσό σε €, κλίμακα υπολογίζεται στο τέλος
                    appointmentsAmount += count;
                    break;

                case "Vodafone Home W/F":
                    // 👇 ΕΔΩ ήταν το πρόβλημα αν έλειπε το "e."
                    String subtype = (e.homeType == null) ? "" : e.homeType.trim();
                    switch (subtype) {
                        case "ADSL 24":
                            adsl24 += count;
                            break;
                        case "ADSL 24 TRIPLE":
                            adsl24Triple += count;
                            break;
                        case "VDSL":
                            vdsl += count;
                            break;
                        case "VDSL TRIPLE":
                            vdslTriple += count;
                            break;
                        case "DOUBLE 300/500/1000":
                            double300 += count;
                            break;
                        case "TRIPLE 300/500/1000":
                            triple300 += count;
                            break;
                        case "FWA":
                            fwa += count;
                            break;
                        default:
                            // Άγνωστος υποτύπος -> 0 bonus
                            break;
                    }
                    break;

                default:
                    // Άγνωστη κατηγορία -> 0 bonus
                    break;
            }
        }

        // 🔹 Vodafone Home W/F bonus ανά υποτύπο
        totalBonus += adsl24 * 5.0;
        totalBonus += adsl24Triple * 20.0;
        totalBonus += vdsl * 12.0;
        totalBonus += vdslTriple * 30.0;
        totalBonus += double300 * 20.0;
        totalBonus += triple300 * 35.0;
        totalBonus += fwa * 10.0;

        // 🔹 Bonus από Ραντεβού με κλίμακες
        if (appointmentsAmount > 0) {
            if (appointmentsAmount > 1200) {
                totalBonus += appointmentsAmount * 0.20;
            } else if (appointmentsAmount >= 900) {
                totalBonus += appointmentsAmount * 0.15;
            } else { // 0–900
                totalBonus += appointmentsAmount * 0.10;
            }
        }

        return totalBonus;
    }
}
