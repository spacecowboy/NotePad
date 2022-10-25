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

package com.nononsenseapps.notepad.fragments;

import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.fragments.DialogPassword.PasswordConfirmedListener;
import com.nononsenseapps.notepad.prefs.PasswordPrefs;

/**
 * Full copy of DialogPassword, but extending native fragment class instead.
 *
 */
@EFragment(resName="fragment_dialog_password")
public class DialogPasswordV11 extends DialogFragment {
	@ViewById(resName="passwordField")
	EditText passwordField;
	
	@ViewById(resName="dialog_yes")
	View dialog_yes;
	@ViewById(resName="dialog_no")
	View dialog_no;

	PasswordConfirmedListener listener = null;
	
	public void setListener(final PasswordConfirmedListener listener) {
		this.listener = listener;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		getDialog().setTitle(R.string.password_required);
		// Let annotations deal with it
		return null;
	}
	
	@Click(resName="dialog_no")
	void cancel() {
		dismiss();
	}
	
	@Click(resName="dialog_yes")
	void confirm() {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		String currentPassword = settings.getString(PasswordPrefs.KEY_PASSWORD,
				"");
		String enteredPassword = passwordField.getText().toString();

		// We want to return true or false, user has entered correct
		// password
		checkPassword(enteredPassword, currentPassword);
	}

	private void checkPassword(String enteredPassword, String currentPassword) {
		if ("".equals(currentPassword)
				|| currentPassword.equals(enteredPassword)) {
			if (listener != null) {
				listener.onPasswordConfirmed();
			}
			dismiss();
		} else {
			Animation shake = AnimationUtils.loadAnimation(getActivity(),
					R.anim.shake);
			passwordField.startAnimation(shake);
			Toast.makeText(getActivity(), getText(R.string.password_incorrect),
					Toast.LENGTH_SHORT).show();
		}
	}
}