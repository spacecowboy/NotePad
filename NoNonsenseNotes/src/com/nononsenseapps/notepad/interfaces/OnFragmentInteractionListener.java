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
	public void onFragmentInteraction(final Uri uri, final View origin);
	public void addTaskInList(final String text, final long listId);
	public void closeFragment(final Fragment fragment);
}
