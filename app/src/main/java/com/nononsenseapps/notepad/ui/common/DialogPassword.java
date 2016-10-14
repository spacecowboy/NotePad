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

package com.nononsenseapps.notepad.ui.common;

import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.databinding.FragmentDialogPasswordBinding;

public class DialogPassword extends DialogFragment {

	PasswordConfirmedListener listener = null;
	private FragmentDialogPasswordBinding binding;

	public void setListener(final PasswordConfirmedListener listener) {
		this.listener = listener;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		final String currentPassword = settings.getString(
				DialogPasswordSettings.KEY_PASSWORD, "");
		if (currentPassword.isEmpty()) {
			getDialog().setTitle(R.string.enter_new_password);
		}
		else {
			getDialog().setTitle(R.string.password_required);
		}

		binding = DataBindingUtil.inflate(inflater, R.layout.fragment_dialog_password, container, true);
		return binding.getRoot();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		showField();
	}

	public void showField() {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		final String currentPassword = settings.getString(
				DialogPasswordSettings.KEY_PASSWORD, "");
		if (currentPassword.isEmpty()) {
			binding.passwordVerificationField.setVisibility(View.VISIBLE);
		}
		else {
			binding.passwordVerificationField.setVisibility(View.GONE);
		}

        binding.passwordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                binding.buttons.dialogYes.setEnabled(s.length() > 0);
            }
        });

        binding.buttons.dialogNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        binding.buttons.dialogYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirm();
            }
        });
	}

	void confirm() {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		final String currentPassword = settings.getString(DialogPasswordSettings.KEY_PASSWORD,
				"");
		final String enteredPassword = binding.passwordField.getText().toString();
		final String verifiedPassword = binding.passwordVerificationField.getText()
				.toString();

		if (currentPassword.isEmpty()) {
			setPassword(enteredPassword, verifiedPassword);
		}
		else {
			// We want to return true or false, user has entered correct
			// password
			checkPassword(enteredPassword, currentPassword);
		}
	}

	private void checkPassword(final String enteredPassword,
			final String currentPassword) {
		if (currentPassword.equals(enteredPassword)) {
			if (listener != null) {
				listener.onPasswordConfirmed();
			}
			dismiss();
		}
		else {
			Animation shake = AnimationUtils.loadAnimation(getActivity(),
					R.anim.shake);
			binding.passwordField.startAnimation(shake);
			Toast.makeText(getActivity(), getText(R.string.password_incorrect),
					Toast.LENGTH_SHORT).show();
		}
	}

	private void setPassword(final String pass1, final String pass2) {
		if (pass1 != null && !pass1.isEmpty() && pass1.equals(pass2)) {
			PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
					.putString(DialogPasswordSettings.KEY_PASSWORD, pass1).commit();
			if (listener != null) {
				listener.onPasswordConfirmed();
			}
			dismiss();
		}
		else {
			Animation shake = AnimationUtils.loadAnimation(getActivity(),
					R.anim.shake);
			binding.passwordVerificationField.startAnimation(shake);
			Toast.makeText(getActivity(),
					getText(R.string.passwords_dont_match), Toast.LENGTH_SHORT)
					.show();
		}
	}

	public interface PasswordConfirmedListener {
		void onPasswordConfirmed();
	}
}
