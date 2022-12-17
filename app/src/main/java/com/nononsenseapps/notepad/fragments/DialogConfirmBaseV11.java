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

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.nononsenseapps.helpers.ThemeHelper;

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

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		int style = ThemeHelper.getPickerDialogTheme(getActivity());
		return new AlertDialog.Builder(getActivity(), style)
				.setTitle(getTitle())
				.setMessage(getMessage())
				.setPositiveButton(android.R.string.ok, (dialog, which) -> onOKClick())
				.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
				.create();
	}

	public abstract int getTitle();

	public abstract CharSequence getMessage();

	public abstract void onOKClick();
}
