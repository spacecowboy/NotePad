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

package com.nononsenseapps.notepad.ui.dashclock;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.nononsenseapps.notepad.R;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class TasksSettings extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setupFauxDialog();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dashclock_settings);

        setupActionBar();
    }

    protected void setupFauxDialog() {
        // Check if this should be a dialog
        TypedValue tv = new TypedValue();
        if (!getTheme().resolveAttribute(R.attr.isDialog, tv, true) ||
            tv.data == 0) {
            return;
        }

        // Should be a dialog; set up the window parameters.
        DisplayMetrics dm = getResources().getDisplayMetrics();

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = getResources()
                .getDimensionPixelSize(R.dimen.configure_dialog_width);
        params.height = Math.min(getResources()
                .getDimensionPixelSize(R.dimen.configure_dialog_max_height),
                dm.heightPixels * 3 / 4);
        params.alpha = 1.0f;
        params.dimAmount = 0.5f;
        getWindow().setAttributes(params);
    }

    protected void setupActionBar() {
        getActionBar().setIcon(R.drawable.ic_stat_notification_edit);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        //getActionBar().setTitle(R.string.dashclock_title_activity_tasks_settings);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
