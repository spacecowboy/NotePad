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

package com.nononsenseapps.notepad.ui.editor;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.local.sql.LegacyDBHelper;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.data.model.sql.TaskList;

/**
 * Container for editor fragment. Only used on phones.
 */
public class ActivityEditor extends ActivityEditorBase implements TaskDetailFragment
        .TaskEditorCallbacks {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public long getEditorTaskId() {
        return getNoteId(getIntent());
    }

    @Override
    public long getListOfTask() {
        return getIntent().getLongExtra(TaskDetailFragment.ARG_ITEM_LIST_ID, -1);
    }

    @Override
    public void closeEditor(Fragment fragment) {
        finish();
    }

    @NonNull
    @Override
    public String getInitialTaskText() {
        // todo
        return "";
    }

    long getNoteId(@NonNull final Intent intent) {
        long retval = -1;
        if (intent.getData() != null && (Intent.ACTION_EDIT.equals(intent.getAction()) || Intent
                .ACTION_VIEW.equals(intent.getAction()))) {
            if (intent.getData().getPath().startsWith(TaskList.URI.getPath())) {
                // Find it in the extras. See DashClock extension for an example
                retval = intent.getLongExtra(Task.TABLE_NAME, -1);
            } else if ((intent.getData().getPath().startsWith(LegacyDBHelper.NotePad.Notes
                    .PATH_VISIBLE_NOTES) ||
                    intent.getData().getPath().startsWith(LegacyDBHelper.NotePad.Notes
                            .PATH_NOTES) ||
                    intent.getData().getPath().startsWith(Task.URI.getPath()))) {
                retval = Long.parseLong(intent.getData().getLastPathSegment());
            }
        }
        return retval;
    }
}
