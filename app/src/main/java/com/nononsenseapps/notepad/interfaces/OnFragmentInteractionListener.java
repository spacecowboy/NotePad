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

package com.nononsenseapps.notepad.interfaces;

import android.net.Uri;
import android.support.v4.app.Fragment;
import android.view.View;

/**
 * This interface must be implemented by activities that contain 
 * fragments to allow an interaction in this fragment to be communicated to
 * the activity and potentially other fragments contained in that activity.
 */
public interface OnFragmentInteractionListener {
	//public void onFragmentInteraction(final Uri uri);
	void onFragmentInteraction(final Uri uri, final long listId, final View origin);
	void addTaskInList(final String text, final long listId);
	void closeFragment(final Fragment fragment);
}
