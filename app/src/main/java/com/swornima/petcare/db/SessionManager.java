package com.swornima.petcare.db;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Tracks the currently logged-in local user, replacing FirebaseAuth's
 * session/current-user concept.
 */
public final class SessionManager {

    private static final String PREFS_NAME = "petcare_session";
    private static final String KEY_UID = "current_uid";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setCurrentUserId(String uid) {
        prefs.edit().putString(KEY_UID, uid).apply();
    }

    public String getCurrentUserId() {
        return prefs.getString(KEY_UID, null);
    }

    public boolean isLoggedIn() {
        return getCurrentUserId() != null;
    }

    public void clear() {
        prefs.edit().remove(KEY_UID).apply();
    }
}
