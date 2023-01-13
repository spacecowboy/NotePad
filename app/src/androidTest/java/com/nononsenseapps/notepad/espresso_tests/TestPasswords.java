package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.instanceOf;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.ui.TitleNoteTextView;

import org.junit.Test;

public class TestPasswords extends BaseTestClass {

	// when the note is locked, only this is shown
	final String noteTitle = "this is my note";

	// note tile + content, shown in the edittext
	final String fullNoteText1 = noteTitle + "\n.\ncontent line";

	// typed into the popup
	final String password = "itsnotasecrettoanybody";

	@Test
	public void testAddNoteLockWithPassword() {
		EspressoHelper.hideShowCaseViewIfShown();

		EspressoHelper.createNoteWithName(fullNoteText1);
		EspressoHelper.navigateUp();

		onView(withText(fullNoteText1)).perform(click());
		openContextualActionModeOverflowMenu();

		String MENU_TEXT = getStringResource(R.string.lock_note);
		onView(withText(MENU_TEXT)).perform(click());

		EspressoHelper
				.onViewWithIdInDialog(R.id.passwordField)
				.perform(typeText(password));
		EspressoHelper
				.onViewWithIdInDialog(R.id.passwordVerificationField)
				.perform(typeText(password));
		EspressoHelper
				.onViewWithIdInDialog(R.id.dialog_yes)
				.perform(click());
		EspressoHelper.waitUi();

		// then it opens the popup again, to ask the password
		EspressoHelper
				.onViewWithIdInDialog(R.id.passwordField)
				.check(matches(isDisplayed()));
		EspressoHelper
				.onViewWithIdInDialog(R.id.passwordField)
				.perform(typeText(password));
		EspressoHelper
				.onViewWithIdInDialog(R.id.dialog_yes)
				.check(matches(isDisplayed()));
		EspressoHelper
				.onViewWithIdInDialog(R.id.dialog_yes)
				.perform(click());

		// the note on the (custom) edittext should appear correctly
		onView(withId(R.id.taskText)).check(matches(withText(fullNoteText1)));
		EspressoHelper.navigateUp();

		// in the list view, only the title is shown
		onView(instanceOf(TitleNoteTextView.class))
				.check(matches(withText(noteTitle)));
	}
}
