// Copyright ® 2022 OneSpan North America, Inc. All rights reserved. 

 
/////////////////////////////////////////////////////////////////////////////
//
//
// This file is example source code. It is provided for your information and
// assistance. See your licence agreement for details and the terms and
// conditions of the licence which governs the use of the source code. By using
// such source code you will be accepting these terms and conditions. If you do
// not wish to accept these terms and conditions, DO NOT OPEN THE FILE OR USE
// THE SOURCE CODE.
//
// Note that there is NO WARRANTY.
//
//////////////////////////////////////////////////////////////////////////////


package com.vasco.orchestration.sample.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.vasco.orchestration.sample.R;

public class SharedPreferencesStorage {

    private final SharedPreferences sharedPref;
    private final Context context;

    public SharedPreferencesStorage(Context context) {
        sharedPref =
                context.getSharedPreferences(
                        context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        this.context = context;
    }

    // region user management
    public String getCurrentUser() {
        return sharedPref.getString(context.getString(R.string.pref_userIdentifier), null);
    }

    public void setCurrentUser(String currentUser) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(context.getString(R.string.pref_userIdentifier), currentUser);
        editor.apply();
    }
    // endregion

    // region Notification management
    public String getStoredNotificationIdForUser(String userIdentifier) {
        return sharedPref.getString(
                context.getString(R.string.pref_notificationId) + userIdentifier, null);
    }

    public void storeNotificationIdForUser(String userIdentifier, String notificationId) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(
                context.getString(R.string.pref_notificationId) + userIdentifier, notificationId);
        editor.apply();
    }

    public void removeNotificationIdForUser(String userIdentifier) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(context.getString(R.string.pref_notificationId) + userIdentifier);
        editor.apply();
    }
    // endregion

}
