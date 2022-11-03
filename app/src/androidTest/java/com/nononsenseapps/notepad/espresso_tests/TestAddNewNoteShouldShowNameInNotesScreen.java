package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;

@LargeTest
public class TestAddNewNoteShouldShowNameInNotesScreen extends BaseTestClass {

	private String noteName1;

	@Before
	public void initStrings() {
		noteName1 = "prepare food";
	}

	@Test
	public void testAddNewNoteShouldShowNameInNotesScreen() {

		EspressoHelper.closeDrawer();
		EspressoHelper.hideShowCaseViewIfShown();

		EspressoHelper.createNoteWithName(noteName1);
		EspressoHelper.navigateUp();

		onView(withText(noteName1)).check(matches(isDisplayed()));
	}

}
