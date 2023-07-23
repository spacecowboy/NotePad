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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.helpers.PreferencesHelper;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.databinding.FragmentDialogPasswordBinding;
import com.nononsenseapps.notepad.fragments.DialogPassword.PasswordConfirmedListener;
import com.nononsenseapps.notepad.prefs.PasswordPrefs;

/**
 * Full copy of {@link DialogPassword}, but extending native fragment class
 * {@link android.app.DialogFragment} instead.
 * It is called when the user changes the existing password.
 * It asks to input the old password.
 */
public class DialogPasswordV11 extends DialogFragment {

	// TODO DialogPassword.java is better. Try to put the functions of this dialog back
	//  into that file, then delete this file

	PasswordConfirmedListener listener = null;

	public void setListener(final PasswordConfirmedListener listener) {
		this.listener = listener;
	}

	/**
	 * for {@link R.layout#fragment_dialog_password}
	 */
	private FragmentDialogPasswordBinding mBinding;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savInstState) {
		getDialog().setTitle(R.string.password_required);
		mBinding = FragmentDialogPasswordBinding.inflate(inflater, container, false);
		return mBinding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		// here you call methods with the old @AfterViews annotation
		mBinding.buttons.dialogNo.setOnClickListener(v -> dismiss());
		mBinding.buttons.dialogYes.setOnClickListener(v -> confirm());
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mBinding = null;
	}

	void confirm() {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		String currentPassword = settings.getString(PasswordPrefs.KEY_PASSWORD,
				"");
		String enteredPassword = mBinding.passwordField.getText().toString();

		// We want to return true or false, user has entered correct
		// password
		checkPassword(enteredPassword, currentPassword);
	}

	private void checkPassword(String enteredPassword, String currentPassword) {
		if ("".equals(currentPassword) || currentPassword.equals(enteredPassword)) {
			if (listener != null) {
				listener.onPasswordConfirmed();
			}
			dismiss();
		} else {
			if (PreferencesHelper.areAnimationsEnabled(this.getContext())) {
				// shake the dialog to show that the password is wrong
				Animation shake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
				mBinding.passwordField.startAnimation(shake);
			}
			Toast.makeText(getActivity(), getText(R.string.password_incorrect),
					Toast.LENGTH_SHORT).show();
		}
	}
}