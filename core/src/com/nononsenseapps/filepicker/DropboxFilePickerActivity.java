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

import android.os.Bundle;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.nononsenseapps.build.Config;

public class DropboxFilePickerActivity extends
        AbstractFilePickerActivity<DbxPath> {

    private DbxFileSystem fs;

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
        DropboxFilePickerFragment fragment = new DropboxFilePickerFragment(fs);
        fragment.setArgs(startPath, mode, allowMultiple, allowCreateDir);
        return fragment;
    }
}
