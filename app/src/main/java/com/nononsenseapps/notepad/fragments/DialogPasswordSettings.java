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

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputLayout;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.prefs.PasswordPrefs;

/**
 * Fragment which handles password setting.
 */
public class DialogPasswordSettings extends DialogFragment implements DialogInterface
		.OnShowListener {

	// TODO here there may be something to copy into PasswordPrefs.java

	public static final String KEY_PASSWORD = "secretPassword";

	private EditText newPasswordText;
	private EditText confirmPasswordText;
	private EditText currentPasswordText;
	private TextInputLayout confirmPasswordLabel;
	private TextInputLayout currentPasswordLabel;

	public static void showDialog(final FragmentManager fm) {
		DialogPasswordSettings d = new DialogPasswordSettings();
		d.setArguments(Bundle.EMPTY);
		d.show(fm, "passwordsettings");
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final AlertDialog d = new AlertDialog.Builder(getActivity()).setTitle(R.string.password)
				.setView(-1).setNegativeButton(android.R.string
						.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Not used, see OnShow
						onOK(dialog);
					}
				}).create();

		d.setOnShowListener(this);

		return d;
	}

	@Override
	public void onStart() {
		super.onStart();
		Dialog dialog = getDialog();

		newPasswordText = (EditText) dialog.findViewById(R.id.tempPassword1);
		confirmPasswordText = (EditText) dialog.findViewById(R.id.tempPassword2);



		currentPasswordText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				currentPasswordLabel.setError(null);
			}
		});

		confirmPasswordText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				confirmPasswordLabel.setError(null);
			}
		});
	}

	@Override
	public void onShow(DialogInterface dialog) {
		final AlertDialog d = (AlertDialog) dialog;
		Button ok = d.getButton(AlertDialog.BUTTON_POSITIVE);
		// Override the actual on click listener to prevent dismissal of dialog
		ok.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onOK(d);
			}
		});
	}

	private void onOK(DialogInterface dialog) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences
				(getContext());
		if (verifyCurrentPassword(sharedPreferences) && verifyNewPassword()) {
			savePassword(sharedPreferences);
			dialog.dismiss();
		}
	}

	private void savePassword(SharedPreferences sharedPreferences) {
		if (newPasswordIsEmpty()) {
			sharedPreferences.edit().remove(KEY_PASSWORD).apply();
		} else {
			sharedPreferences.edit().putString(KEY_PASSWORD, newPasswordText.getText().toString()
			).apply();
		}
	}

	private boolean newPasswordIsEmpty() {
		return newPasswordText.getText().toString().isEmpty();
	}

	private boolean verifyNewPassword() {
		if (newPasswordText.getText().toString().equals(confirmPasswordText.getText().toString())) {
			return true;
		}

		confirmPasswordLabel.setError(getString(R.string.passwords_dont_match));
		return false;
	}

	private boolean verifyCurrentPassword(SharedPreferences sharedPreferences) {
		final String currentPassword = sharedPreferences.getString(KEY_PASSWORD, "");
		if (currentPassword.equals(currentPasswordText.getText().toString())) {
			return true;
		}

		currentPasswordLabel.setError(getString(R.string.password_incorrect));
		return false;

	}
}