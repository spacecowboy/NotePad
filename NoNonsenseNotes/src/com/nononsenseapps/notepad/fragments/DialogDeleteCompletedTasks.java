package com.nononsenseapps.notepad.fragments;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;

public class DialogDeleteCompletedTasks extends DialogDeleteTask {
	public static void showDialog(final FragmentManager fm, final long listId,
			final DialogConfirmedListener listener) {
		DialogDeleteCompletedTasks d = new DialogDeleteCompletedTasks();
		d.setListener(listener);
		Bundle args = new Bundle();
		args.putLong(ID, listId);
		d.setArguments(args);
		d.show(fm, TAG);
	}

	@Override
	public int getMessage() {
		return R.string.delete_items_message;
	}

	@Override
	public void onOKClick() {
		if (getArguments().getLong(ID, -1) > 0) {
			if (0 < getActivity().getContentResolver().delete(
					Task.URI,
					Task.Columns.DBLIST + " IS ? AND " + Task.Columns.COMPLETED
							+ " IS NOT NULL",
					new String[] { Long
							.toString(getArguments().getLong(ID, -1)) })) {
				Toast.makeText(getActivity(), R.string.deleted,
						Toast.LENGTH_SHORT).show();
			}
		}
		if (listener != null) {
			listener.onConfirm();
		}
		getDialog().dismiss();
	}

}
