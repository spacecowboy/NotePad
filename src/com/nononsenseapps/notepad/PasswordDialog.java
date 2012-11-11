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

package com.nononsenseapps.notepad;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.nononsenseapps.notepad_donate.R;
import com.nononsenseapps.notepad.interfaces.PasswordChecker;
import com.nononsenseapps.notepad.prefs.PasswordPrefs;

public class PasswordDialog extends DialogFragment implements OnClickListener {
	private Activity activity;
	private EditText passwordText;
	private int actionId = -1;
	
//	public static void showPasswordDialog() {
//		// Create and show the dialog.
//		DialogFragment newFragment = new PasswordDialog();
//		FragmentTransaction ft = newFragment.getFragmentManager().beginTransaction();
//		Fragment prev = newFragment.getFragmentManager().findFragmentByTag("newpassdialog");
//		if (prev != null) {
//			ft.remove(prev);
//		}
//		ft.addToBackStack(null);
//		
//		newFragment.show(ft, "newpassdialog");
//	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.confirm_password_dialog, container,
				false);
		getDialog().setTitle(R.string.password_required);

		passwordText = (EditText) v.findViewById(R.id.editTitle);

		Button yesButton = (Button) v.findViewById(R.id.dialog_yes);
		yesButton.setOnClickListener(this);

		Button noButton = (Button) v.findViewById(R.id.dialog_no);
		noButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((PasswordChecker) activity).PasswordVerified(getResult(false));
				dismiss();
			}
		});

		return v;
	}

	@Override
	public void onClick(View v) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(activity);
		String currentPassword = settings.getString(PasswordPrefs.KEY_PASSWORD,
				"");
		String enteredPassword = passwordText.getText().toString();

			// We want to return true or false, user has entered correct
			// password
			checkPassword(enteredPassword, currentPassword);
	}

	private void checkPassword(String enteredPassword, String currentPassword) {
		if ("".equals(currentPassword)
				|| currentPassword.equals(enteredPassword)) {
			((PasswordChecker) activity).PasswordVerified(getResult(true));
			dismiss();
		} else {
			Animation shake = AnimationUtils.loadAnimation(activity,
					R.anim.shake);
			passwordText.startAnimation(shake);
			Toast.makeText(activity, "Password incorrect string",
					Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * To be able to tell what it is we are verifying later
	 * @param actionId
	 */
	public void setAction(int actionId) {
		this.actionId = actionId;
	}
	
	public ActionResult getResult(boolean verified) {
		ActionResult result = new ActionResult(actionId);
		result.result = verified;
		return result;
	}
	
	public static class ActionResult {
		public boolean result = false;
		public int actionId = -1;
		public ActionResult() {
		}
		public ActionResult(int actionId) {
			this.actionId = actionId;
		}
	}
}