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

package com.nononsenseapps.notepad.sync.orgsync;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFileSystem;
import com.nononsenseapps.build.Config;

/**
 * An {@link IntentService} subclass that synchronizes the Dropbox Cache in
 * the background
 */
public class DropboxSyncHelper extends IntentService {

    public final static String ACTION_SYNC_FULL = "ACTION_SYNC_FULL";
    public final static String ACTION_SYNC_FIRST = "ACTION_SYNC_FIRST";

    /**
     * Execute first sync. If first sync has been completed,
     * this method does nothing.
     */
    public static void doFirstSync(final Context context) {
        Intent intent = new Intent(context, DropboxSyncHelper.class);
        intent.setAction(ACTION_SYNC_FIRST);
        context.startService(intent);
    }

    /**
     * Do a full sync regardless of state.
     */
    public static void doFullSync(final Context context) {
        Intent intent = new Intent(context, DropboxSyncHelper.class);
        intent.setAction(ACTION_SYNC_FULL);
        context.startService(intent);
    }

    /**
     *
     * @param context
     * @return true if we have synced and it's ok to bring up the file picker.
     */
    public static boolean hasSynced(final Context context) {
        final DbxAccountManager accountManager = DbxAccountManager.getInstance
                (context.getApplicationContext(),
                        Config.getKeyDropboxSyncPublic(context),
                        Config.getKeyDropboxSyncSecret(context));

        if (accountManager.hasLinkedAccount()) {
            try {
                final DbxFileSystem fs = DbxFileSystem.forAccount(accountManager
                        .getLinkedAccount());
                return fs.hasSynced();
            } catch (DbxException.Unauthorized ignored) {
            } catch (DbxException ignored) {
            }
        }
        return false;
    }

    public DropboxSyncHelper() {
        super("DropboxSyncHelper");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final DbxAccountManager accountManager = DbxAccountManager.getInstance
                (getApplicationContext(),
                Config.getKeyDropboxSyncPublic(this),
                Config.getKeyDropboxSyncSecret(this));

        if (accountManager.hasLinkedAccount()) {
            try {
                final DbxFileSystem fs = DbxFileSystem.forAccount(accountManager
                        .getLinkedAccount());

                if (ACTION_SYNC_FULL.equals(intent.getAction())) {
                    fs.syncNowAndWait();
                } else if (ACTION_SYNC_FIRST.equals(intent.getAction()) &&
                        !fs.hasSynced()) {
                    fs.syncNowAndWait();
                }

            } catch (DbxException.Unauthorized ignored) {
            } catch (DbxException ignored) {
            }
        }
    }
}
