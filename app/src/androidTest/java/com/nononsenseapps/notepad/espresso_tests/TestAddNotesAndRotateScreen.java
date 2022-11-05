package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.nononsenseapps.notepad.espresso_tests.OrientationChangeAction.orientationLandscape;
import static com.nononsenseapps.notepad.espresso_tests.OrientationChangeAction.orientationPortrait;

import android.os.SystemClock;

import androidx.test.filters.LargeTest;

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

		// rotate screen
		onView(isRoot()).perform(orientationLandscape());
		onView(isRoot()).perform(orientationPortrait());

		// wait for it to finish the rotation(s)
		SystemClock.sleep(1500);

		//check that textviews still show up
		onView(withText(noteNames[0])).check(matches(isDisplayed()));
		onView(withText(noteNames[1])).check(matches(isDisplayed()));
		onView(withText(noteNames[2])).check(matches(isDisplayed()));
		onView(withText(noteNames[3])).check(matches(isDisplayed()));
	}
}


