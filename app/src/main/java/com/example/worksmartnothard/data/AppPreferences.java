
package com.example.worksmartnothard.data;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {

    // Επιστρέφει το index του accent color (0=μωβ, 1=μπλε, 2=μαύρο, 3=γκρι, 4=ροζ)
    public static int getAccentColor(Context context) {
        SharedPreferences prefs = getPrefs(context);
        // Αν δεν έχει οριστεί, default = 3 (γκρι)
        return prefs.getInt("accent_color_index", 3);
    }

    private static final String PREFS_NAME = "worksmart_prefs";

    // --- Keys χρήστη ---
    private static final String KEY_FIRST_NAME = "first_name";
    private static final String KEY_LAST_NAME = "last_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_STORE_CODE = "store_code";

    // --- Onboarding ---
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";

    // --- Report Email (παραλήπτης Excel/CSV) ---
    private static final String KEY_REPORT_EMAIL = "report_email";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // -------------------------
    // --- First Name ---
    // -------------------------
    public static void setFirstName(Context context, String firstName) {
        getPrefs(context).edit().putString(KEY_FIRST_NAME, firstName).apply();
    }

    public static String getFirstName(Context context) {
        return getPrefs(context).getString(KEY_FIRST_NAME, "");
    }

    // -------------------------
    // --- Last Name ---
    // -------------------------
    public static void setLastName(Context context, String lastName) {
        getPrefs(context).edit().putString(KEY_LAST_NAME, lastName).apply();
    }

    public static String getLastName(Context context) {
        return getPrefs(context).getString(KEY_LAST_NAME, "");
    }

    // -------------------------
    // --- Email ---
    // -------------------------
    public static void setEmail(Context context, String email) {
        getPrefs(context).edit().putString(KEY_EMAIL, email).apply();
    }

    public static String getEmail(Context context) {
        return getPrefs(context).getString(KEY_EMAIL, "");
    }

    // -------------------------
    // --- Nickname ---
    // -------------------------
    public static void setNickname(Context context, String nickname) {
        getPrefs(context).edit().putString(KEY_NICKNAME, nickname).apply();
    }

    public static String getNickname(Context context) {
        return getPrefs(context).getString(KEY_NICKNAME, "Χρήστης");
    }

    // -------------------------
    // --- Store Code ---
    // -------------------------
    public static void setStoreCode(Context context, String storeCode) {
        getPrefs(context).edit().putString(KEY_STORE_CODE, storeCode).apply();
    }

    public static String getStoreCode(Context context) {
        return getPrefs(context).getString(KEY_STORE_CODE, "Κατάστημα");
    }

    // -------------------------
    // --- Βοηθητικό save (προαιρετικό) ---
    // -------------------------
    public static void saveUserInfo(Context context, String nickname, String storeCode) {
        getPrefs(context).edit()
                .putString(KEY_NICKNAME, nickname)
                .putString(KEY_STORE_CODE, storeCode)
                .apply();
    }

    // -------------------------
    // --- Κατάσταση onboarding ---
    // -------------------------
    public static boolean isOnboardingCompleted(Context context) {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public static void setOnboardingCompleted(Context context, boolean completed) {
        getPrefs(context).edit()
                .putBoolean(KEY_ONBOARDING_COMPLETED, completed)
                .apply();
    }

    // -------------------------
    // --- Report Email ---
    // -------------------------
    public static void setReportEmail(Context context, String email) {
        getPrefs(context).edit().putString(KEY_REPORT_EMAIL, email).apply();
    }

    public static String getReportEmail(Context context) {
        return getPrefs(context).getString(KEY_REPORT_EMAIL, "");
    }

    // -------------------------
    // --- Smart fallback για report email ---
    // 1) report email
    // 2) προσωπικό email
    // 3) default
    // -------------------------
    public static String getEffectiveReportEmail(Context context) {
        String report = getReportEmail(context);
        if (report != null && !report.trim().isEmpty()) {
            return report.trim();
        }

        String personal = getEmail(context);
        if (personal != null && !personal.trim().isEmpty()) {
            return personal.trim();
        }

        return "example@gmail.com";
    }
}
