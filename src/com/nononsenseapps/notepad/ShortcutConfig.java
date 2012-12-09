package com.nononsenseapps.notepad;

import com.nononsenseapps.notepad_donate.R;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.database.Cursor;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

public class ShortcutConfig extends Activity {

	private CheckBox createNoteCheckBox;
	private Spinner listSpinner;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.shortcut_config);

		// Default result is fail
		setResult(RESULT_CANCELED);

		createNoteCheckBox = (CheckBox) findViewById(R.id.create_note);
		listSpinner = (Spinner) findViewById(R.id.list);

		setListEntries(listSpinner);

		Button okButton = (Button) findViewById(R.id.ok);
		okButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent shortcutIntent = new Intent();
				// Set icon
				ShortcutIconResource iconResource = Intent.ShortcutIconResource
						.fromContext(ShortcutConfig.this, R.drawable.app_icon);
				shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
						iconResource);
				String shortcutTitle = "";
				Intent intent = new Intent();
				if (createNoteCheckBox.isChecked()) {
					shortcutTitle = ShortcutConfig.this
							.getString(R.string.title_create);

					intent.setClass(ShortcutConfig.this, RightActivity.class)
							.setData(NotePad.Notes.CONTENT_VISIBLE_URI)
							.setAction(Intent.ACTION_INSERT)
							.putExtra(NotePad.Notes.COLUMN_NAME_LIST,
									listSpinner.getSelectedItemId());
				} else {
					Cursor c = (Cursor) listSpinner.getSelectedItem();

					if (c != null && !c.isClosed() && !c.isAfterLast()) {
						shortcutTitle = c.getString(c
								.getColumnIndex(NotePad.Lists.COLUMN_NAME_TITLE));
					}
					shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, ""
							+ listSpinner.getSelectedItem());

					intent.setClass(ShortcutConfig.this, MainActivity.class)
							.setAction(Intent.ACTION_VIEW)
							.setData(
									Uri.withAppendedPath(
											NotePad.Lists.CONTENT_VISIBLE_ID_URI_BASE,
											Long.toString(listSpinner
													.getSelectedItemId())));
				}
				shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
				shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
						shortcutTitle);

				setResult(RESULT_OK, shortcutIntent);

				// Destroy activity
				finish();
			}
		});
	}

	private void setListEntries(Spinner listSpinner) {
		Cursor cursor = getContentResolver().query(
				NotePad.Lists.CONTENT_VISIBLE_URI,
				new String[] { NotePad.Lists._ID,
						NotePad.Lists.COLUMN_NAME_TITLE }, null, null,
				NotePad.Lists.SORT_ORDER);
		if (cursor == null) {
			return;
		}

		SimpleCursorAdapter mSpinnerAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, cursor,
				new String[] { NotePad.Lists.COLUMN_NAME_TITLE },
				new int[] { android.R.id.text1 }, 0);

		mSpinnerAdapter
				.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
		listSpinner.setAdapter(mSpinnerAdapter);
	}
}
