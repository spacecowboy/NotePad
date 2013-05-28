/**
 * Copyright (C) 2011, Karsten Priegnitz
 *
 * Permission to use, copy, modify, and distribute this piece of software
 * for any purpose with or without fee is hereby granted, provided that
 * the above copyright notice and this permission notice appear in the
 * source code of all copies.
 *
 * It would be appreciated if you mention the author in your change log,
 * contributors list or the like.
 *
 * @author: Karsten Priegnitz
 * @see: http://code.google.com/p/android-change-log/
 * 
 * 
 * Modified by Jonas Kalderstam 2012
 */
package sheetrock.panda.changelog;

import com.nononsenseapps.notepad.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import com.nononsenseapps.helpers.Log;
import android.view.LayoutInflater;
import android.view.View;

public class ChangeLog {
    private static final String TAG = "ChangeLog";
    
    private final Context context;
    private String lastVersion, thisVersion;

    // this is the key for storing the version name in SharedPreferences
    private static final String VERSION_KEY = "PREFS_VERSION_KEY";
    
    /**
     * Constructor
     *
     * Retrieves the version names and stores the new version name in
     * SharedPreferences
     * 
     * @param context
     */
    public ChangeLog(Context context) {
        this(context, PreferenceManager.getDefaultSharedPreferences(context));
    }
        
    /**
     * Constructor
     *
     * Retrieves the version names and stores the new version name in
     * SharedPreferences
     * 
     * @param context
     * @param sp  the shared preferences to store the last version name into
     */
    public ChangeLog(Context context, SharedPreferences sp) {
        this.context = context;

        // get version numbers
        this.lastVersion = sp.getString(VERSION_KEY, "");
        Log.d(TAG, "lastVersion: " + lastVersion);
        try {
			this.thisVersion = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			this.thisVersion = "?";
			Log.e(TAG, "could not get version name from manifest!");
			e.printStackTrace();
		}
        Log.d(TAG, "appVersion: " + this.thisVersion);
        
        // save new version number to preferences
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(VERSION_KEY, this.thisVersion);
        editor.commit();
    }
    
    /**
     * @return  The version name of the last installation of this app (as
     *          described in the former manifest). This will be the same as
     *          returned by <code>getThisVersion()</code> the second time
     *          this version of the app is launched (more precisely: the
     *          second time ChangeLog is instantiated).
     * @see AndroidManifest.xml#android:versionName
     */
    public String getLastVersion() {
    	return  this.lastVersion;
    }
    
    /**
     * @return  The version name of this app as described in the manifest.
     * @see AndroidManifest.xml#android:versionName
     */
    public String getThisVersion() {
    	return  this.thisVersion;
    }

    /**
     * @return  <code>true</code> if this version of your app is started the
     *          first time
     */
    public boolean firstRun() {
        return  ! this.lastVersion.equals(this.thisVersion);
    }

    /**
     * @return  <code>true</code> if your app is started the first time ever.
     *          Also <code>true</code> if your app was deinstalled and 
     *          installed again.
     */
    public boolean firstRunEver() {
        return  "".equals(this.lastVersion);
    }

    /**
     * @return  an AlertDialog displaying the changes since the previous
     *          installed version of your app (what's new).
     */
    public AlertDialog getLogDialog() {
        return  this.getDialog(false);
    }

    /**
     * @return  an AlertDialog with a full change log displayed
     */
    public AlertDialog getFullLogDialog() {
        return  this.getDialog(true);
    }
    
    private AlertDialog getDialog(boolean full) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        View view               = inflater.inflate(R.layout.changelog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        
        builder.setView(view).setTitle(R.string.changelog_title)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        return  builder.create();   
    }

    /**
     * manually set the last version name - for testing purposes only
     * @param lastVersion
     */
    void setLastVersion(String lastVersion) {
    	this.lastVersion = lastVersion;
    }
}