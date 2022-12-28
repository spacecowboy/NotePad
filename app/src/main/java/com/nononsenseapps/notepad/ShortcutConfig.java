package com.nononsenseapps.notepad;

import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.databinding.ActivityShortcutConfigBinding;


/**
 * Shows a window to configure the app's smaller widget, letting the user choose which note list
 * will be opened
 */
public class ShortcutConfig extends AppCompatActivity {

	/**
	 * for {@link R.layout#activity_shortcut_config}
	 */
	private ActivityShortcutConfigBinding mBinding;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBinding = ActivityShortcutConfigBinding.inflate(getLayoutInflater());
		setContentView(mBinding.getRoot());
		mBinding.ok.setOnClickListener(x -> onOK());

		// Default result is fail
		setResult(RESULT_CANCELED);
		setListEntries(mBinding.listSpinner);
	}

	void onOK() {
		final Intent shortcutIntent = new Intent();
		// Set icon
		final ShortcutIconResource iconResource = Intent.ShortcutIconResource
				.fromContext(ShortcutConfig.this, R.drawable.app_icon);
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				iconResource);
		String shortcutTitle = "";
		final Intent intent = new Intent();
		if (mBinding.createNoteSwitch.isChecked()) {
			shortcutTitle = ShortcutConfig.this
					.getString(R.string.title_create);

			intent.setClass(ShortcutConfig.this, ActivityMain.class)
					.setData(Task.URI)
					.setAction(Intent.ACTION_INSERT)
					.putExtra(Task.Columns.DBLIST, mBinding.listSpinner.getSelectedItemId());
		} else {
			final Cursor c = (Cursor) mBinding.listSpinner.getSelectedItem();

			if (c != null && !c.isClosed() && !c.isAfterLast()) {
				shortcutTitle = c.getString(1);
			}
			shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, ""
					+ mBinding.listSpinner.getSelectedItem());

			intent.setClass(ShortcutConfig.this, ActivityMain.class)
					.setAction(Intent.ACTION_VIEW)
					.setData(TaskList.getUri(mBinding.listSpinner.getSelectedItemId()));
		}
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutTitle);

		setResult(RESULT_OK, shortcutIntent);

		// Destroy activity
		finish();
	}

	private void setListEntries(final Spinner listSpinner) {
		final SimpleCursorAdapter mSpinnerAdapter = new SimpleCursorAdapter(
				this, android.R.layout.simple_spinner_dropdown_item, null,
				new String[] { TaskList.Columns.TITLE },
				new int[] { android.R.id.text1 }, 0);

		listSpinner.setAdapter(mSpinnerAdapter);

		getLoaderManager().restartLoader(0, null, new LoaderCallbacks<Cursor>() {

			@Override
			public Loader<Cursor> onCreateLoader(int id, Bundle args) {
				return new CursorLoader(ShortcutConfig.this,
						TaskList.URI, new String[] {
						TaskList.Columns._ID,
						TaskList.Columns.TITLE }, null, null,
						TaskList.Columns.TITLE);
			}

			@Override
			public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
				mSpinnerAdapter.swapCursor(c);
			}

			@Override
			public void onLoaderReset(Loader<Cursor> arg0) {
				mSpinnerAdapter.swapCursor(null);
			}
		});
	}
}
