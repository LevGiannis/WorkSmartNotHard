package com.example.worksmartnothard.data;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {

    private static final String PREFS_NAME = "worksmart_prefs";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_STORE_CODE = "store_code";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // --- Στοιχεία χρήστη ---
    public static void saveUserInfo(Context context, String nickname, String storeCode) {
        getPrefs(context).edit()
                .putString(KEY_NICKNAME, nickname)
                .putString(KEY_STORE_CODE, storeCode)
                .apply();
    }

    public static String getNickname(Context context) {
        return getPrefs(context).getString(KEY_NICKNAME, "Χρήστης");
    }

    public static String getStoreCode(Context context) {
        return getPrefs(context).getString(KEY_STORE_CODE, "Κατάστημα");
    }

    // --- Κατάσταση onboarding ---
    public static boolean isOnboardingCompleted(Context context) {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public static void setOnboardingCompleted(Context context, boolean completed) {
        getPrefs(context).edit()
                .putBoolean(KEY_ONBOARDING_COMPLETED, completed)
                .apply();
    }
}
