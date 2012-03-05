/*
 * Copyright (C) 2012 Jonas Kalderstam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.notepad.prefs;

import com.nononsenseapps.notepad.FragmentLayout;
import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class PasswordPrefs extends PreferenceFragment {

	public static final String KEY_PASSWORD = "secretPassword";

	private Activity activity;
	private EditText password1;
	private EditText password2;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		// addPreferencesFromResource(R.xml.app_pref_password);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View layout = inflater.inflate(R.layout.app_pref_password_layout,
				container, false);

		// Set click listeners for the button here
		password1 = (EditText) layout.findViewById(R.id.tempPassword1);
		password2 = (EditText) layout.findViewById(R.id.tempPassword2);

		layout.findViewById(R.id.applyPassword).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						applyPassword();
					}
				});
		layout.findViewById(R.id.clearPassword).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						clearPassword();
					}
				});

		return layout;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private void applyPassword() {
		if (password1.getText().toString()
				.equals(password2.getText().toString())) {
			// They are the same
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(activity);
			String currentPassword = settings.getString(KEY_PASSWORD, "");

			if ("".equals(currentPassword)) {
				// Save the password directly
				settings.edit()
						.putString(KEY_PASSWORD, password1.getText().toString())
						.commit();
				// TODO resource
				Toast.makeText(activity, "Password set string",
						Toast.LENGTH_SHORT).show();
			} else {
				// confirm with existing password first
				showPasswordDialog(password1.getText().toString());
			}

		} else {
			// Show a toast so the user knows he did something wrong
			// TODO to string resource
			Animation shake = AnimationUtils.loadAnimation(activity,
					R.anim.shake);
			password2.startAnimation(shake);
			Toast.makeText(activity, "Passwords do not match string",
					Toast.LENGTH_SHORT).show();
		}
	}

	private void clearPassword() {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(activity);
		String currentPassword = settings.getString(KEY_PASSWORD, "");

		if ("".equals(currentPassword)) {
			// Save the password directly
			settings.edit()
					.putString(KEY_PASSWORD, "")
					.commit();
			// TODO resource
			Toast.makeText(activity, "Password cleared string",
					Toast.LENGTH_SHORT).show();
		} else {
			// confirm with existing password first
			showPasswordDialog("");
		}
	}

	private void showPasswordDialog(String newPassword) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("newpassdialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		// Create and show the dialog.
		Bundle args = new Bundle();
		args.putString(KEY_PASSWORD, newPassword);
		DialogFragment newFragment = new PasswordDialog();
		newFragment.setArguments(args);
		newFragment.show(ft, "newpassdialog");
	}

	public static class PasswordDialog extends DialogFragment implements
			OnClickListener {
		Bundle args;
		private Activity activity;
		private EditText passwordText;

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			this.activity = activity;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			Bundle args = getArguments();
			if (args != null) {
				this.args = args;
			} else {
				this.args = new Bundle();
			}
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.confirm_password_dialog,
					container, false);
			// TODO resource
			getDialog().setTitle("Password required string");

			passwordText = (EditText) v.findViewById(R.id.editTitle);

			Button yesButton = (Button) v.findViewById(R.id.dialog_yes);
			yesButton.setOnClickListener(this);

			Button noButton = (Button) v.findViewById(R.id.dialog_no);
			noButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dismiss();
				}
			});

			return v;
		}

		@Override
		public void onClick(View v) {
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(activity);
			String currentPassword = settings.getString(
					PasswordPrefs.KEY_PASSWORD, "");
			String enteredPassword = passwordText.getText().toString();
			String newPassword = args.getString(KEY_PASSWORD);

			if ("".equals(currentPassword)
					|| currentPassword.equals(enteredPassword)) {
				if (newPassword != null) {
					// Set new password
					settings.edit()
							.putString(PasswordPrefs.KEY_PASSWORD, newPassword)
							.commit();
					// TODO resource
					Toast.makeText(activity,
							("".equals(newPassword)) ? "Password cleared string" : "Password set string ",
							Toast.LENGTH_SHORT).show();
				} else {
					// Confirm that correct password was set to activity
				}
				dismiss();
			} else {
				Animation shake = AnimationUtils.loadAnimation(activity,
						R.anim.shake);
				passwordText.startAnimation(shake);
				Toast.makeText(activity, "Password incorrect string",
						Toast.LENGTH_SHORT).show();
			}
		}
	}
}