/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.notepad.fragments;

import android.os.Bundle;

import androidx.fragment.app.FragmentManager;

import android.widget.Toast;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;

/**
 * Popup to confirm the user's choice to delete all completed tasks
 */
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
		return R.string.delete_completed_tasks_question;
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

		int rowsDeleted = getActivity().getContentResolver().delete(Task.URI, where, whereArgs);
		if (0 < rowsDeleted) {
			Toast.makeText(getActivity(), R.string.deleted, Toast.LENGTH_SHORT).show();
		}
		if (listener != null) {
			listener.onConfirm();
		}
		getDialog().dismiss();
	}

}
