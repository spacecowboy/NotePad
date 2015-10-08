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
		pd.setListener(new PasswordConfirmedListener() {
			@Override
			public void onPasswordConfirmed() {
				PreferenceManager
						.getDefaultSharedPreferences(getActivity()).edit()
						.putString(PasswordPrefs.KEY_PASSWORD, newPassword)
						.commit();
				Toast.makeText(
						getActivity(),
						("".equals(newPassword)) ? getText(R.string.password_cleared)
								: getText(R.string.password_set),
						Toast.LENGTH_SHORT).show();
			}
		});
		pd.show(getFragmentManager(), "pw-verify");
	}
}