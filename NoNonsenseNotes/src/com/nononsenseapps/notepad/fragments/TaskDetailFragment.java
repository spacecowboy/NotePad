package com.nononsenseapps.notepad.fragments;

import java.security.InvalidParameterException;

import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A fragment representing a single Note detail screen. This fragment is either
 * contained in a {@link TaskListActivity} in two-pane mode (on tablets) or a
 * {@link TaskDetailActivity} on handsets.
 */
@EFragment(R.layout.fragment_task_detail)
public class TaskDetailFragment extends Fragment {
	
	@ViewById
	EditText taskText;
	
	@ViewById
	View detailsSection;
	
	@ViewById
	CheckBox taskCompleted;
	
	// Id of task to open
	public static final String ARG_ITEM_ID = "item_id";
	// If no id is given, a string can be accepted as initial state
	public static final String ARG_ITEM_CONTENT = "item_text";
	// A list id is necessary
	public static final String ARG_ITEM_LIST_ID = "item_list_id";

	// Dao version of the object this fragment represents
	private Task mTask;
	
	public static TaskDetailFragment_ getInstance(final long itemId) {
		Bundle arguments = new Bundle();
		arguments
				.putLong(TaskDetailFragment.ARG_ITEM_ID, itemId);
		TaskDetailFragment_ fragment = new TaskDetailFragment_();
		fragment.setArguments(arguments);
		return fragment;
	}

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public TaskDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().getLong(ARG_ITEM_ID, -1) > 0) {
			// Load data from database
		} else {
			if (!getArguments().containsKey(ARG_ITEM_LIST_ID)) {
				throw new InvalidParameterException("Must specify a list id to create a note in!");
			}
			
			mTask = new Task();
			mTask.dblist = getArguments().getLong(ARG_ITEM_LIST_ID);
			if (getArguments().containsKey(ARG_ITEM_CONTENT)) {
				// New note but start with the text given
				mTask.setText(getArguments().getString(ARG_ITEM_CONTENT));
			}
		}
	}

	
}
