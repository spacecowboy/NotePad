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

package com.nononsenseapps.utils.filepicker;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;

import com.nononsenseapps.utils.R;

public abstract class AbstractFilePickerActivity<T> extends Activity implements
        AbstractFilePickerFragment.OnFilePickedListener<T> {
    public static final String RESULT_PATH = "result_path";
    private static final String TAG = "filepicker_fragment";

    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setupFauxDialog();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_filepicker);

        String startPath = null;
        Intent intent = getIntent();
        if (intent != null) {
            startPath = intent.getStringExtra(RESULT_PATH);
        }

        AbstractFilePickerFragment<T> fragment = getFragment(startPath);
        getFragmentManager().beginTransaction().replace(R.id.fragment,
                fragment, TAG).commit();
    }

    private void setupFauxDialog() {
        // Check if this should be a dialog
        TypedValue tv = new TypedValue();
        if (!getTheme().resolveAttribute(R.attr.isDialog, tv, true) || tv.data == 0) {
            return;
        }

        // Should be a dialog; set up the window parameters.
        DisplayMetrics dm = getResources().getDisplayMetrics();

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = getResources().getDimensionPixelSize(R.dimen.configure_dialog_width);
        params.height = Math.min(
                getResources().getDimensionPixelSize(R.dimen.configure_dialog_max_height),
                dm.heightPixels * 3 / 4);
        params.alpha = 1.0f;
        params.dimAmount = 0.5f;
        getWindow().setAttributes(params);
    }

    protected abstract AbstractFilePickerFragment<T> getFragment(final String
                                                                         startPath);

    @Override
    public void onFilePicked(final T file) {
        Intent i = new Intent();
        i.putExtra(RESULT_PATH, file.toString());
        setResult(Activity.RESULT_OK, i);
        finish();
    }

    @Override
    public void onCancelled() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }
}
