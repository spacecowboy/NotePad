package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;

import androidx.test.espresso.Espresso;
import androidx.test.filters.LargeTest;

import com.nononsenseapps.notepad.R;

import org.junit.Test;

@LargeTest
public class TestSaveLoadJsonBackup extends BaseTestClass {

	final String noteText1 = "random note";
	final String noteText2 = "other random note";

	@Test
	public void testSaveLoadBackup() {
		EspressoHelper.closeDrawer();
		EspressoHelper.hideShowCaseViewIfShown();

		// add 2 notes
		EspressoHelper.createNoteWithName(noteText1);
		EspressoHelper.createNoteWithName(noteText2);

		EspressoHelper.navigateUp();

		// make a backup
		openContextualActionModeOverflowMenu();
		String SETTINGS_TEXT = getStringResource(R.string.menu_preferences);
		onView(withText(SETTINGS_TEXT))
				.perform(scrollTo()) // in case the keyboard is covering the menu
				.perform(click());

		String SETTINGS_BACKUP_TEXT = getStringResource(R.string.backup);
		onView(withText(SETTINGS_BACKUP_TEXT)).perform(click());

		String EXPORT_BACKUP_TEXT = getStringResource(R.string.backup_export);
		onView(withText(EXPORT_BACKUP_TEXT)).perform(click());
		onView(withId(android.R.id.button1)).perform(click());

		// return to the notes list
		Espresso.pressBack();
		Espresso.pressBack();

		// check & delete both notes
		clickCheckBoxAt(0);
		clickCheckBoxAt(1);

		// clear completed notes
		openContextualActionModeOverflowMenu();
		String CLEAR_COMPLETED = getStringResource(R.string.menu_clearcompleted);
		onView(withText(CLEAR_COMPLETED)).perform(click());
		onView(withId(android.R.id.button1)).perform(click());

		// restore the backup
		openContextualActionModeOverflowMenu();
		onView(withText(SETTINGS_TEXT)).perform(click());
		onView(withText(SETTINGS_BACKUP_TEXT)).perform(click());

		String IMPORT_BACKUP_TEXT = getStringResource(R.string.backup_import);
		onView(withText(IMPORT_BACKUP_TEXT)).perform(click());
		onView(withId(android.R.id.button1)).perform(click());

		// return to the notes list
		Espresso.pressBack();
		Espresso.pressBack();

		// ensure both notes were restored
		onView(withText(noteText1)).check(matches(isDisplayed()));
		onView(withText(noteText2)).check(matches(isDisplayed()));
	}

	// this one expects the list to have 2 children
	private void clickCheckBoxAt(int position) {
		var i = onData(anything())
				.inAdapterView(allOf(withId(android.R.id.list), hasChildCount(2)))
				.atPosition(position)
				.onChildView(withId(R.id.checkbox));
		i.perform(click());
	}

}
