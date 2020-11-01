package com.nhs.youtubedl.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.nhs.youtubedl.Constants;

/**
 * Created by Henry on 2020. 11. 01..
 */
public class SharedPrefsHelper {
    private SharedPrefsHelper() {}

    public static void update(Context appContext, String key, String value) {
        SharedPreferences pref = appContext.getSharedPreferences(Constants.baseName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    @Nullable
    public static String get(Context appContext, String key) {
        SharedPreferences pref = appContext.getSharedPreferences(Constants.baseName, Context.MODE_PRIVATE);
        return pref.getString(key, null);
    }
}
