package com.nononsenseapps.notepad.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;

/**
 * Simple confirm dialog fragment.
 */
public abstract class DialogConfirmBase extends DialogFragment {

	public interface DialogConfirmedListener {
		void onConfirm();
	}

	DialogConfirmedListener listener;

	public void setListener(final DialogConfirmedListener l) {
		listener = l;
	}

	public DialogConfirmBase() {
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new AlertDialog.Builder(getActivity())
				.setTitle(getTitle())
				.setMessage(getMessage())
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						onOKClick();
					}
				})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
												int which) {
								dialog.dismiss();
							}
						}).create();
	}

	public abstract int getTitle();

	public abstract int getMessage();

	public abstract void onOKClick();
}
