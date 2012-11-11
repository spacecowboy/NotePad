/*
 * Copyright (C) 2012 Jonas Kalderstam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.notepad;

import java.util.Calendar;
import java.util.TimeZone;

import android.annotation.TargetApi;
import android.app.Activity;
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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.Time;
import com.nononsenseapps.helpers.Log;
import android.util.TimeFormatException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.ShareActionProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nononsenseapps.helpers.UpdateNotifier;
import com.nononsenseapps.notepad.PasswordDialog.ActionResult;
import com.nononsenseapps.notepad.prefs.MainPrefs;
import com.nononsenseapps.notepad.prefs.PasswordPrefs;
import com.nononsenseapps.ui.TextPreviewPreference;

public class NotesEditorFragment extends Fragment implements TextWatcher,
		OnDateSetListener, OnClickListener,
		LoaderManager.LoaderCallbacks<Cursor> {

	// Two ways of expressing: "Mon, 16 Jan"
	// Time is not stable, will always claim it's Sunday
	// public final static String ANDROIDTIME_FORMAT = "%a, %e %b";
	public final static String DATEFORMAT_FORMAT_SHORT = "E, d MMM";
	public final static String DATEFORMAT_FORMAT_LONG = "EEEE, d MMMM";
	/*
	 * Creates a projection that returns the note ID and the note contents.
	 */
	public static final String[] PROJECTION = new String[] { NotePad.Notes._ID,
			NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_NOTE,
			NotePad.Notes.COLUMN_NAME_DUE_DATE,
			NotePad.Notes.COLUMN_NAME_DELETED, NotePad.Notes.COLUMN_NAME_LIST,
			NotePad.Notes.COLUMN_NAME_GTASKS_STATUS };

	// A label for the saved state of the activity
	public static final String ORIGINAL_NOTE = "origContent";
	public static final String ORIGINAL_TITLE = "origTitle";
	public static final String ORIGINAL_DUE = "origDue";
	public static final String ORIGINAL_DUE_STATE = "origDueState";
	public static final String ORIGINAL_COMPLETE = "origComplete";
	public static final String ORIGINAL_LIST = "origList";

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
	protected static final int SHOW_NOTE = 10;
	protected static final int LOCK_NOTE = 11;
	protected static final int UNLOCK_NOTE = 12;

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
	// public boolean mOriginalComplete;
	public long mOriginalListId;

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

	private Spinner listSpinner;
	private SimpleCursorAdapter listAdapter;
	private static int LOADER_NOTE_ID = 0;
	private static int LOADER_LISTS_ID = 1;

	private NoteAttributes noteAttrs;
	private Handler mHandler = new Handler();
	private boolean mComplete;
	private boolean mCompleteChanged = false;
	private CheckBox conComplete;
	private CheckBox expComplete;
	private TextView details;
	private TextView lockedText;
	private View lockButton;
	private View unlockButton;
	private View detailsContracted;
	private View detailsExpanded;
	private ImageButton cancelButton;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
	}

	/**
	 * Create a new instance of DetailsFragment, initialized to show the text at
	 * 'index'.
	 */
	public static NotesEditorFragment newInstance(long id) {
		NotesEditorFragment f = new NotesEditorFragment();

		// Supply index input as an argument.
		Bundle args = new Bundle();
		args.putLong(KEYID, id);
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
		// Just make sure we are attached
		if (activity != null && !activity.isFinishing()) {
			if (id != -1) {
				// Existing note
				mUri = getUriFrom(id);
			}
			// Just in case mUri failed some how...
			if (id == -1 || mUri == null) {
				// Invalid id
				// Closes the activity.
				if (activity != null)
					activity.finish();
				return;
			} else {

				// Load original data if exists
				if (savedInstanceState != null) {
					mOriginalNote = savedInstanceState.getString(ORIGINAL_NOTE);
					mOriginalDueDate = savedInstanceState
							.getString(ORIGINAL_DUE);
					mOriginalTitle = savedInstanceState
							.getString(ORIGINAL_TITLE);
					mOriginalDueState = savedInstanceState
							.getBoolean(ORIGINAL_DUE_STATE);
					mOriginalListId = savedInstanceState.getLong(ORIGINAL_LIST);
					
				} else {
					mOriginalNote = "";
					mOriginalDueDate = "";
					mOriginalTitle = "";
					mOriginalDueState = false;
					mOriginalListId = -1;
				}

				// Prepare the loader. Either re-connect with an existing one,
				// or start a new one. Will open the note
				id = getIdFromUri(mUri);
				getLoaderManager().restartLoader(LOADER_NOTE_ID, null, this);
				// Populate the list also
				getLoaderManager().restartLoader(LOADER_LISTS_ID, null, this);
				// Do not start sub task loader until we have list

			}
		}
	}

	public static long getIdFromUri(Uri uri) {
		String newId = uri.getPathSegments().get(
				NotePad.Notes.NOTE_ID_PATH_POSITION);
		return Long.parseLong(newId);
	}

	private boolean hasNoteChanged() {
		// Null check, can happen at first start up
		if (noteAttrs == null || mTitle == null || mText == null) {
			return false;
		}

		boolean title, note, completed, date, listC = false;
		// Get the current note text.
		String text = noteAttrs.getFullNote(mText.getText().toString());

		title = !mTitle.getText().toString().equals(mOriginalTitle);
		note = !text.equals(mOriginalNote);
		completed = mCompleteChanged;
		date = dueDateSet != mOriginalDueState
				|| (dueDateSet && !noteDueDate.format3339(false).equals(
						mOriginalDueDate));
		listC = this.listId != mOriginalListId && mOriginalListId > -1 && this.listId > -1;

		Log.d("posredux", "has note changed");
		Log.d("posredux", "title " + title);
		Log.d("posredux", "note " + note);
		Log.d("posredux", "completed " + completed);
		Log.d("posredux", "date " + date);
		Log.d("posredux", "listC " + listC + " " + this.listId + " " + mOriginalListId);

		return title || note || completed || date || listC;
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
	private final void updateNote(String title, String text, String due,
			boolean completed, long listId) {
		Log.d(TAG, "Note text: " + text);
		// Sets up a map to contain values to be updated in the
		// provider.
		ContentValues values = new ContentValues();

		// If no title was provided as an argument, create one from the
		// note
		// text.
		// if (title.isEmpty()) {
		// title = makeTitle(text);
		// }

		values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);

		// This puts the desired notes text into the map.
		Log.d(TAG, "Is text enabled? " + mText.isEnabled());
		if (mText.isEnabled()) {
			// the field will only be enabled if the password was
			// successfully inputted (if one exists. Otherwise, don't
			// change the text
			values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);
		}

		// Add list if changed
		if (listId != mOriginalListId && mOriginalListId > -1 && listId > -1)
			values.put(NotePad.Notes.COLUMN_NAME_LIST, listId);

		// Put the due-date in
		if (dueDateSet) {
			values.put(NotePad.Notes.COLUMN_NAME_DUE_DATE, due);
		} else {
			values.put(NotePad.Notes.COLUMN_NAME_DUE_DATE, "");
		}

		// Note completed?
		if (completed) {
			values.put(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS,
					getString(R.string.gtask_status_completed));
		} else {
			values.put(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS,
					getString(R.string.gtask_status_uncompleted));
		}

		/*
		 * Updates the provider with the new values in the map. The ListView is
		 * updated automatically. The provider sets this up by setting the
		 * notification URI for query Cursor objects to the incoming URI. The
		 * content resolver is thus automatically notified when the Cursor for
		 * the URI changes, and the UI is updated. Note: This is being done on
		 * the UI thread. It will block the thread until the update completes.
		 * In a sample app, going against a simple provider based on a local
		 * database, the block will be momentary, but in a real app you should
		 * use android.content.AsyncQueryHandler or android.os.AsyncTask.
		 */

		if (mUri != null) {
			activity.getContentResolver().update(mUri, values, null, null);
		} else {
			mUri = activity.getContentResolver().insert(
					NotePad.Notes.CONTENT_URI, values);
			id = getIdFromUri(mUri);
		}
		UpdateNotifier.notifyChangeNote(activity, mUri);
		// update changed variable
		mOriginalTitle = title;
		mOriginalNote = text;
		mOriginalDueDate = dueDateSet ? due : "";

		mCompleteChanged = false;
		mOriginalListId = listId;
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
			mText.setText(noteAttrs.parseNote(mOriginalNote));
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

		Log.d("NotesEditorFragment", "onCreate");
		super.onCreate(savedInstanceState);
		// To get the call back to add items to the menu
		setHasOptionsMenu(true);

		Log.d("NotesEditorFragment",
				"Value: "
						+ Boolean.toString(getArguments() != null
								&& getArguments().containsKey(KEYID)
								&& getArguments().containsKey(LISTID)));

		id = -1;
		if (getArguments() != null && getArguments().containsKey(KEYID)) {
			id = getArguments().getLong(KEYID);
		}
		if (savedInstanceState != null && savedInstanceState.containsKey(KEYID)) {
			id = savedInstanceState.getLong(KEYID);
		}

		noteDueDate = new Time(Time.getCurrentTimezone());

		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		year = c.get(Calendar.YEAR);
		month = c.get(Calendar.MONTH);
		day = c.get(Calendar.DAY_OF_MONTH);

		noteAttrs = new NoteAttributes(); // Just a precaution
		
		if (id == -1) {
			Log.d(TAG, "onCreate, no valid values in arguments");
			//throw new NullPointerException(
			//		"No note id was specified in the arguments!");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		Log.d("NotesEditorFragment", "oncreateview");

		int layout = R.layout.editor_layout;

		// Gets a handle to the EditText in the the layout.
		LinearLayout theView = (LinearLayout) inflater.inflate(layout,
				container, false);

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

		// Make some action happen when you click stuff
		final View detailsContracted = theView
				.findViewById(R.id.detailsContracted);
		final View detailsExpanded = theView.findViewById(R.id.detailsExpanded);
		if (detailsContracted != null) {
			detailsContracted.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (v != null)
						v.setVisibility(View.GONE);
					if (detailsExpanded != null)
						detailsExpanded.setVisibility(View.VISIBLE);
				}
			});
		}
		if (detailsExpanded != null) {
			detailsExpanded.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (v != null)
						v.setVisibility(View.GONE);
					if (detailsContracted != null)
						detailsContracted.setVisibility(View.VISIBLE);
				}
			});
		}
		this.detailsContracted = detailsContracted;
		this.detailsExpanded = detailsExpanded;

		final CheckBox conComplete = (CheckBox) theView
				.findViewById(R.id.conCompleted);
		final CheckBox expComplete = (CheckBox) theView
				.findViewById(R.id.expCompleted);
		if (conComplete != null) {
			conComplete
					.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							setCompleted(isChecked);
							if (expComplete != null)
								expComplete.setChecked(isChecked);
						}
					});
		}
		if (expComplete != null) {
			expComplete
					.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							setCompleted(isChecked);
							if (conComplete != null)
								conComplete.setChecked(isChecked);
						}
					});
		}
		this.conComplete = conComplete;
		this.expComplete = expComplete;

		details = (TextView) theView.findViewById(R.id.editorDetails);

		lockButton = theView.findViewById(R.id.lockButton);
		unlockButton = theView.findViewById(R.id.unlockButton);
		lockedText = (TextView) theView.findViewById(R.id.lockButtonText);

		OnClickListener l = new OnClickListener() {
			@Override
			public void onClick(View v) {
				showPasswordDialog(noteAttrs.locked ? UNLOCK_NOTE : LOCK_NOTE);
			}
		};
		lockButton.setOnClickListener(l);
		unlockButton.setOnClickListener(l);
		lockedText.setOnClickListener(l);

		// Main note edit text
		mText = (EditText) theView.findViewById(R.id.noteBox);
		mText.addTextChangedListener(this);
		// Title edit text
		mTitle = (EditText) theView.findViewById(R.id.titleBox);
		mTitle.addTextChangedListener(this);
		// dueDate button
		mDueDate = (Button) theView.findViewById(R.id.dueDateBox);
		mDueDate.setOnClickListener(this);
		// List spinner
		listSpinner = (Spinner) theView.findViewById(R.id.noteListSpinner);
		listAdapter = new SimpleCursorAdapter(getActivity(),
				android.R.layout.simple_list_item_1, null,
				new String[] { NotePad.Lists.COLUMN_NAME_TITLE },
				new int[] { android.R.id.text1 });
		listSpinner.setAdapter(listAdapter);
		listSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
					long id) {
				moveToList(id);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// Can this happen?
			}
		});
		// Subtask spinner
		/*
		subtaskSpinner = (Spinner) theView.findViewById(R.id.subtaskSpinner);
		subtaskAdapter = new ExtrasCursorAdapter(getActivity(),
				android.R.layout.simple_list_item_1, null,
				new String[] { Notes.COLUMN_NAME_TITLE },
				new int[] { android.R.id.text1 }, new int[] { TOPNOTE },
				new int[] { R.string.empty_string });
		subtaskSpinner.setAdapter(subtaskAdapter);
		subtaskSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
					long id) {
				makeSubtaskOf(id);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// Can this happen?
			}
		});

		// Subtask clear button
		ImageButton clearSubTask = (ImageButton) theView
				.findViewById(R.id.subtaskRemoveButton);
		clearSubTask.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				makeSubtaskOf(null);
				subtaskSpinner
						.setSelection(getPosOfId(subtaskAdapter, TOPNOTE));
			}
		});
		*/

		cancelButton = (ImageButton) theView
				.findViewById(R.id.dueCancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				clearDueDate();
			}
		});

		return theView;
	}

	protected void moveToList(long newListId) {
		if (listId != newListId && newListId > -1 && listId > -1) {
			listId = newListId;
			saveNote();
		}
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

		if (details != null)
			details.setText(R.string.editor_details);

		// Remember to update share intent
		if (activity != null && !activity.isFinishing())
			setActionShareIntent();
	}

	private void setFontSettings() {
		if (mText != null && mTitle != null) {
			// set characteristics from settings
			float size = PreferenceManager
					.getDefaultSharedPreferences(activity).getInt(
							MainPrefs.KEY_FONT_SIZE_EDITOR,
							getResources().getInteger(
									R.integer.default_editor_font_size));
			Typeface tf = TextPreviewPreference.getTypeface(PreferenceManager
					.getDefaultSharedPreferences(activity).getString(
							MainPrefs.KEY_FONT_TYPE_EDITOR, MainPrefs.SANS));

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

		Log.d(TAG, "onActivityCreated");
		if (timeToDie) {

			Log.d("NotesEditorFragment",
					"onActivityCreated, but it is time to die so doing nothing...");
		} else if (id > -1) {
			Log.d(TAG,
					"onActivityCreated, got valid id atleast. Displaying note...");
			openNote(saves);
		} else {
			Log.d(TAG,
					"onActivityCreated, could not find valid values. Maybe I should die now?");
			//throw new NullPointerException("No valid id was given!");
		}
	}

	/**
	 * Should only be called by notes editor activity!!!!!!
	 * 
	 * @param id
	 * @param listId
	 */
	public void setValues(long id) {
		this.id = id;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

		Log.d("NotesEditorFragment", "onCreateOptions");
		if (timeToDie) {

			Log.d("NotesEditorFragment",
					"onCreateOptions, but it is time to die so doing nothing...");
		} else {
			// Inflate menu from XML resource
			// if (FragmentLayout.lightTheme)
			// inflater.inflate(R.menu.editor_options_menu_light, menu);
			// else
			inflater.inflate(R.menu.editor_options_menu, menu);

			/*
			 * COmmented out until the bug in the platform is fixed if
			 * (getResources() .getBoolean(R.bool.atLeastIceCreamSandwich)) { //
			 * Set default intent on ShareProvider and set shareListener to //
			 * this so // we can update with current note // Set file with share
			 * history to the provider and set the share // intent. MenuItem
			 * shareItem = menu
			 * .findItem(R.id.editor_share_action_provider_action_bar);
			 * 
			 * ShareActionProvider shareProvider = (ShareActionProvider)
			 * shareItem .getActionProvider(); shareProvider
			 * .setShareHistoryFileName
			 * (ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
			 * 
			 * this.shareActionProvider = shareProvider;
			 * 
			 * // Note that you can set/change the intent any time, // say when
			 * the user has selected an image. setActionShareIntent(); }
			 */

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

	private void setCompleted(boolean val) {
		if (activity != null) {
			mComplete = val;
			mCompleteChanged = true;
			saveNote();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		switch (item.getItemId()) {
		case R.id.menu_revert:
			cancelNote();
			Toast.makeText(activity, getString(R.string.reverted),
					Toast.LENGTH_SHORT).show();
			break;
		case R.id.menu_copy:
			copyText(makeShareText());
			Toast.makeText(activity, getString(R.string.notecopied),
					Toast.LENGTH_SHORT).show();
			break;
		case R.id.menu_share:
			shareNote();
			break;
		case R.id.menu_sync:
			// Save note!
			saveNote();
			break;
		case R.id.menu_lock:
			// Lock note
			showPasswordDialog(LOCK_NOTE);
			break;
		case R.id.menu_unlock:
			// Unlock note
			showPasswordDialog(UNLOCK_NOTE);
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
					+ DateFormat.format(DATEFORMAT_FORMAT_SHORT, c) + "\n";
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
			// Settings might have been changed
			setFontSettings();
			// We don't want to do this the first time
			if (opened) {
				openNote(null);
			}
		}
	}

	private void showNote(Cursor mCursor) {
		if (detailsContracted != null) {
			detailsContracted.setVisibility(View.VISIBLE);
		}
		if (detailsExpanded != null) {
			detailsExpanded.setVisibility(View.GONE);
		}
		if (mCursor != null && !mCursor.isClosed() && mCursor.moveToFirst()) {
			/*
			 * Moves to the first record. Always call moveToFirst() before
			 * accessing data in a Cursor for the first time. The semantics of
			 * using a Cursor are that when it is created, its internal index is
			 * pointing to a "place" immediately before the first record.
			 */

			/*
			 * onResume() may have been called after the Activity lost focus
			 * (was paused). The user was either editing or creating a note when
			 * the Activity paused. The Activity should re-display the text that
			 * had been retrieved previously, but it should not move the cursor.
			 * This helps the user to continue editing or entering.
			 */

			int colTitleIndex = mCursor
					.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
			String title = mCursor.getString(colTitleIndex);
			mTitle.setText(title);
			mTitle.setEnabled(true);
			if ("".equals(title)) {
				// Open keyboard on new notes so users can start typing directly
				// need small delay (100ms) for it to open consistently
				(new Handler()).postDelayed(new Runnable() {
					public void run() {
						if (mTitle != null) {
							mTitle.dispatchTouchEvent(MotionEvent.obtain(
									SystemClock.uptimeMillis(),
									SystemClock.uptimeMillis(),
									MotionEvent.ACTION_DOWN, 0, 0, 0));
							mTitle.dispatchTouchEvent(MotionEvent.obtain(
									SystemClock.uptimeMillis(),
									SystemClock.uptimeMillis(),
									MotionEvent.ACTION_UP, 0, 0, 0));
						}

					}
				}, 100);
			}

			mComplete = getText(R.string.gtask_status_completed)
					.toString()
					.equals(mCursor.getString(mCursor
							.getColumnIndex(NotePad.Notes.COLUMN_NAME_GTASKS_STATUS)));

			conComplete.setEnabled(true);
			expComplete.setEnabled(true);
			details.setEnabled(true);
			lockedText.setEnabled(true);
			lockButton.setEnabled(true);
			lockButton.setEnabled(true);
			listSpinner.setEnabled(true);
			cancelButton.setEnabled(true);

			// Will call expComplete
			conComplete.setChecked(mComplete);
			mCompleteChanged = false;

			// Set list ID
			listId = mCursor.getLong(mCursor
					.getColumnIndex(NotePad.Notes.COLUMN_NAME_LIST));

			if (listAdapter.getCount() > 0) {
				// It's loaded, select current
				listSpinner.setSelection(getPosOfId(listAdapter, listId));
			}

			// Load sub task spinner
			/*
			getLoaderManager().restartLoader(OTHER_NOTES_LOADER, null, this);

			if (subtaskAdapter.getCount() > 0) {
				// It's loaded, select current
				if (this.parent != null)
					subtaskSpinner.setSelection(getPosOfId(subtaskAdapter,
							this.parent));
				else
					subtaskSpinner.setSelection(getPosOfId(subtaskAdapter,
							TOPNOTE));
			}
			*/

			// Gets the note text from the Cursor and puts it in the
			// TextView,
			// but doesn't change
			// the text cursor's position. setTextKeepState
			int colNoteIndex = mCursor
					.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
			String note = mCursor.getString(colNoteIndex);

			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(activity);
			String currentPassword = settings.getString(
					PasswordPrefs.KEY_PASSWORD, "");

			noteAttrs = new NoteAttributes();
			noteAttrs.parseNote(note);

			if (lockButton != null) {
				lockButton.setVisibility(noteAttrs.locked ? View.INVISIBLE
						: View.VISIBLE);
			}
			if (unlockButton != null) {
				unlockButton.setVisibility(noteAttrs.locked ? View.VISIBLE
						: View.INVISIBLE);
			}
			if (lockedText != null) {
				lockedText
						.setText(noteAttrs.locked ? R.string.password_required
								: R.string.unlocked);
			}
			// Clear content if this is called in onResume
			mText.setText("");
			mText.setEnabled(false);
			// Don't care about locks if no password is set
			if (noteAttrs.locked && !"".equals(currentPassword)) {
				// Need password confirmation
				// This is called in onLoadFinished and you are not allowed
				// to
				// make fragment transactions there. Hence, we bypass that
				// by
				// opening it through the handler instead.
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						showPasswordDialog(SHOW_NOTE);
					}
				});
			} else {
				mText.setText(noteAttrs.getNoteText());
				mText.setEnabled(true);
			}
			// Sets cursor at the end
			// mText.setSelection(note.length());

			int colDueIndex = mCursor
					.getColumnIndex(NotePad.Notes.COLUMN_NAME_DUE_DATE);
			String due = mCursor.getString(colDueIndex);

			setDueDate(due);
			mDueDate.setEnabled(true);

			// Stores the original note text, to allow the user to revert
			// changes.
			mOriginalNote = note; // Save original text here
			mOriginalDueDate = due;
			mOriginalDueState = dueDateSet;
			mOriginalTitle = title;

			mOriginalListId = listId;

			// Some things might have changed
			getActivity().invalidateOptionsMenu();
			
			doSave = true;
			opened = true;
		} else {
			// You have tried to open something that doesn't exist
			clearNoSave();
		}

		// Regardless, set the share intent
		setActionShareIntent();
	}

	/**
	 * Clears and disables fields. Does NOT save.
	 */
	public void clearNoSave() {
		mUri = null;
		id = -1;
		doSave = false;
		opened = false;
		if (mText != null) {
			mText.setText("");
			mText.setEnabled(false);
		}
		if (mTitle != null) {
			mTitle.setText("");
			mTitle.setEnabled(false);
		}
		if (mDueDate != null) {
			mDueDate.setText(getText(R.string.editor_due_date_hint));
			mDueDate.setEnabled(false);
		}

		if (conComplete != null) {
			conComplete.setChecked(false);
			conComplete.setEnabled(false);
			mCompleteChanged = false;
		}
		if (expComplete != null) {
			expComplete.setChecked(false);
			expComplete.setEnabled(false);
			mCompleteChanged = false;
		}
		if (details != null) {
			details.setText(R.string.editor_details);
			details.setEnabled(false);
			details.setClickable(false);
		}
		if (lockedText != null) {
			lockedText.setText(R.string.unlocked);
			lockedText.setEnabled(false);
		}
		if (lockButton != null) {
			lockButton.setVisibility(View.VISIBLE);
			lockButton.setEnabled(false);
		}
		if (unlockButton != null) {
			unlockButton.setVisibility(View.INVISIBLE);
			lockButton.setEnabled(false);
		}
		if (detailsContracted != null) {
			detailsContracted.setVisibility(View.VISIBLE);
		}
		if (detailsExpanded != null) {
			detailsExpanded.setVisibility(View.GONE);
		}
		
		if (listSpinner != null)
			listSpinner.setEnabled(false);
		
		if (cancelButton != null)
			cancelButton.setEnabled(false);
	}

	private static int getPosOfId(ResourceCursorAdapter adapter, long id) {
		int length = adapter.getCount();
		int position;
		boolean found = false;
		for (position = 0; position < length; position++) {
			if (id == adapter.getItemId(position)) {
				found = true;
				break;
			}
		}
		if (!found) {
			// Happens both if list is empty
			// and if id is -1
			position = -1;
		}
		return position;
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

				mDueDate.setText(DateFormat.format(DATEFORMAT_FORMAT_LONG, c));
				Log.d("listproto", "Note has date: " + due);
				Log.d("listproto",
						"Note date shown as: "
								+ DateFormat.format(DATEFORMAT_FORMAT_LONG, c));
				if (details != null)
					details.setText(DateFormat.format(DATEFORMAT_FORMAT_SHORT,
							c));
			} catch (TimeFormatException e) {
				noteDueDate.setToNow();
				dueDateSet = false;
				if (details != null)
					details.setText(R.string.editor_details);
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
		outState.putLong(KEYID, this.id);
		outState.putLong(NotesEditorFragment.LISTID, this.listId);
		outState.putString(ORIGINAL_NOTE, mOriginalNote);
		outState.putString(ORIGINAL_DUE, noteDueDate.format3339(false));
		outState.putBoolean(ORIGINAL_DUE_STATE, mOriginalDueState);
		outState.putString(ORIGINAL_TITLE, mOriginalTitle);
		outState.putLong(ORIGINAL_LIST, mOriginalListId);
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

		if (doSave && opened)
			saveNote();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getLoaderManager().destroyLoader(LOADER_LISTS_ID);
		getLoaderManager().destroyLoader(LOADER_NOTE_ID);
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		this.activity = null;
	}

	private void saveNote() {
		if (doSave && opened && mText != null && mTitle != null) {

			// Get the current note text.
			String text = noteAttrs.getFullNote(mText.getText().toString());
			Log.d(TAG, "From editor: " + mText.getText());
			Log.d(TAG, "From attr: " + text);

			// Get title text
			String title = mTitle.getText().toString();

			// if (isFinishing() && (length == 0)) {
			if (hasNoteChanged())
				updateNote(title, text, noteDueDate.format3339(false),
						mComplete, listId);
		}
	}

	@TargetApi(14)
	private void setActionShareIntent() {
		if (getActivity() != null && !getActivity().isFinishing()) {
			if (getResources().getBoolean(R.bool.atLeast14)
					&& shareActionProvider != null) {
				Intent share = new Intent(Intent.ACTION_SEND);
				share.setType("text/plain");
				share.putExtra(Intent.EXTRA_TEXT, makeShareText());
				if (mTitle != null)
					share.putExtra(Intent.EXTRA_SUBJECT, mTitle.getText());
				share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

				((ShareActionProvider) shareActionProvider)
						.setShareIntent(share);
			}
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

		final CharSequence shortTimeToShow = DateFormat.format(
				DATEFORMAT_FORMAT_SHORT, c);
		final CharSequence longTimeToShow = DateFormat.format(
				DATEFORMAT_FORMAT_LONG, c);

		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (mDueDate != null) {
					mDueDate.setText(longTimeToShow);
				}
				if (details != null)
					details.setText(shortTimeToShow);
				// Remember to update share intent
				setActionShareIntent();
			}

		});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.dueDateBox:
		default:
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			Fragment prev = getFragmentManager().findFragmentByTag("dialog");
			if (prev != null) {
				ft.remove(prev);
			}
			ft.addToBackStack(null);

			// Create and show the dialog.
			DatePickerDialogFragment newFragment = new DatePickerDialogFragment();
			newFragment.setCallback(this);
			newFragment.show(ft, "dialog");
			break;
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// This is called when a new Loader needs to be created.

		// Now create and return a CursorLoader that will take care of
		// creating a Cursor for the data being displayed.
		if (LOADER_LISTS_ID == id)
			return new CursorLoader(activity,
					NotePad.Lists.CONTENT_VISIBLE_URI, new String[] {
							BaseColumns._ID, NotePad.Lists.COLUMN_NAME_TITLE },
					null, null, NotePad.Lists.SORT_ORDER);
		else
			return new CursorLoader(activity, mUri, PROJECTION, null, null,
					null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (LOADER_LISTS_ID == loader.getId()) {
			listAdapter.swapCursor(data);
			if (listId > -1)
				listSpinner.setSelection(getPosOfId(listAdapter, listId));
		} else {
			showNote(data);
			// We do NOT want updates on this URI
			getLoaderManager().destroyLoader(LOADER_NOTE_ID);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (LOADER_LISTS_ID == loader.getId())
			listAdapter.swapCursor(null);
	}

	public long getCurrentNoteId() {
		return id;
	}

	private void showPasswordDialog(int actionId) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("newpassdialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		// Create and show the dialog.
		PasswordDialog newFragment = new PasswordDialog();
		newFragment.setAction(actionId);
		newFragment.show(ft, "newpassdialog");
	}

	public void OnPasswordVerified(ActionResult result) {
		if (result != null && result.result && mText != null
				&& noteAttrs != null) {
			switch (result.actionId) {
			case LOCK_NOTE:
				noteAttrs.locked = true;
				Toast.makeText(activity, getString(R.string.locked),
						Toast.LENGTH_SHORT).show();
				if (lockButton != null) {
					lockButton.setVisibility(View.INVISIBLE);
				}
				if (unlockButton != null) {
					unlockButton.setVisibility(View.VISIBLE);
				}
				if (lockedText != null) {
					lockedText.setText(R.string.password_required);
				}
				break;
			case UNLOCK_NOTE:
				noteAttrs.locked = false;
				Toast.makeText(activity, getString(R.string.unlocked),
						Toast.LENGTH_SHORT).show();
				if (lockButton != null) {
					lockButton.setVisibility(View.VISIBLE);
				}
				if (unlockButton != null) {
					unlockButton.setVisibility(View.INVISIBLE);
				}
				if (lockedText != null) {
					lockedText.setText(R.string.unlocked);
				}
				// Fall through and show the note as well
			case SHOW_NOTE:
				mText.setText(noteAttrs.getNoteText());
				mText.setEnabled(true);
				break;
			default:
				// Dont do anything without proper key
				break;
			}
		}
		// Invalidate the menu so it redraws and hides/shows the icons if
		// applicable
		getActivity().invalidateOptionsMenu();
	}

}
