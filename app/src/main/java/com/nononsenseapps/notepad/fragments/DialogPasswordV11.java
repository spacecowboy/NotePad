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

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.fragments.DialogPassword.PasswordConfirmedListener;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

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
		String currentPassword = settings.getString(DialogPasswordSettings.KEY_PASSWORD,
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