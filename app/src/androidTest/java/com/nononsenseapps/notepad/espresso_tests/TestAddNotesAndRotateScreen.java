package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;

import androidx.test.filters.LargeTest;

import com.nononsenseapps.ui.TitleNoteTextView;

import org.junit.Before;
import org.junit.Test;


@LargeTest
public class TestAddNotesAndRotateScreen extends BaseTestClass {

	String noteName1, noteName2, noteName3, noteName4;

	@Before
	public void initStrings() {
		noteName1 = "prepare food";
		noteName2 = "take dogs out";
		noteName3 = "water plants";
		noteName4 = "sleep";
	}

	@Test
	public void testAddNotesAndRotateScreen() {
		EspressoHelper.hideShowCaseViewIfShown();

		String[] noteNames = { noteName1, noteName2, noteName3, noteName4 };

		EspressoHelper.closeDrawer();
		EspressoHelper.createNotes(noteNames);
		EspressoHelper.navigateUp();

		EspressoHelper.rotateScreen();

		// check that textviews still show up
		onView(allOf(instanceOf(TitleNoteTextView.class), withText(noteNames[0])))
				.check(matches(isDisplayed()));
		onView(allOf(instanceOf(TitleNoteTextView.class), withText(noteNames[1])))
				.check(matches(isDisplayed()));
		onView(allOf(instanceOf(TitleNoteTextView.class), withText(noteNames[2])))
				.check(matches(isDisplayed()));
		onView(allOf(instanceOf(TitleNoteTextView.class), withText(noteNames[3])))
				.check(matches(isDisplayed()));
	}
}


