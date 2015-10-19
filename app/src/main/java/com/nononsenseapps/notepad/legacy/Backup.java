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

package com.nononsenseapps.notepad.legacy;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.sync.files.JSONBackup;

import java.io.FileNotFoundException;

/**
 * Legacy backup importer.
 */
public class Backup {

    public static void importLegacyBackup(@NonNull Context context) {

        JSONBackup backupMaker = new JSONBackup(context);

    }

    private class RestoreBackupTask extends AsyncTask<Void, Void, Integer> {
        private final Context context;
        private final JSONBackup backupMaker;

        /**
         * Creates a new asynchronous task. This constructor must be invoked on the UI thread.
         */
        public RestoreBackupTask(@NonNull Context context, @NonNull JSONBackup backupMaker) {
            super();
            this.context = context;
            this.backupMaker = backupMaker;
        }

        protected Integer doInBackground(Void... params) {
            try {
                backupMaker.restoreBackup();
                return 0;
            } catch (FileNotFoundException e) {
                return 1;
            } catch (Exception e) {
                return 2;
            }
        }

        protected void onPostExecute(final Integer result) {
            switch (result) {
                case 0:
                    Toast.makeText(context, R.string.backup_import_success, Toast.LENGTH_SHORT)
                            .show();
                    break;
                case 1:
                    Toast.makeText(context, R.string.backup_file_not_found, Toast.LENGTH_SHORT)
                            .show();
                    break;
                case 2:
                    Toast.makeText(context, R.string.backup_import_failed, Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
        }
    }

}
