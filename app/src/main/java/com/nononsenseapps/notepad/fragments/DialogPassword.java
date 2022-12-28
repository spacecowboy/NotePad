/*
 * Copyright (C) 2012 Jonas Kalderstam
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.databinding.FragmentDialogPasswordBinding;
import com.nononsenseapps.notepad.prefs.PasswordPrefs;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;

@EFragment()
public class DialogPassword extends DialogFragment {

	PasswordConfirmedListener listener = null;

	public interface PasswordConfirmedListener {
		void onPasswordConfirmed();
	}

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
							 @Nullable Bundle savedInstanceState) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		final String currentPassword = settings.getString(PasswordPrefs.KEY_PASSWORD, "");
		if (currentPassword.isEmpty()) {
			getDialog().setTitle(R.string.enter_new_password);
		} else {
			getDialog().setTitle(R.string.password_required);
		}

		mBinding = FragmentDialogPasswordBinding.inflate(inflater, container, false);
		return mBinding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		// here you call methods with the old @AfterViews annotation
		showField();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mBinding = null;
	}

	public void showField() {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		final String currentPassword = settings.getString(PasswordPrefs.KEY_PASSWORD, "");
		if (currentPassword.isEmpty()) {
			mBinding.passwordVerificationField.setVisibility(View.VISIBLE);
		} else {
			mBinding.passwordVerificationField.setVisibility(View.GONE);
		}
	}

	@Click(resName = "dialog_no")
	void cancel() {
		dismiss();
	}

	@Click(resName = "dialog_yes")
	void confirm() {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		final String currentPassword = settings.getString(PasswordPrefs.KEY_PASSWORD,
				"");
		final String enteredPassword = mBinding.passwordField.getText().toString();
		final String verifiedPassword = mBinding.passwordVerificationField.getText().toString();

		if (currentPassword.isEmpty()) {
			setPassword(enteredPassword, verifiedPassword);
		} else {
			// We want to return true or false, user has entered correct password
			checkPassword(enteredPassword, currentPassword);
		}
	}

	private void checkPassword(final String enteredPassword, final String currentPassword) {
		if (currentPassword.equals(enteredPassword)) {
			if (listener != null) {
				listener.onPasswordConfirmed();
			}
			dismiss();
		} else {
			Animation shake = AnimationUtils.loadAnimation(getActivity(),
					R.anim.shake);
			mBinding.passwordField.startAnimation(shake);
			Toast.makeText(getActivity(), getText(R.string.password_incorrect),
					Toast.LENGTH_SHORT).show();
		}
	}

	private void setPassword(final String pass1, final String pass2) {
		if (pass1 != null && !pass1.isEmpty() && pass1.equals(pass2)) {
			PreferenceManager.getDefaultSharedPreferences(getActivity())
					.edit()
					.putString(PasswordPrefs.KEY_PASSWORD, pass1)
					.commit();
			if (listener != null) {
				listener.onPasswordConfirmed();
			}
			dismiss();
		} else {
			Animation shake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
			mBinding.passwordVerificationField.startAnimation(shake);
			Toast.makeText(getActivity(), getText(R.string.passwords_dont_match),
					Toast.LENGTH_SHORT).show();
		}
	}
}
