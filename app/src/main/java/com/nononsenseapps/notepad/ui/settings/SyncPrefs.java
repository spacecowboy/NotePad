/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.notepad.ui.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.nononsenseapps.build.Config;
import com.nononsenseapps.notepad.ui.orgmodedropbox.DropboxFilePickerActivity;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.notepad.util.Log;
import com.nononsenseapps.notepad.BuildConfig;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.local.sql.MyContentProvider;
import com.nononsenseapps.notepad.data.remote.gtasks.GoogleTasksClient;
import com.nononsenseapps.notepad.data.remote.orgmodedropbox.DropboxSyncHelper;
import com.nononsenseapps.notepad.data.remote.orgmodedropbox.DropboxSynchronizer;
import com.nononsenseapps.notepad.data.service.OrgSyncService;
import com.nononsenseapps.notepad.data.local.orgmode.SDSynchronizer;

import java.io.File;
import java.io.IOException;

// import com.nononsenseapps.notepad.NotePad;

public class SyncPrefs extends PreferenceFragment implements
        OnSharedPreferenceChangeListener {

    public static final String KEY_SYNC_ENABLE = "syncEnablePref";
    public static final String KEY_ACCOUNT = "accountPref";
    // public static final String KEY_SYNC_FREQ = "syncFreq";
    public static final String KEY_FULLSYNC = "syncFull";
    public static final String KEY_SYNC_ON_START = "syncOnStart";
    public static final String KEY_SYNC_ON_CHANGE = "syncOnChange";
    public static final String KEY_BACKGROUND_SYNC = "syncInBackground";
    // Used for sync on start and on change
    public static final String KEY_LAST_SYNC = "lastSync";

    // SD sync
    public static final String KEY_SD_ENABLE = "pref_sync_sd_enabled";
    public static final String KEY_SD_DIR = "pref_sync_sd_dir";
    // Dropbox sync
    public static final String KEY_DROPBOX_ENABLE = "pref_sync_dropbox_enabled";
    public static final String KEY_DROPBOX_DIR = "pref_sync_dropbox_dir";
    private static final int PICK_SD_DIR_CODE = 1;
    private static final int PICK_DROPBOX_DIR_CODE = 2;


    private Activity activity;

    private Preference prefAccount;
    private Preference prefSdDir;
    private Preference prefDropboxDir;
    private DropboxSyncHelper mDropboxHelper = null;

    // private Preference prefSyncFreq;

    /**
     * Finds and returns the account of the name given
     *
     * @param accountName
     * @return
     */
    public static Account getAccount(AccountManager manager, String accountName) {
        Account[] accounts = manager.getAccountsByType("com.google");
        for (Account account : accounts) {
            if (account.name.equals(accountName)) {
                return account;
            }
        }
        return null;
    }

    public static void setSyncInterval(Context activity,
                                       SharedPreferences sharedPreferences) {
        String accountName = sharedPreferences.getString(KEY_ACCOUNT, "");
        boolean backgroundSync = sharedPreferences.getBoolean(
                KEY_BACKGROUND_SYNC, false);

        if (accountName != null && !accountName.isEmpty()) {
            Account account = getAccount(AccountManager.get(activity), accountName);
            if (account != null) {
                if (!backgroundSync) {
                    // Disable periodic syncing
                    ContentResolver.removePeriodicSync(
                            account,
                            MyContentProvider.AUTHORITY, new Bundle());
                } else {
                    // Convert from minutes to seconds
                    long pollFrequency = 3600;
                    // Set periodic syncing
                    ContentResolver.addPeriodicSync(
                            account,
                            MyContentProvider.AUTHORITY, new Bundle(),
                            pollFrequency);
                }
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mDropboxHelper != null) {
            if (mDropboxHelper.handleLinkResult()) {
                // Success
            } else {
                // Link failed or was cancelled by the user.
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean
                        (KEY_DROPBOX_ENABLE, false).commit();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.app_pref_sync);

        prefAccount = findPreference(KEY_ACCOUNT);
        // prefSyncFreq = findPreference(KEY_SYNC_FREQ);


        final SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(activity);
        // Set up a listener whenever a key changes
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);

        // Set summaries

        setAccountTitle(sharedPrefs);

        prefAccount
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        // Show dialog
                        showAccountDialog();
                        return true;
                    }
                });

        // Disable prefs if this is not correct build
        findPreference(KEY_SYNC_ENABLE).setEnabled(
                null != Config.getGtasksApiKey(getActivity()) &&
                !Config.getGtasksApiKey(getActivity()).contains(" "));

        // SD Card
        prefSdDir = findPreference(KEY_SD_DIR);
        setSdDirSummary(sharedPrefs);
        prefSdDir.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                // Start the filepicker
                Intent i = new Intent(getActivity(), FilePickerActivity.class);

                i.putExtra(FilePickerActivity.EXTRA_START_PATH,
                        sharedPrefs.getString(KEY_SD_DIR,
                                SDSynchronizer.DEFAULT_ORG_DIR)
                )
                        .putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)
                        .putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true)
                        .putExtra(FilePickerActivity.EXTRA_MODE,
                                FilePickerActivity.MODE_DIR);
                startActivityForResult(i, PICK_SD_DIR_CODE);
                return true;
            }
        });

        if (BuildConfig.DROPBOX_ENABLED && BuildConfig.NONFREE) {
            // Dropbox, disable if no key present
            findPreference(KEY_DROPBOX_ENABLE)
                    .setEnabled(BuildConfig.DROPBOX_ENABLED &&
                                Config.getKeyDropboxAPI(getActivity()) !=
                                null &&
                                !Config.getKeyDropboxAPISecret(getActivity())
                                        .contains(" "));
            prefDropboxDir = findPreference(KEY_DROPBOX_DIR);
            prefDropboxDir.setEnabled(BuildConfig.DROPBOX_ENABLED);
            setDropboxDirSummary(sharedPrefs);
            prefDropboxDir.setOnPreferenceClickListener(
                    new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(
                                final Preference preference) {
                            // See if initial sync is complete
                            if (mDropboxHelper == null) {
                                mDropboxHelper = new DropboxSyncHelper(getActivity());
                            }
                            if (mDropboxHelper.isLinked()) {
                                // Start the filepicker
                                Intent i = new Intent(getActivity(),
                                        DropboxFilePickerActivity.class);
                                i.putExtra(FilePickerActivity.EXTRA_START_PATH,
                                        sharedPrefs.getString(KEY_DROPBOX_DIR,
                                                DropboxSynchronizer.DEFAULT_DIR));
                                i.putExtra(
                                        FilePickerActivity.EXTRA_ALLOW_MULTIPLE,
                                        false).putExtra(
                                        FilePickerActivity.EXTRA_ALLOW_CREATE_DIR,
                                        true)
                                        .putExtra(FilePickerActivity.EXTRA_MODE,
                                                FilePickerActivity.MODE_DIR);
                                i.addFlags(
                                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                startActivityForResult(i,
                                        PICK_DROPBOX_DIR_CODE);

                            }

                            return true;
                        }
                    });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(activity)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_DROPBOX_DIR_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString
                        (KEY_DROPBOX_DIR, data.getData().getPath()).commit();
            } // else was cancelled
        } else if (requestCode == PICK_SD_DIR_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // Set it
                File path = new File(data.getData().getPath());
                if (path.exists() && path.isDirectory() && path.canWrite()) {
                    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString
                            (KEY_SD_DIR, path.toString()).commit();
                } else {
                    Toast.makeText(getActivity(), R.string.cannot_write_to_directory, Toast
                            .LENGTH_SHORT).show();
                }
            } // else was cancelled
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void showAccountDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("accountdialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        // Bundle args = new Bundle();
        // args.putString(KEY_ACCOUNT, newPassword);
        DialogFragment newFragment = new AccountDialog();
        // newFragment.setArguments(args);
        newFragment.show(ft, "accountdialog");
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs,
                                          String key) {
        try {
            Log.d("syncPrefs", "onChanged");
            if (activity.isFinishing()) {
                // Setting the summary now would crash it with
                // IllegalStateException since we are not attached to a view
            } else {
                if (KEY_SYNC_ENABLE.equals(key)) {
                    toggleSync(prefs);
                } else if (KEY_BACKGROUND_SYNC.equals(key)) {
                    setSyncInterval(activity, prefs);
                } else if (KEY_ACCOUNT.equals(key)) {
                    Log.d("syncPrefs", "account");
                    prefAccount.setTitle(prefs.getString(
                            KEY_ACCOUNT, ""));
                } else if (KEY_SD_ENABLE.equals(key)) {
                    // Restart the sync service
                    OrgSyncService.stop(getActivity());
                } else if (KEY_SD_DIR.equals(key)) {
                    setSdDirSummary(prefs);
                } else if (KEY_DROPBOX_ENABLE.equals(key)) {
                    // TODO
                    if (mDropboxHelper == null) {
                        mDropboxHelper = new DropboxSyncHelper(getActivity());
                    }
                    if (prefs.getBoolean(key, false)) {
                        // authorize the user
                        mDropboxHelper.linkAccount();
//                        DropboxSynchronizer.linkAccount(this,
//                                DROPBOX_LINK_CODE);
                    } else {
                        mDropboxHelper.unlinkAccount();
//                        DropboxSynchronizer.unlink(getActivity());
                    }
                    // Restart sync service
                    OrgSyncService.stop(getActivity());
                } else if (KEY_DROPBOX_DIR.equals(key)) {
                    setDropboxDirSummary(prefs);
                }
            }
        } catch (IllegalStateException e) {
            // This is just in case the "isFinishing" wouldn't be enough
            // The isFinishing will try to prevent us from doing something
            // stupid
            // This catch prevents the app from crashing if we do something
            // stupid
        }

    }

    private void toggleSync(SharedPreferences sharedPreferences) {
        boolean enabled = sharedPreferences.getBoolean(KEY_SYNC_ENABLE, false);
        String accountName = sharedPreferences.getString(KEY_ACCOUNT, "");

        if (accountName != null && !accountName.isEmpty()) {
            Account account = getAccount(AccountManager.get(activity), accountName);
            if (account != null) {
                if (enabled) {
                    // set syncable
                    ContentResolver.setSyncAutomatically(
                            account,
                            MyContentProvider.AUTHORITY, true);
                    ContentResolver.setIsSyncable(
                            account,
                            MyContentProvider.AUTHORITY, 1);
                    // Also set sync frequency
                    setSyncInterval(activity, sharedPreferences);
                } else {
                    // set unsyncable
                    // ContentResolver.setIsSyncable(
                    // getAccount(AccountManager.get(activity), accountName),
                    // MyContentProvider.AUTHORITY, 0);
                }
            }
        } else if (enabled) {
            showAccountDialog();
        }
    }

    private void setAccountTitle(final SharedPreferences sharedPreferences) {
        prefAccount.setTitle(sharedPreferences.getString(KEY_ACCOUNT, ""));
        prefAccount.setSummary(R.string.settings_account_summary);
    }

    private void setSdDirSummary(final SharedPreferences sharedPreferences) {
        prefSdDir.setSummary(sharedPreferences.getString(KEY_SD_DIR,
                SDSynchronizer.DEFAULT_ORG_DIR));
    }

    private void setDropboxDirSummary(final SharedPreferences
                                              sharedPreferences) {
        prefDropboxDir.setSummary(sharedPreferences.getString(KEY_DROPBOX_DIR,
                DropboxSynchronizer.DEFAULT_DIR));
    }

    public static class AccountDialog extends DialogFragment implements
            AccountManagerCallback<Bundle> {
        private Activity activity;
        private Account account;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            this.activity = activity;
        }

        @Override
        public Dialog onCreateDialog(Bundle args) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.select_account);
            final Account[] accounts = AccountManager.get(activity)
                    .getAccountsByType("com.google");
            final int size = accounts.length;
            String[] names = new String[size];
            for (int i = 0; i < size; i++) {
                names[i] = accounts[i].name;
            }
            // TODO
            // Could add a clear alternative here
            builder.setItems(names, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // Stuff to do when the account is selected by the user
                    accountSelected(accounts[which]);
                }
            });
            return builder.create();
        }

        /**
         * Called from the activity, since that one builds the dialog
         *
         * @param account
         */
        public void accountSelected(final Account account) {
            if (account != null) {
                Log.d("prefsActivity", "step one");
                this.account = account;
                // Request user's permission
                GoogleTasksClient.getAuthTokenAsync(activity, account, this);
                // work continues in callback, method run()
            }
        }

        /**
         * User wants to select an account to sync with. If we get an approval,
         * activate sync and set periodicity also.
         */
        @SuppressLint("CommitPrefEdits")
        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            try {
                Log.d("prefsActivity", "step two");
                // If the user has authorized
                // your application to use the
                // tasks API
                // a token is available.
                String token = future.getResult().getString(
                        AccountManager.KEY_AUTHTOKEN);
                // Now we are authorized by the user.
                Log.d("prefsActivity", "step two-b: " + token);

                if (token != null && !token.isEmpty() && account != null) {
                    Log.d("prefsActivity", "step three: " + account.name);
                    SharedPreferences customSharedPreference = PreferenceManager
                            .getDefaultSharedPreferences(activity);
                    customSharedPreference.edit()
                            .putString(SyncPrefs.KEY_ACCOUNT, account.name)
                            .putBoolean(KEY_SYNC_ENABLE, true).commit();

                    // Set it syncable
                    ContentResolver.setSyncAutomatically(account,
                            MyContentProvider.AUTHORITY, true);
                    ContentResolver.setIsSyncable(account,
                            MyContentProvider.AUTHORITY, 1);
                    // Set sync frequency
                    SyncPrefs.setSyncInterval(activity, customSharedPreference);
                }
            } catch (OperationCanceledException e) {
                // if the request was canceled for any reason
            } catch (AuthenticatorException e) {
                // if there was an error communicating with the authenticator or
                // if the authenticator returned an invalid response
            } catch (IOException e) {
                // if the authenticator returned an error response that
                // indicates that it encountered an IOException while
                // communicating with the authentication server
            }

        }
    }
}
