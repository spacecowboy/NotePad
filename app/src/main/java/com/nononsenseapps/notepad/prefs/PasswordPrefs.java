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

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.fragments.DialogPassword.PasswordConfirmedListener;
import com.nononsenseapps.notepad.fragments.DialogPasswordV11_;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

public class PasswordPrefs extends Fragment {

	public static final String KEY_PASSWORD = "secretPassword";

	private Activity activity;
	private EditText password1;
	private EditText password2;

	// TODO copy from DialogPasswordSettings.java and delete that file

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

		layout.findViewById(R.id.applyPassword).setOnClickListener(v -> applyPassword());
		layout.findViewById(R.id.clearPassword).setOnClickListener(v -> clearPassword());

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
				Toast.makeText(activity, getText(R.string.password_set),
						Toast.LENGTH_SHORT).show();
			} else {
				// confirm with existing password first
				showPasswordDialog(password1.getText().toString());
			}

		} else {
			// Show a toast so the user knows he did something wrong
			Animation shake = AnimationUtils.loadAnimation(activity,
					R.anim.shake);
			password2.startAnimation(shake);
			Toast.makeText(activity, getText(R.string.passwords_dont_match),
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
			Toast.makeText(activity, getText(R.string.password_cleared),
					Toast.LENGTH_SHORT).show();
		} else {
			// confirm with existing password first
			showPasswordDialog("");
		}
	}

	private void showPasswordDialog(final String newPassword) {
		final DialogPasswordV11_ pd = new DialogPasswordV11_();
		pd.setListener(() -> {
			PreferenceManager
					.getDefaultSharedPreferences(getActivity()).edit()
					.putString(PasswordPrefs.KEY_PASSWORD, newPassword)
					.commit();
			Toast.makeText(
					getActivity(),
					("".equals(newPassword)) ? getText(R.string.password_cleared)
							: getText(R.string.password_set),
					Toast.LENGTH_SHORT).show();
		});
		pd.show(getFragmentManager(), "pw-verify");
	}
}