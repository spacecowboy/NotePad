package com.nononsenseapps.notepad.interfaces;

import com.nononsenseapps.notepad.activities.ActivityMain;
import com.nononsenseapps.notepad.fragments.TaskListViewPagerFragment;

/**
 * Allows interactions between {@link ActivityMain} and {@link TaskListViewPagerFragment}
 */
public interface ListOpener {
	void openList(final long id);
}