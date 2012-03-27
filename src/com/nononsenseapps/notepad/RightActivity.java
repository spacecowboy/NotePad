package com.nononsenseapps.notepad;

/**
 * Convenience class where external edits are opened.
 *
 */
public class RightActivity extends MainActivity {

	/**
	 * Launches the main activity
	 */
	@Override
	protected void goUp() {
		finish();
		super.goUp();
	}
}
