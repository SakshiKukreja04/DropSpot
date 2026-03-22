package com.example.dropspot;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_NAME = "userName";
    private static final String KEY_EMAIL = "userEmail";
    private static final String KEY_PHOTO_URL = "userPhotoUrl";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveUser(String name, String email, String photoUrl) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PHOTO_URL, photoUrl);
        editor.apply();
    }

    public void savePhotoUrl(String photoUrl) {
        editor.putString(KEY_PHOTO_URL, photoUrl);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserName() {
        return pref.getString(KEY_NAME, "User");
    }

    public String getUserEmail() {
        return pref.getString(KEY_EMAIL, "");
    }

    public String getUserPhotoUrl() {
        return pref.getString(KEY_PHOTO_URL, null);
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
