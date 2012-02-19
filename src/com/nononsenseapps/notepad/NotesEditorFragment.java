package com.nononsenseapps.notepad;

import java.util.Calendar;
import java.util.TimeZone;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.nononsenseapps.notepad.interfaces.DeleteActionListener;
import com.nononsenseapps.ui.TextPreviewPreference;

public class NotesEditorFragment extends Fragment implements TextWatcher,
		OnDateSetListener, OnClickListener,
		LoaderManager.LoaderCallbacks<Cursor> {

	// Two ways of expressing: "Mon, 16 Jan"
	// Time is not stable, will always claim it's Sunday
	// public final static String ANDROIDTIME_FORMAT = "%a, %e %b";
	public final static String DATEFORMAT_FORMAT = "E, d MMM";
	/*
	 * Creates a projection that returns the note ID and the note contents.
	 */
	public static final String[] PROJECTION = new String[] { NotePad.Notes._ID,
			NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_NOTE,
			NotePad.Notes.COLUMN_NAME_DUE_DATE,
			NotePad.Notes.COLUMN_NAME_DELETED };

	// A label for the saved state of the activity
	public static final String ORIGINAL_NOTE = "origContent";
	public static final String ORIGINAL_TITLE = "origTitle";
	public static final String ORIGINAL_DUE = "origDue";
	public static final String ORIGINAL_DUE_STATE = "origDueState";

	// Argument keys
	public static final String KEYID = "com.nononsenseapps.notepad.NoteId";
	public static final String LISTID = "com.nononsenseapps.notepad.ListId";

	// This Activity can be started by more than one action. Each action is
	// represented
	// as a "state" constant
	// public static final int STATE_EDIT = 0;
	// public static final int STATE_INSERT = 1;

	protected static final int DATE_DIALOG_ID = 999;
	private static final String TAG = "NotesEditorFragment";

	// These fields are strictly used for the date picker dialog. They should
	// not be used to get the notes due date!
	public int year;
	public int month;
	public int day;

	// This object is used to save the correct date in the database.
	private Time noteDueDate;
	private boolean dueDateSet = false;

	// Global mutable variables
	private Uri mUri;

	private EditText mText;
	public String mOriginalNote;
	public String mOriginalTitle;
	public String mOriginalDueDate;

	private boolean doSave = false;

	private long id = -1;
	private long listId = -1;

	private boolean timeToDie;

	private Object shareActionProvider = null; // Must be object otherwise HC
												// will crash

	private Activity activity;

	private EditText mTitle;

	private Button mDueDate;
	private boolean mOriginalDueState;
	private boolean opened = false;
	public boolean selfAction = false;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
	}

	/**
	 * Create a new instance of DetailsFragment, initialized to show the text at
	 * 'index'.
	 */
	public static NotesEditorFragment newInstance(long id, long listId) {
		NotesEditorFragment f = new NotesEditorFragment();

		// Supply index input as an argument.
		if (FragmentLayout.UI_DEBUG_PRINTS)
			Log.d("NotesEditorFragment", "Creating Fragment, args: " + id);
		Bundle args = new Bundle();
		args.putLong(KEYID, id);
		args.putLong(LISTID, listId);
		f.setArguments(args);

		return f;
	}

	public static Uri getUriFrom(long id) {
		return Uri.withAppendedPath(NotePad.Notes.CONTENT_URI,
				String.valueOf(id));
	}

	/**
	 * If we are supposed to open a new note, the arguments will contain
	 * "content".
	 * 
	 * @param savedInstanceState
	 * @param createNew
	 *            if True, will create new if the id is -1
	 */
	private void openNote(Bundle savedInstanceState) {
		if (FragmentLayout.UI_DEBUG_PRINTS)
			Log.d("NotesEditorFragment", "OpenNOTe: Id is " + id);
		selfAction = false;
		doSave = true;
		opened = true;
		if (id != -1) {
			// Existing note
			mUri = getUriFrom(id);
			if (FragmentLayout.UI_DEBUG_PRINTS)
				Log.d("NotesEditorFragment", "Editing existing note, uri = "
						+ mUri.toString());
		}
		// Just in case mUri failed some how...
		if (id == -1 || mUri == null) {
			// Invalid id
			// Closes the activity.
			activity.finish();
			return;
		} else {

			// Load original data if exists
			if (savedInstanceState != null) {
				mOriginalNote = savedInstanceState.getString(ORIGINAL_NOTE);
				mOriginalDueDate = savedInstanceState.getString(ORIGINAL_DUE);
				mOriginalTitle = savedInstanceState.getString(ORIGINAL_TITLE);
				mOriginalDueState = savedInstanceState
						.getBoolean(ORIGINAL_DUE_STATE);
			} else {
				// If null, will set to current values in showNote
				mOriginalNote = null;
				mOriginalDueDate = null;
				mOriginalTitle = null;
				mOriginalDueState = false;
			}

			// Prepare the loader. Either re-connect with an existing one,
			// or start a new one. Will open the note
			id = getIdFromUri(mUri);
			getLoaderManager().restartLoader(0, null, this);
		}
	}

	private static long getIdFromUri(Uri uri) {
		String newId = uri.getPathSegments().get(
				NotePad.Notes.NOTE_ID_PATH_POSITION);
		return Long.parseLong(newId);
	}

	private boolean hasNoteChanged() {
		if (mOriginalNote == null || mOriginalTitle == null
				|| mOriginalDueDate == null) {
			return false;
		} else {
			return !(mText.getText().toString().equals(mOriginalNote)
					&& mTitle.getText().toString().equals(mOriginalTitle)
					&& dueDateSet == mOriginalDueState && (!dueDateSet || (dueDateSet && noteDueDate
					.format3339(false).equals(mOriginalDueDate))));
		}
	}

	/**
	 * Replaces the current note contents with the text and title provided as
	 * arguments.
	 * 
	 * Only works if mUri is not null
	 * 
	 * @param text
	 *            The new note contents to use.
	 */
	private final void updateNote(String title, String text, String due) {
		if (mUri != null) {

			// Only updates if the text is different from original content
			// Only compare dates if there actually is a previous date to
			// compare
			if (!hasNoteChanged()) {
				if (FragmentLayout.UI_DEBUG_PRINTS)
					Log.d("NotesEditorFragment",
							"Note has not changed, won't save");
				// Do Nothing in this case.
			} else {
				if (FragmentLayout.UI_DEBUG_PRINTS)
					Log.d("NotesEditorFragment", "Updating note");
				if (FragmentLayout.UI_DEBUG_PRINTS)
					Log.d("NotesEditorFragment",
							"" + text.equals(mOriginalNote));
				if (FragmentLayout.UI_DEBUG_PRINTS)
					Log.d("NotesEditorFragment",
							"" + title.equals(mOriginalTitle));
				if (FragmentLayout.UI_DEBUG_PRINTS)
					Log.d("NotesEditorFragment", "" + due + " "
							+ mOriginalDueDate);
				if (FragmentLayout.UI_DEBUG_PRINTS)
					Log.d("NotesEditorFragment", ""
							+ (dueDateSet == mOriginalDueState));

				// Sets up a map to contain values to be updated in the
				// provider.
				ContentValues values = new ContentValues();

				// Add new modification time
				values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
						System.currentTimeMillis());

				// If no title was provided as an argument, create one from the
				// note
				// text.
				if (title.isEmpty()) {
					title = makeTitle(text);
				}

				// If the action is to insert a new note, this creates an
				// initial
				// title
				// for it.
				// if (mState == STATE_INSERT) {
				// In the values map, sets the value of the title
				// values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
				// } else if (title != null) {
				// In the values map, sets the value of the title
				values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
				// }

				// This puts the desired notes text into the map.
				values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);

				// Add list
				values.put(NotePad.Notes.COLUMN_NAME_LIST, listId);

				// Put the due-date in
				if (dueDateSet) {
					values.put(NotePad.Notes.COLUMN_NAME_DUE_DATE, due);
				} else {
					values.put(NotePad.Notes.COLUMN_NAME_DUE_DATE, "");
				}

				/*
				 * Updates the provider with the new values in the map. The
				 * ListView is updated automatically. The provider sets this up
				 * by setting the notification URI for query Cursor objects to
				 * the incoming URI. The content resolver is thus automatically
				 * notified when the Cursor for the URI changes, and the UI is
				 * updated. Note: This is being done on the UI thread. It will
				 * block the thread until the update completes. In a sample app,
				 * going against a simple provider based on a local database,
				 * the block will be momentary, but in a real app you should use
				 * android.content.AsyncQueryHandler or android.os.AsyncTask.
				 */
				if (FragmentLayout.UI_DEBUG_PRINTS)
					Log.d("NotesEditorFragment", "URI: " + mUri);
				if (FragmentLayout.UI_DEBUG_PRINTS)
					Log.d("NotesEditorFragment", "values: " + values.toString());
				activity.getContentResolver().update(mUri, // The URI for the
															// record to
						// update.
						values, // The map of column names and new values to
								// apply
								// to
								// them.
						null, // No selection criteria are used, so no where
								// columns
								// are
								// necessary.
						null // No where columns are used, so no where arguments
								// are
								// necessary.
						);
			}
		}
	}

	private String makeTitle(String text) {
		String title = null;
		// Get the note's length
		int length = text.length();

		// Sets the title by getting a substring of the text that is 31
		// characters long
		// or the number of characters in the note plus one, whichever is
		// smaller.
		title = text.substring(0, Math.min(30, length));
		int firstNewLine = title.indexOf("\n");

		// Only use the first line of text as title
		if (firstNewLine > 0) {
			title = title.substring(0, firstNewLine);
		} else if (firstNewLine == 0) {
			title = "First line empty...";
		}

		// If the resulting length is more than 30 characters, chops off any
		// trailing spaces
		if (title.length() > 30) {
			int lastSpace = title.lastIndexOf(' ');
			if (lastSpace > 0) {
				title = title.substring(0, lastSpace);
			}
		}
		return title;
	}

	/**
	 * This helper method cancels the work done on a note. It deletes the note
	 * if it was newly created, or reverts to the original text of the note i
	 */
	private final void cancelNote() {
		if (mOriginalTitle != null && mTitle != null)
			mTitle.setText(mOriginalTitle);
		if (mOriginalNote != null && mText != null)
			mText.setText(mOriginalNote);
		if (mOriginalDueDate != null && mDueDate != null) {
			setDueDate(mOriginalDueDate);
		}
	}

	private void copyText(String text) {
		ClipboardManager clipboard = (ClipboardManager) activity
				.getSystemService(Context.CLIPBOARD_SERVICE);
		// ICS style
		clipboard.setPrimaryClip(ClipData.newPlainText("Note", text));
		// Gingerbread style.
		// clipboard.setText(text);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (FragmentLayout.UI_DEBUG_PRINTS)
			Log.d("NotesEditorFragment", "onCreate");
		super.onCreate(savedInstanceState);
		// To get the call back to add items to the menu
		setHasOptionsMenu(true);

		if (FragmentLayout.UI_DEBUG_PRINTS)
			Log.d("NotesEditorFragment",
					"Value: "
							+ Boolean.toString(getArguments() != null
									&& getArguments().containsKey(KEYID)
									&& getArguments().containsKey(LISTID)));

		if (getArguments() != null && getArguments().containsKey(KEYID)
				&& getArguments().containsKey(LISTID)) {
			if (FragmentLayout.UI_DEBUG_PRINTS)
				Log.d(TAG, "Should never happen right");
			id = getArguments().getLong(KEYID);
			listId = getArguments().getLong(LISTID);
		} else {
			if (FragmentLayout.UI_DEBUG_PRINTS)
				Log.d(TAG, "onCreate, no valid values in arguments");
			id = -1;
			listId = -1;
		}

		noteDueDate = new Time(Time.getCurrentTimezone());

		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		year = c.get(Calendar.YEAR);
		month = c.get(Calendar.MONTH);
		day = c.get(Calendar.DAY_OF_MONTH);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (FragmentLayout.UI_DEBUG_PRINTS)
			Log.d("NotesEditorFragment", "onCreateView");

		int layout = R.layout.editor_layout;

		// Gets a handle to the EditText in the the layout.
		ScrollView theView = (ScrollView) inflater.inflate(layout, container,
				false);
		// This is to prevent the view from setting focus (and bringing up the
		// keyboard)
		theView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
		theView.setFocusable(true);
		theView.setFocusableInTouchMode(true);
		theView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				v.requestFocusFromTouch();
				return false;
			}
		});

		// Main note edit text
		mText = (EditText) theView.findViewById(R.id.noteBox);
		mText.addTextChangedListener(this);
		mText.setEnabled(false);
		// Title edit text
		mTitle = (EditText) theView.findViewById(R.id.titleBox);
		mTitle.addTextChangedListener(this);
		mTitle.setEnabled(false);
		// dueDate button
		mDueDate = (Button) theView.findViewById(R.id.dueDateBox);
		mDueDate.setOnClickListener(this);
		mDueDate.setEnabled(false);

		ImageButton cancelButton = (ImageButton) theView
				.findViewById(R.id.dueCancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				clearDueDate();
			}
		});

		return theView;
	}

	private void clearDueDate() {
		if (mDueDate != null) {
			mDueDate.setText(getText(R.string.editor_due_date_hint));
			// set year, month, day variables to today
			Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			year = c.get(Calendar.YEAR);
			month = c.get(Calendar.MONTH);
			day = c.get(Calendar.DAY_OF_MONTH);
		}
		dueDateSet = false;

		// Remember to update share intent
		setActionShareIntent();
	}

	private void setFontSettings() {
		if (mText != null && mTitle != null) {
			// set characteristics from settings
			float size = PreferenceManager
					.getDefaultSharedPreferences(activity).getInt(
							NotesPreferenceFragment.KEY_FONT_SIZE_EDITOR,
							getResources().getInteger(
									R.integer.default_editor_font_size));
			Typeface tf = TextPreviewPreference.getTypeface(PreferenceManager
					.getDefaultSharedPreferences(activity).getString(
							NotesPreferenceFragment.KEY_FONT_TYPE_EDITOR,
							NotesPreferenceFragment.SANS));

			mText.setTextSize(size);
			mText.setTypeface(tf);
			mTitle.setTextSize(size);
			mTitle.setTypeface(tf);
			mTitle.setPaintFlags(mTitle.getPaintFlags()
					| Paint.FAKE_BOLD_TEXT_FLAG);
		}
	}

	@Override
	public void onActivityCreated(Bundle saves) {
		super.onActivityCreated(saves);
		// if Time to Die, do absolutely nothing since this fragment will go bye
		// bye
		if (FragmentLayout.UI_DEBUG_PRINTS)
			Log.d(TAG, "onActivityCreated");
		if (timeToDie) {
			if (FragmentLayout.UI_DEBUG_PRINTS)
				Log.d("NotesEditorFragment",
						"onActivityCreated, but it is time to die so doing nothing...");
		} else if (saves != null && saves.containsKey(KEYID)
				&& saves.containsKey(LISTID)) {
			if (FragmentLayout.UI_DEBUG_PRINTS)
				Log.d(TAG, "onActivityCrated, saves are not null!");
			openNote(saves);
		} else if (listId > -1) {
			if (FragmentLayout.UI_DEBUG_PRINTS)
				Log.d(TAG,
						"onActivityCreated, got valid list id atleast. Displaying note...");
			// in activity
			// displayNote(id, listId);
			openNote(null);
		} else {
			if (FragmentLayout.UI_DEBUG_PRINTS)
				Log.d(TAG,
						"onActivityCreated, could not find valid values. Maybe I should die now?");
		}
	}

	/**
	 * Should only be called by notes editor activity!!!!!!
	 * 
	 * @param id
	 * @param listId
	 */
	public void setValues(long id, long listId) {
		this.id = id;
		this.listId = listId;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (FragmentLayout.UI_DEBUG_PRINTS)
			Log.d("NotesEditorFragment", "onCreateOptions");
		if (timeToDie) {
			if (FragmentLayout.UI_DEBUG_PRINTS)
				Log.d("NotesEditorFragment",
						"onCreateOptions, but it is time to die so doing nothing...");
		} else {
			// Inflate menu from XML resource
			// if (FragmentLayout.lightTheme)
			// inflater.inflate(R.menu.editor_options_menu_light, menu);
			// else
			inflater.inflate(R.menu.editor_options_menu, menu);

			if (FragmentLayout.AT_LEAST_ICS) {
				// Set delete listener to this
				MenuItem actionItem = menu.findItem(R.id.action_delete);

				DeleteActionProvider actionProvider = (DeleteActionProvider) actionItem
						.getActionProvider();

				// Make sure containing activity implements listner interface
				actionProvider
						.setDeleteActionListener((DeleteActionListener) activity);

				// Set default intent on ShareProvider and set shareListener to
				// this so
				// we can update with current note
				// Set file with share history to the provider and set the share
				// intent.
				MenuItem shareItem = menu
						.findItem(R.id.editor_share_action_provider_action_bar);

				ShareActionProvider shareProvider = (ShareActionProvider) shareItem
						.getActionProvider();
				shareProvider
						.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);

				this.shareActionProvider = shareProvider;

				// Note that you can set/change the intent any time,
				// say when the user has selected an image.
				setActionShareIntent();
			}

			// Append to the
			// menu items for any other activities that can do stuff with it
			// as well. This does a query on the system for any activities
			// that
			// implement the ALTERNATIVE_ACTION for our data, adding a menu
			// item
			// for each one that is found.
			Intent intent = new Intent(null, mUri);
			intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
			menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
					new ComponentName(activity, NotesEditorFragment.class),
					null, intent, 0, null);
		}
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		if (timeToDie) {
			if (FragmentLayout.UI_DEBUG_PRINTS)
				Log.d("NotesEditorFragment",
						"onPrepareOptionsMenu, but it is time to die so doing nothing...");
		} else {
			// Check if note has changed and enable/disable the revert option
			if (!hasNoteChanged()) {
				menu.findItem(R.id.menu_revert).setVisible(false);
			} else {
				menu.findItem(R.id.menu_revert).setVisible(true);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		switch (item.getItemId()) {
		case R.id.menu_revert:
			cancelNote();
			Toast.makeText(activity, "Reverted changes", Toast.LENGTH_SHORT)
					.show();
			break;
		case R.id.menu_copy:
			copyText(makeShareText());
			Toast.makeText(activity, "Note placed in clipboard",
					Toast.LENGTH_SHORT).show();
			break;
		case R.id.menu_share:
			shareNote();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private String makeShareText() {
		String note = "";

		if (mTitle != null)
			note = mTitle.getText().toString() + "\n";

		if (dueDateSet && noteDueDate != null) {

			Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			c.setTimeInMillis(noteDueDate.toMillis(false));

			dueDateSet = true;

			note = note + getText(R.string.editor_due_date_hint) + ": "
					+ DateFormat.format(DATEFORMAT_FORMAT, c) + "\n";
		}

		if (mText != null)
			note = note + "\n" + mText.getText().toString();

		return note;
	}

	private void shareNote() {
		Intent share = new Intent(Intent.ACTION_SEND);
		share.setType("text/plain");
		share.putExtra(Intent.EXTRA_TEXT, makeShareText());
		if (mTitle != null)
			share.putExtra(Intent.EXTRA_SUBJECT, mTitle.getText());
		share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		startActivity(Intent.createChooser(share, "Share note"));
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!timeToDie) {
			if (FragmentLayout.UI_DEBUG_PRINTS)
				Log.d("NotesEditorFragment", "onResume");

			// Settings might have been changed
			setFontSettings();
			// We don't want to do this the first time
			if (opened) {
				if (FragmentLayout.UI_DEBUG_PRINTS)
					Log.d("InsertError", "onResume Editor");
				openNote(null);
			}
		}
	}

	/**
	 * Will save existing note (except if empty new note)
	 * 
	 * @param id
	 * @param mCurListId
	 */
	public void displayNote(long id, long listid) {
		if (FragmentLayout.UI_DEBUG_PRINTS)
			Log.d("NotesEditorFragment", "Display note: " + id);

		// TODO make the fragment be a progress bar like the list here until it
		// is opened
		// Not sure if it is necessary with the fixes to selfAction.
		// If i'm going to do it, use a ViewSwitcher
		saveNote();
		doSave = true;
		selfAction = false;
		this.id = id;
		this.listId = listid;
		if (FragmentLayout.UI_DEBUG_PRINTS)
			Log.d("insertError", "displayNote");
		openNote(null);
	}

	private void showNote(Cursor mCursor) {
		boolean lockdown = false;
		if (mCursor != null && !mCursor.isClosed() && !mCursor.isAfterLast()) {
			/*
			 * Moves to the first record. Always call moveToFirst() before
			 * accessing data in a Cursor for the first time. The semantics of
			 * using a Cursor are that when it is created, its internal index is
			 * pointing to a "place" immediately before the first record.
			 */
			mCursor.moveToFirst();

			/*
			 * onResume() may have been called after the Activity lost focus
			 * (was paused). The user was either editing or creating a note when
			 * the Activity paused. The Activity should re-display the text that
			 * had been retrieved previously, but it should not move the cursor.
			 * This helps the user to continue editing or entering.
			 */

			// CHeck if note was deleted
			if (1 == mCursor.getInt(mCursor
					.getColumnIndex(NotePad.Notes.COLUMN_NAME_DELETED))) {
				lockdown = true;
			} else {

				int colTitleIndex = mCursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
				String title = mCursor.getString(colTitleIndex);
				mTitle.setText(title);
				mTitle.setEnabled(true);

				// Gets the note text from the Cursor and puts it in the
				// TextView,
				// but doesn't change
				// the text cursor's position. setTextKeepState
				int colNoteIndex = mCursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
				String note = mCursor.getString(colNoteIndex);
				mText.setText(note);
				mText.setEnabled(true);
				// Sets cursor at the end
				// mText.setSelection(note.length());

				int colDueIndex = mCursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_DUE_DATE);
				String due = mCursor.getString(colDueIndex);

				setDueDate(due);
				mDueDate.setEnabled(true);

				// Stores the original note text, to allow the user to revert
				// changes.
				if (mOriginalNote == null) {
					mOriginalNote = note;
				}
				if (mOriginalDueDate == null) {
					mOriginalDueDate = due;
					mOriginalDueState = dueDateSet;
				}
				if (mOriginalTitle == null) {
					mOriginalTitle = title;
				}

				// Request focus, will not open keyboard
				if (FragmentLayout.LANDSCAPE_MODE) {
					activity.findViewById(R.id.noteBox).requestFocus();
				}
			}

			/*
			 * Something is wrong. The Cursor should always contain data. Report
			 * an error in the note.
			 */
		} else {
			// Can also be set inside the loading if deleted flag is true
			lockdown = true;
		}

		if (lockdown) {
			// This WILL happen if the note is deleted by the server.
			mUri = null;
			// id = -1;
			doSave = false;
			selfAction = false;
			if (mText != null) {
				mText.setText(getText(R.string.error_message));
				mText.setEnabled(false);
			}
			if (mTitle != null) {
				mTitle.setText(getText(R.string.error_title));
				mTitle.setEnabled(false);
			}
			if (mDueDate != null) {
				mDueDate.setText(getText(R.string.editor_due_date_hint));
				mDueDate.setEnabled(false);
			}
		}

		// Regardless, set the share intent
		setActionShareIntent();
	}

	private void setDueDate(String due) {
		// update year, month, day here from database instead if they exist
		if (due == null || due.isEmpty()) {
			clearDueDate();
		} else {
			try {
				noteDueDate.parse3339(due);
				dueDateSet = true;
				Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				c.setTimeInMillis(noteDueDate.toMillis(false));

				mDueDate.setText(DateFormat.format(DATEFORMAT_FORMAT, c));
			} catch (TimeFormatException e) {
				noteDueDate.setToNow();
				dueDateSet = false;
			}
		}
	}

	/**
	 * This method is called when an Activity loses focus during its normal
	 * operation, and is then later on killed. The Activity has a chance to save
	 * its state so that the system can restore it.
	 * 
	 * Notice that this method isn't a normal part of the Activity lifecycle. It
	 * won't be called if the user simply navigates away from the Activity.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Save away the original text, so we still have it if the activity
		// needs to be killed while paused.
		outState.putString(ORIGINAL_NOTE, mOriginalNote);
		outState.putString(ORIGINAL_DUE, noteDueDate.format3339(false));
		outState.putBoolean(ORIGINAL_DUE_STATE, mOriginalDueState);
		outState.putString(ORIGINAL_TITLE, mOriginalTitle);
	}

	/**
	 * This method is called when the Activity loses focus.
	 * 
	 * For Activity objects that edit information, onPause() may be the one
	 * place where changes are saved. The Android application model is
	 * predicated on the idea that "save" and "exit" aren't required actions.
	 * When users navigate away from an Activity, they shouldn't have to go back
	 * to it to complete their work. The act of going away should save
	 * everything and leave the Activity in a state where Android can destroy it
	 * if necessary.
	 * 
	 * If the user hasn't done anything, then this deletes or clears out the
	 * note, otherwise it writes the user's work to the provider.
	 */
	@Override
	public void onPause() {
		super.onPause();

		saveNote();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Just make sure we are unregistered
	}

	private void saveNote() {
		selfAction = true; // Don't try to reload the note
		if (doSave && mText != null && mTitle != null) {
			if (FragmentLayout.UI_DEBUG_PRINTS)
				Log.d("NotesEditorFragment", "Saving/Deleting Note");

			// Get the current note text.
			String text = mText.getText().toString();

			// Get title text
			String title = mTitle.getText().toString();

			/*
			 * If the Activity is in the midst of finishing and there is no text
			 * in the current note, returns a result of CANCELED to the caller,
			 * and deletes the note. This is done even if the note was being
			 * edited, the assumption being that the user wanted to "clear out"
			 * (delete) the note.
			 */
			// if (isFinishing() && (length == 0)) {
			updateNote(title, text, noteDueDate.format3339(false));
		}
	}

	/**
	 * Prevents this Fragment from saving the note on exit Intended for deletion
	 * of notes. Also disables monitoring of this note.
	 */
	public void setSelfAction() {
		if (activity != null && !activity.isFinishing()) {
			selfAction = true;
		}
	}

	private void setActionShareIntent() {
		if (FragmentLayout.AT_LEAST_ICS && shareActionProvider != null) {
			Intent share = new Intent(Intent.ACTION_SEND);
			share.setType("text/plain");
			share.putExtra(Intent.EXTRA_TEXT, makeShareText());
			if (mTitle != null)
				share.putExtra(Intent.EXTRA_SUBJECT, mTitle.getText());
			share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

			((ShareActionProvider) shareActionProvider).setShareIntent(share);
		}
	}

	@Override
	public void afterTextChanged(Editable s) {
		setActionShareIntent();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// Don't care
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// Don't care
	}

	/**
	 * Once the user picks a date from the dialog, the date is received here and
	 * fields are updated, button text changed.
	 */
	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		// Update variables and set the button text
		this.year = year;
		this.month = monthOfYear;
		this.day = dayOfMonth;

		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		c.set(year, monthOfYear, dayOfMonth);

		noteDueDate.set(dayOfMonth, monthOfYear, year);
		dueDateSet = true;

		final CharSequence timeToShow = DateFormat.format(DATEFORMAT_FORMAT, c);

		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (mDueDate != null) {
					mDueDate.setText(timeToShow);
				}
				// Remember to update share intent
				setActionShareIntent();
			}

		});
	}

	@Override
	public void onClick(View v) {

		// activity.showDialog(DATE_DIALOG_ID);
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		// Create and show the dialog.
		DatePickerDialogFragment newFragment = new DatePickerDialogFragment(
				this);
		newFragment.show(ft, "dialog");
	}

	public class DatePickerDialogFragment extends DialogFragment {
		private NotesEditorFragment mFragment;

		public DatePickerDialogFragment(NotesEditorFragment callback) {
			mFragment = callback;
		}

		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new DatePickerDialog(activity, mFragment, mFragment.year,
					mFragment.month, mFragment.day);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// This is called when a new Loader needs to be created.

		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.
		return new CursorLoader(activity, mUri, PROJECTION, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (FragmentLayout.UI_DEBUG_PRINTS)
			Log.d("notewa" + TAG, "onLoadFinished");
		if (!selfAction) {
			// This note has changed, re-open it.
			// As a special case, we will not re-open it if that is
			// equivalent to deleting all text.
			// This will happen if you create a new note, start typing and
			// then a background sync happens.
			// Simply reopening it will delete everything you did. So only
			// re-open if there was any content
			// to begin with.
			// Note the "NOT" in the beginning.
			boolean reload = true;
			if (FragmentLayout.UI_DEBUG_PRINTS)
				Log.d(TAG, "title field: " + mOriginalTitle);
			if (FragmentLayout.UI_DEBUG_PRINTS)
				Log.d(TAG, "text field: " + mOriginalNote);
			if (mOriginalTitle != null && mOriginalNote != null) {
				if (mOriginalNote.isEmpty() && mOriginalTitle.isEmpty()
						&& !mText.getText().toString().isEmpty()) {
					// In this case, don't reload.
					reload = false;
				}
			}
			if (reload) {
				showNote(data);
			}
		}
		// Only one event is allowed to be ignored
		selfAction = false;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	public long getCurrentNoteId() {
		return id;
	}
}
