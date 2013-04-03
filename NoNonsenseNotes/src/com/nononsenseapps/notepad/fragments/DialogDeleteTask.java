package com.nononsenseapps.notepad.fragments;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;

public class DialogDeleteTask extends DialogConfirmBase {
	static final String ID = "id";
	static final String TAG = "deletetaskok";

	public static void showDialog(final FragmentManager fm, final long taskId,
			final DialogConfirmedListener listener) {
		DialogDeleteTask d = new DialogDeleteTask();
		d.setListener(listener);
		Bundle args = new Bundle();
		args.putLong(ID, taskId);
		d.setArguments(args);
		d.show(fm, TAG);
	}

	@Override
	int getTitle() {
		return R.string.archive_item_title;
	}

	@Override
	int getMessage() {
		return R.string.archive_item_message;
	}

	@Override
	void onOKClick() {
		if (getArguments().getLong(ID, -1) > 0) {
			if (0 < getActivity().getContentResolver().delete(
					Task.getUri(getArguments().getLong(ID, -1)), null, null)) {
				Toast.makeText(getActivity(), R.string.archive_item_toast,
						Toast.LENGTH_SHORT).show();
			}
		}
		if (listener != null) {
			listener.onConfirm();
		}
		getDialog().dismiss();
	}

}
