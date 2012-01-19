package com.nononsenseapps.notepad;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class NotesEditorFragment extends Fragment {
	/*
	 * Creates a projection that returns the note ID and the note contents.
	 */
	private static final String[] PROJECTION = new String[] {
			NotePad.Notes._ID, NotePad.Notes.COLUMN_NAME_TITLE,
			NotePad.Notes.COLUMN_NAME_NOTE };

	// A label for the saved state of the activity
	public static final String ORIGINAL_CONTENT = "origContent";

	// Argument keys
	public static final String KEYID = "noteid";
	public static final String KEYPOS = "index";

	// This Activity can be started by more than one action. Each action is
	// represented
	// as a "state" constant
	public static final int STATE_EDIT = 0;
	public static final int STATE_INSERT = 1;

	// Global mutable variables
	private int mState;
	private Uri mUri;
	private Cursor mCursor;
	private EditText mText;
	private String mOriginalContent;

	private boolean mDualPane;

	private boolean timeToDie;

	/**
	 * Create a new instance of DetailsFragment, initialized to show the text at
	 * 'index'.
	 */
	public static NotesEditorFragment newInstance(int index, long id) {
		NotesEditorFragment f = new NotesEditorFragment();

		// Supply index input as an argument.
		Log.d("NotesEditorFragment", "Creating Fragment, args: " + index + ", "
				+ id);
		Bundle args = new Bundle();
		args.putInt(KEYPOS, index);
		args.putLong(KEYID, id);
		f.setArguments(args);

		return f;
	}

	/**
	 * 
	 * @return -1 if new note
	 */
	public int getShownIndex() {
		return getArguments().getInt(KEYPOS, -1);
	}

	/**
	 * 
	 * @return -1 if new note
	 */
	public long getShownId() {
		return getArguments().getLong(KEYID, -1);
	}

	private Uri getUriFrom(long id) {
		return Uri.withAppendedPath(NotePad.Notes.CONTENT_URI,
				String.valueOf(id));
	}

	/**
	 * If we are supposed to open a new note, the arguments will contain
	 * "content".
	 * 
	 * @param savedInstanceState
	 */
	private void openNote(Bundle savedInstanceState) {
		Log.d("NotesEditorFragment", "Does argument have id? "
				+ getArguments().containsKey(KEYID));
		long id = getShownId();
		Log.d("NotesEditorFragment", "OpenNOTe: Id is " + id);
		if (id != -1) {
			// Existing note
			mState = STATE_EDIT;
			mUri = getUriFrom(id);
			Log.d("NotesEditorFragment",
					"Editing existing note, uri = " + mUri.toString());
		} else {
			// New note
			mState = STATE_INSERT;
			mUri = getActivity().getContentResolver().insert(
					NotePad.Notes.CONTENT_URI, null);
			Log.d("NotesEditorFragment",
					"Inserting new note, uri = " + mUri.toString());
			/*
			 * If the attempt to insert the new note fails, shuts down this
			 * Activity. The originating Activity receives back RESULT_CANCELED
			 * if it requested a result. Logs that the insert failed.
			 */
			if (mUri == null) {
				// Closes the activity.
				getActivity().finish();
				return;
			}
		}
		/*
		 * Using the URI passed in with the triggering Intent, gets the note or
		 * notes in the provider. Note: This is being done on the UI thread. It
		 * will block the thread until the query completes. In a sample app,
		 * going against a simple provider based on a local database, the block
		 * will be momentary, but in a real app you should use
		 * android.content.AsyncQueryHandler or android.os.AsyncTask.
		 */
		mCursor = getActivity().managedQuery(mUri, // The URI that gets multiple
													// notes from
				// the provider.
				PROJECTION, // A projection that returns the note ID and note
							// content for each note.
				null, // No "where" clause selection criteria.
				null, // No "where" clause selection values.
				null // Use the default sort order (modification date,
						// descending)
				);

		Log.d("NotesEditorFragment", "mDualPane: " + mDualPane);
		if (mState == STATE_INSERT && mDualPane) {
			long newId = getIdFromUri(mUri);
			Log.d("NotesEditorFragment", "Id to report is:" + newId);
			((NotesListFragment) getFragmentManager().findFragmentById(
					R.id.noteslistfragment)).selectNewId(newId);
		}

		// For a paste, initializes the data from clipboard.
		// (Must be done after mCursor is initialized.)
		if (mState == STATE_INSERT && getArguments().containsKey("content")) {
			// Does the paste
			updateNote(getArguments().getString("content"));
		}
		// Switches the state to EDIT so the title can be modified.
		mState = STATE_EDIT;

		if (savedInstanceState != null) {
			mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
		}

		// If the cursor is after last NOW, it means we were given an invalid
		// ID. (Happens you click on the title in list of "unnamed note" while
		// editing it already and list doesn't have access to right id in
		// editor. Fixed that though.
		if (mCursor.isAfterLast()) {
			Log.d("NotesSHOULDNEVERHAPPEN",
					"Cursor is after last, recreating a new note!");
			setShownId(-1);
			openNote(savedInstanceState);
		}
	}

	public long getIdFromUri(Uri uri) {
		String newId = uri.getPathSegments().get(
				NotePad.Notes.NOTE_ID_PATH_POSITION);
		return Long.parseLong(newId);
	}

	/**
	 * Replaces the current note contents with the text and title provided as
	 * arguments.
	 * 
	 * @param text
	 *            The new note contents to use.
	 */
	private final void updateNote(String text) {

		// Only updates if the text is different from original content
		if (text.equals(mOriginalContent)) {
			Log.d("NotesEditorFragment", "Updating (not) note");
			// Do Nothing in this case.
		} else {
			Log.d("NotesEditorFragment", "Updating note");
			String title = null;
			// Sets up a map to contain values to be updated in the provider.
			ContentValues values = new ContentValues();
			values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
					System.currentTimeMillis());

			// If no title was provided as an argument, create one from the note
			// text.
			title = makeTitle(text);

			// If the action is to insert a new note, this creates an initial
			// title
			// for it.
			if (mState == STATE_INSERT) {
				// In the values map, sets the value of the title
				values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
			} else if (title != null) {
				// In the values map, sets the value of the title
				values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
			}

			// This puts the desired notes text into the map.
			values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);

			/*
			 * Updates the provider with the new values in the map. The ListView
			 * is updated automatically. The provider sets this up by setting
			 * the notification URI for query Cursor objects to the incoming
			 * URI. The content resolver is thus automatically notified when the
			 * Cursor for the URI changes, and the UI is updated. Note: This is
			 * being done on the UI thread. It will block the thread until the
			 * update completes. In a sample app, going against a simple
			 * provider based on a local database, the block will be momentary,
			 * but in a real app you should use
			 * android.content.AsyncQueryHandler or android.os.AsyncTask.
			 */
			Log.d("NotesEditorFragment", "URI: " + mUri);
			Log.d("NotesEditorFragment", "values: " + values.toString());
			getActivity().getContentResolver().update(mUri, // The URI for the
															// record to
					// update.
					values, // The map of column names and new values to apply
							// to
							// them.
					null, // No selection criteria are used, so no where columns
							// are
							// necessary.
					null // No where columns are used, so no where arguments are
							// necessary.
					);
		}

		// Set the shown ID of the note
		long id = getIdFromUri(mUri);
		setShownId(id);
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
		if (mCursor != null) {
			if (mState == STATE_EDIT) {
				// Put the original note text back into the database
				mCursor.close();
				mCursor = null;
				ContentValues values = new ContentValues();
				values.put(NotePad.Notes.COLUMN_NAME_NOTE, mOriginalContent);
				getActivity().getContentResolver().update(mUri, values, null,
						null);
			} else if (mState == STATE_INSERT) {
				// We inserted an empty note, make sure to delete it
				deleteNote();
			}
		}
	}

	/**
	 * Take care of deleting a note. Simply deletes the entry.
	 */
	private final void deleteNote() {
		Log.d("NotesEditorFragment", "Deleting note");
		if (mCursor != null) {
			mCursor.close();
			mCursor = null;
			getActivity().getContentResolver().delete(mUri, null, null);
			mText.setText("");
		}
	}

	private void copyText(String text) {
		getActivity();
		ClipboardManager clipboard = (ClipboardManager) getActivity()
				.getSystemService(Context.CLIPBOARD_SERVICE);
		// ICS style
		clipboard.setPrimaryClip(ClipData.newPlainText("Note", text));
		// Gingerbread style.
		// clipboard.setText(text);
	}

	private void shareNote(String text) {
		Intent share = new Intent(Intent.ACTION_SEND);
		share.setType("text/plain");
		share.putExtra(Intent.EXTRA_TEXT, text);
		startActivity(Intent.createChooser(share, "Share note"));
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// To get the call back to add items to the menu
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (container == null) {
			// We have different layouts, and in one of them this
			// fragment's containing frame doesn't exist. The fragment
			// may still be created from its saved state, but there is
			// no reason to try to create its view hierarchy because it
			// won't be displayed. Note this is not needed -- we could
			// just run the code below, where we would create and return
			// the view hierarchy; it would just never be used.
			Log.d("NotesEditorFragment", "Should return null");
			timeToDie = true;
			return null;
		}

		int layout = R.layout.note_editor_dark;
		if (FragmentLayout.lightTheme) {
			layout = R.layout.note_editor_light;
		}

		// Gets a handle to the EditText in the the layout.
		mText = (EditText) inflater.inflate(layout, container, false);
		Log.d("NotesEditorFragment",
				"onCreateView openNote, should be the only call!");
		return mText;
	}

	@Override
	public void onActivityCreated(Bundle saves) {
		super.onActivityCreated(saves);
		// if Time to Die, do absolutely nothing since this fragment will go bye
		// bye
		if (timeToDie) {
			Log.d("NotesEditorFragment",
					"onActivityCreated, but it is time to die so doing nothing...");
		} else {
			// Need to know if this is shown in dualpane mode
			View editorFrame = getActivity().findViewById(R.id.editor);
			mDualPane = editorFrame != null
					&& editorFrame.getVisibility() == View.VISIBLE;

			openNote(saves);
			showTheNote();
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		Log.d("NotesEditorFragment", "onCreateOptions");
		if (timeToDie) {
			Log.d("NotesEditorFragment",
					"onCreateOptions, but it is time to die so doing nothing...");
		} else {
			// Inflate menu from XML resource
			if (FragmentLayout.lightTheme)
				inflater.inflate(R.menu.editor_options_menu_light, menu);
			else
				inflater.inflate(R.menu.editor_options_menu_dark, menu);

			// Only add extra menu items for a saved note
			if (mState == STATE_EDIT) {
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
						new ComponentName(getActivity(),
								NotesEditorFragment.class), null, intent, 0,
						null);
			}
		}
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		if (timeToDie) {
			Log.d("NotesEditorFragment",
					"onPrepareOptionsMenu, but it is time to die so doing nothing...");
		} else {
			// Check if note has changed and enable/disable the revert option
			if (mCursor != null) {
				int colNoteIndex = mCursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
				// if (colNoteIndex != -1)
				String savedNote = mCursor.getString(colNoteIndex);
				String currentNote = mText.getText().toString();
				if (savedNote.equals(currentNote)) {
					menu.findItem(R.id.menu_revert).setVisible(false);
				} else {
					menu.findItem(R.id.menu_revert).setVisible(true);
				}
			} else {
				menu.findItem(R.id.menu_revert).setVisible(false);
			}
		}
	}

	public void onSharedItemSelected(MenuItem item) {
		Log.d("NotesEditorFragment", "onSharedSelection");
		// Handle all of the possible menu actions.
		switch (item.getItemId()) {
		case R.id.menu_delete:
			deleteNote();
			break;
		case R.id.menu_add:
			updateNote(mText.getText().toString());
			break;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		String text;
		// Handle all of the possible menu actions.
		switch (item.getItemId()) {
		case R.id.menu_add:
		case R.id.menu_delete:
			// Handled in shared elsewhere
			// deleteNote();
			return false;
		case R.id.menu_revert:
			cancelNote();
			break;
		case R.id.menu_share:
			text = mText.getText().toString();
			shareNote(text);
			break;
		case R.id.menu_copy:
			text = mText.getText().toString();
			copyText(text);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d("NotesEditorFragment", "onResume");

		/*
		 * mCursor is initialized, since onCreate() always precedes onResume for
		 * any running process. This tests that it's not null, since it should
		 * always contain data.
		 */
		showTheNote();
	}

	private void showTheNote() {
		if (mCursor != null) {
			// Requery in case something changed while paused (such as the
			// title)
			mCursor.requery();

			/*
			 * Moves to the first record. Always call moveToFirst() before
			 * accessing data in a Cursor for the first time. The semantics of
			 * using a Cursor are that when it is created, its internal index is
			 * pointing to a "place" immediately before the first record.
			 */
			mCursor.moveToFirst();

			// Modifies the window title for the Activity according to the
			// current Activity state.
			if (mState == STATE_EDIT) {
				// Set the title of the Activity to include the note title
				int colTitleIndex = mCursor
						.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
				String title = mCursor.getString(colTitleIndex);
				Resources res = getResources();
				String text = String.format(res.getString(R.string.title_edit),
						title);
				getActivity().setTitle(text);
				// Sets the title to "create" for inserts
			} else if (mState == STATE_INSERT) {
				getActivity().setTitle(getText(R.string.title_create));
				// Set the shown ID of the note
				long id = getIdFromUri(mUri);
				setShownId(id);
			}

			/*
			 * onResume() may have been called after the Activity lost focus
			 * (was paused). The user was either editing or creating a note when
			 * the Activity paused. The Activity should re-display the text that
			 * had been retrieved previously, but it should not move the cursor.
			 * This helps the user to continue editing or entering.
			 */

			// Gets the note text from the Cursor and puts it in the TextView,
			// but doesn't change
			// the text cursor's position.
			int colNoteIndex = mCursor
					.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
			String note = mCursor.getString(colNoteIndex);
			mText.setTextKeepState(note);

			// Stores the original note text, to allow the user to revert
			// changes.
			if (mOriginalContent == null) {
				mOriginalContent = note;
			}

			/*
			 * Something is wrong. The Cursor should always contain data. Report
			 * an error in the note.
			 */
		} else {
			getActivity().setTitle(getText(R.string.error_title));
			if (mText != null)
				mText.setText(getText(R.string.error_message));
		}
		// Request focus if dual pane
		if (mDualPane) {
			getActivity().findViewById(R.id.editor).requestFocus();
		}
	}

	private void setShownId(long id) {
		getArguments().remove(KEYID);
		getArguments().putLong(KEYID, id);
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
		outState.putString(ORIGINAL_CONTENT, mOriginalContent);
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
		Log.d("NotesEditorFragment", "onPause");

		/*
		 * Tests to see that the query operation didn't fail (see onCreate()).
		 * The Cursor object will exist, even if no records were returned,
		 * unless the query failed because of some exception or error.
		 */
		if (mCursor != null) {

			// Get the current note text.
			String text = mText.getText().toString();

			/*
			 * If the Activity is in the midst of finishing and there is no text
			 * in the current note, returns a result of CANCELED to the caller,
			 * and deletes the note. This is done even if the note was being
			 * edited, the assumption being that the user wanted to "clear out"
			 * (delete) the note.
			 */
			// if (isFinishing() && (length == 0)) {
			if (text.isEmpty()) {
				getActivity().setResult(Activity.RESULT_CANCELED);
				deleteNote();

				/*
				 * Writes the edits to the provider. The note has been edited if
				 * an existing note was retrieved into the editor *or* if a new
				 * note was inserted. In the latter case, onCreate() inserted a
				 * new empty note into the provider, and it is this new note
				 * that is being edited.
				 */
			} else if (mState == STATE_EDIT) {
				// Creates a map to contain the new values for the columns
				updateNote(text);
			} else if (mState == STATE_INSERT) {
				updateNote(text);
				mState = STATE_EDIT;
			}
		}
	}
}
