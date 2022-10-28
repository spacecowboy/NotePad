package com.nononsenseapps.notepad.fragments;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;

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
		String where = Task.Columns.COMPLETED + " IS NOT NULL";
		String[] whereArgs = null;


		switch ((int) getArguments().getLong(ID, -1)) {
			case TaskListFragment.LIST_ID_ALL:
				// Nothing to do. Take all completed
				break;
			case TaskListFragment.LIST_ID_OVERDUE:
				where += TaskListFragment.andWhereOverdue();
				break;
			case TaskListFragment.LIST_ID_TODAY:
				where += TaskListFragment.andWhereToday();
				break;
			case TaskListFragment.LIST_ID_WEEK:
				where += TaskListFragment.andWhereWeek();
				break;
			default:
				where += " AND " + Task.Columns.DBLIST + " IS ?";
				whereArgs = new String[] { Long
						.toString(getArguments().getLong(ID, -1)) };
				break;
		}

		if (0 < getActivity().getContentResolver().delete(
				Task.URI,
				where,
				whereArgs)) {
			Toast.makeText(getActivity(), R.string.deleted,
					Toast.LENGTH_SHORT).show();
		}
		if (listener != null) {
			listener.onConfirm();
		}
		getDialog().dismiss();
	}

}
