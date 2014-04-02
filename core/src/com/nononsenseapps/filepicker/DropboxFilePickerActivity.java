/*
 * Copyright (c) 2014 Jonas Kalderstam.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.filepicker;

import android.os.AsyncTask;
import android.os.Bundle;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.nononsenseapps.build.Config;
import com.nononsenseapps.notepad.core.R;

public class DropboxFilePickerActivity extends
        AbstractFilePickerActivity<DbxPath> {

    private static final String TAG = "filepicker_fragment";
    private DbxFileSystem fs;
    private String startPath;

    @Override
    public void onCreate(Bundle b) {// Make sure we are linked
        DbxAccountManager accountManager = DbxAccountManager.getInstance(getApplicationContext(),
                Config.getKeyDropboxSyncPublic(this),
                Config.getKeyDropboxSyncSecret(this));

        if (accountManager.hasLinkedAccount()) {
            try {
                fs = DbxFileSystem.forAccount(accountManager.getLinkedAccount());
            } catch (DbxException.Unauthorized unauthorized) {
                finish();
            }
        } else {
            finish();
        }

        super.onCreate(b);
    }

    @Override
    protected AbstractFilePickerFragment<DbxPath> getFragment(final String startPath,
                                                              final int mode,
                                                              final boolean allowMultiple,
                                                              final boolean allowCreateDir) {
        this.startPath = startPath;
        try {
            if (fs.hasSynced()) {
                DropboxFilePickerFragment fragment = new DropboxFilePickerFragment(fs);
                fragment.setArgs(startPath, mode, allowMultiple, allowCreateDir);
                return fragment;
            }
        } catch (DbxException e) {
            // e.printStackTrace();
        }
        // Not synced, try to sync.


        return new DropboxLoadingFragment(fs);
    }

    private class WaitOnSyncTask extends AsyncTask<DbxFileSystem, Void, Void> {

        /**
         * Override this method to perform a computation on a background thread. The
         * specified parameters are the parameters passed to {@link #execute}
         * by the caller of this task.
         * <p/>
         * This method can call {@link #publishProgress} to publish updates
         * on the UI thread.
         *
         * @param params The parameters of the task.
         * @return A result, defined by the subclass of this task.
         * @see #onPreExecute()
         * @see #onPostExecute
         * @see #publishProgress
         */
        @Override
        protected Void doInBackground(final DbxFileSystem... params) {
            try {
                if (fs != null) {
                    fs.awaitFirstSync();
                }
            } catch (DbxException e) {
                // e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void p) {
            AbstractFilePickerFragment<DbxPath> fragment = getFragment(startPath, mode, allowMultiple, allowCreateDir);
            getFragmentManager().beginTransaction().replace(R.id.fragment,
                    fragment, TAG).commit();
        }
    }
}
