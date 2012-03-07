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

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

public class DatePickerDialogFragment extends DialogFragment {
	private NotesEditorFragment mFragment = null;

//	public DatePickerDialogFragment() {
//		super();
//		// This will result in an immediate finish. Only here to avoid
//		// crashes by the system on restarts
//	}
//
//	public DatePickerDialogFragment(NotesEditorFragment callback) {
//		super();
//		mFragment = callback;
//	}
	
	public void setCallback(NotesEditorFragment fragment) {
		mFragment = fragment;
	}

	@Override
	public void onCreate(Bundle saves) {
		super.onCreate(saves);
		if (mFragment == null) {
			// Device was rotated perhaps
			// This activity was destroyed and restarted. Can't save the
			// callback then.
			dismiss();
		}
	}

	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (mFragment == null) {
			dismiss();
			return super.onCreateDialog(savedInstanceState);
		}
		else
			return new DatePickerDialog(getActivity(), mFragment,
					mFragment.year, mFragment.month, mFragment.day);
	}
}
