package com.nononsenseapps.notepad.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Simple confirm dialog fragment, extending from V11 fragment
 */
public abstract class DialogConfirmBaseV11 extends DialogFragment {

	public interface DialogConfirmedListener {
		void onConfirm();
	}

	DialogConfirmedListener listener;

	public void setListener(final DialogConfirmedListener l) {
		listener = l;
	}

	public DialogConfirmBaseV11() {
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

	public abstract CharSequence getMessage();

	public abstract void onOKClick();
}
