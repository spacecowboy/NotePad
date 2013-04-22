package com.nononsenseapps.notepad.fragments;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.TaskList;

public class DialogDeleteList extends DialogConfirmBase {
	static final String ID = "id";
	static final String TAG = "deletelistok";

	public static void showDialog(final FragmentManager fm, final long listId, final DialogConfirmedListener listener) {
		DialogDeleteList d = new DialogDeleteList();
		d.setListener(listener);
		Bundle args = new Bundle();
		args.putLong(ID, listId);
		d.setArguments(args);
		d.show(fm, TAG);
	}

	@Override
	int getTitle() {
		return R.string.delete_question;
	}

	@Override
	int getMessage() {
		return R.string.delete_list_message;
	}

	@Override
	void onOKClick() {
		if (getArguments().getLong(ID, -1) > 0) {
			if (0 < getActivity().getContentResolver()
					.delete(TaskList.getUri(getArguments().getLong(ID, -1)),
							null, null)) {
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
