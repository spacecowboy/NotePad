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

import android.content.Context;
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
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.databinding.AppPrefPasswordLayoutBinding;
import com.nononsenseapps.notepad.fragments.DialogPasswordV11;

public class PasswordPrefs extends Fragment {

	public static final String KEY_PASSWORD = "secretPassword";

	private Context activity;


	// TODO copy from DialogPasswordSettings.java and delete that file

	@Override
	public void onAttach(@NonNull Context activity) {
		super.onAttach(activity);
		this.activity = activity;
	}

	/**
	 * for {@link R.layout#app_pref_password_layout}
	 */
	private AppPrefPasswordLayoutBinding mBinding;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
							 @Nullable Bundle savedInstanceState) {
		mBinding = AppPrefPasswordLayoutBinding.inflate(inflater, container, false);
		return mBinding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		// here you call methods with the old @AfterViews annotation
		mBinding.applyPassword.setOnClickListener(v -> applyPassword());
		mBinding.clearPassword.setOnClickListener(v -> clearPassword());
	}

	private void applyPassword() {
		if (mBinding.tempPassword1.getText().toString()
				.equals(mBinding.tempPassword2.getText().toString())) {
			// They are the same
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(activity);
			String currentPassword = settings.getString(KEY_PASSWORD, "");

			if ("".equals(currentPassword)) {
				// Save the password directly
				settings.edit()
						.putString(KEY_PASSWORD, mBinding.tempPassword1.getText().toString())
						.commit();
				Toast.makeText(activity, getText(R.string.password_set),
						Toast.LENGTH_SHORT).show();
			} else {
				// confirm with existing password first
				showPasswordDialog(mBinding.tempPassword1.getText().toString());
			}

		} else {
			// Show a toast so the user knows he did something wrong
			Animation shake = AnimationUtils.loadAnimation(activity,
					R.anim.shake);
			mBinding.tempPassword2.startAnimation(shake);
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
		final DialogPasswordV11 pd = new DialogPasswordV11();
		pd.setListener(() -> {
			PreferenceManager
					.getDefaultSharedPreferences(getActivity()).edit()
					.putString(PasswordPrefs.KEY_PASSWORD, newPassword)
					.commit();
			Toast.makeText(getActivity(),
					("".equals(newPassword))
							? getText(R.string.password_cleared)
							: getText(R.string.password_set),
					Toast.LENGTH_SHORT).show();
		});
		pd.show(getFragmentManager(), "pw-verify");
	}
}