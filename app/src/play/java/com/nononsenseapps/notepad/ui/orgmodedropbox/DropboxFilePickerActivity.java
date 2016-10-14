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

package com.nononsenseapps.notepad.ui.orgmodedropbox;

import android.os.Bundle;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.nononsenseapps.filepicker.AbstractFilePickerActivity;
import com.nononsenseapps.filepicker.AbstractFilePickerFragment;
import com.nononsenseapps.notepad.data.remote.orgmodedropbox.DropboxSyncHelper;

public class DropboxFilePickerActivity extends
        AbstractFilePickerActivity<DropboxAPI.Entry> {

    // In the class declaration section:
    private DropboxAPI<AndroidAuthSession> mDBApi;

    @Override
    public void onCreate(Bundle b) {
        mDBApi = DropboxSyncHelper.getDBApi(this);
        if (!mDBApi.getSession().isLinked()) {
            // No valid authentication
            finish();
        }

        super.onCreate(b);
    }

    @Override
    protected AbstractFilePickerFragment<DropboxAPI.Entry> getFragment(
            final String startPath, final int mode, final boolean allowMultiple,
            final boolean allowCreateDir) {
        if (mDBApi == null || !mDBApi.getSession().isLinked()) {
            // No valid authentication
            finish();
            return null;
        }

        DropboxFilePickerFragment fragment =
                new DropboxFilePickerFragment(mDBApi);
        fragment.setArgs(startPath, mode, allowMultiple, allowCreateDir);
        return fragment;
    }

}
