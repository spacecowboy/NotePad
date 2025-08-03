package com.nononsenseapps.notepad.widget.shortcut;

import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.nononsenseapps.helpers.ActivityHelper;
import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.helpers.ThemeHelper;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.activities.main.ActivityMain_;
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
		// Must do this before super.onCreate
		ThemeHelper.setTheme(this);
		ActivityHelper.setSelectedLanguage(this);
		super.onCreate(savedInstanceState);
		mBinding = ActivityShortcutConfigBinding.inflate(getLayoutInflater());
		setContentView(mBinding.getRoot());
		mBinding.ok.setOnClickListener(x -> onOK());

		// Default result is fail
		setResult(RESULT_CANCELED);
		setListEntries(mBinding.listSpinner);
	}

	/**
	 * @return a {@link Bitmap} representing the given {@link Drawable}. Supports also
	 * {@link AdaptiveIconDrawable}, so you can build a bitmap of an adaptive icon
	 */
	@NonNull
	private static Bitmap getBitmapFromDrawable(@NonNull Drawable drawable) {
		final Bitmap bmp = Bitmap.createBitmap(
				drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight(),
				Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(bmp);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bmp;
	}

	void onOK() {
		// newer android versions have stricter limits on nested Intents
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

			ShortcutManager shortcutManager = this.getSystemService(ShortcutManager.class);

			String shortcutTitle = "";
			final Intent intent = new Intent();
			if (mBinding.createNoteSwitch.isChecked()) {

				String listName = null;
				final Cursor c = (Cursor) mBinding.listSpinner.getSelectedItem();
				if (c != null && !c.isClosed() && !c.isAfterLast()) {
					listName = c.getString(1);
				}
				if (listName == null) {
					NnnLogger.error(ShortcutConfig.class, "Unexpected null in listName in ShortcutConfig.java");
				}

				shortcutTitle = listName + " - " + ShortcutConfig.this.getString(R.string.title_create);

				intent.setClass(ShortcutConfig.this, ActivityMain_.class)
						.setData(Task.URI)
						.setAction(Intent.ACTION_INSERT)
						.putExtra(Task.Columns.DBLIST, mBinding.listSpinner.getSelectedItemId());
			} else {
				// this shortcut widget shows a list of notes
				final Cursor c = (Cursor) mBinding.listSpinner.getSelectedItem();

				if (c != null && !c.isClosed() && !c.isAfterLast()) {
					shortcutTitle = c.getString(1);
				}
				intent.setClass(ShortcutConfig.this, ActivityMain_.class)
						.setAction(Intent.ACTION_VIEW)
						.setData(TaskList.getUri(mBinding.listSpinner.getSelectedItemId()));
			}

			// widget IDs must be unique. We use unique titles for all widget combinations
			String shortcutId = shortcutTitle;
			ShortcutInfo shortcut = new ShortcutInfo.Builder(this, shortcutId)
					.setShortLabel(shortcutTitle)
					.setLongLabel(shortcutTitle)
					.setIcon(Icon.createWithResource(this, R.drawable.app_icon))
					.setIntent(intent)
					.build();

			shortcutManager.requestPinShortcut(shortcut, null);
			setResult(RESULT_OK);
			return;
		}

		// legacy code that still works for API 23 and 24
		final Intent shortcutIntent = new Intent();
		// Set the icon for the shortcut widget
		Drawable iconDrawable = AppCompatResources.getDrawable(this, R.drawable.app_icon);
		// we have to give it a bitmap, or else the icon does not appear in simple launcher
		// https://github.com/SimpleMobileTools/Simple-Launcher
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, getBitmapFromDrawable(iconDrawable));

		String shortcutTitle = "";
		final Intent intent = new Intent();
		if (mBinding.createNoteSwitch.isChecked()) {
			shortcutTitle = ShortcutConfig.this.getString(R.string.title_create);

			intent.setClass(ShortcutConfig.this, ActivityMain_.class)
					.setData(Task.URI)
					.setAction(Intent.ACTION_INSERT)
					.putExtra(Task.Columns.DBLIST, mBinding.listSpinner.getSelectedItemId());
		} else {
			// this shortcut widget shows a list of notes
			final Cursor c = (Cursor) mBinding.listSpinner.getSelectedItem();

			if (c != null && !c.isClosed() && !c.isAfterLast()) {
				shortcutTitle = c.getString(1);
			}
			shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
					"" + mBinding.listSpinner.getSelectedItem());

			intent.setClass(ShortcutConfig.this, ActivityMain_.class)
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

		LoaderManager
				.getInstance(this)
				.restartLoader(0, null, new LoaderManager.LoaderCallbacks<Cursor>() {

					@NonNull
					@Override
					public Loader<Cursor> onCreateLoader(int id, Bundle args) {
						return new CursorLoader(ShortcutConfig.this,
								TaskList.URI,
								new String[] { TaskList.Columns._ID, TaskList.Columns.TITLE },
								null,
								null,
								TaskList.Columns.TITLE);
					}

					@Override
					public void onLoadFinished(@NonNull Loader<Cursor> arg0, Cursor c) {
						mSpinnerAdapter.swapCursor(c);
					}

					@Override
					public void onLoaderReset(@NonNull Loader<Cursor> arg0) {
						mSpinnerAdapter.swapCursor(null);
					}
				});
	}
}
