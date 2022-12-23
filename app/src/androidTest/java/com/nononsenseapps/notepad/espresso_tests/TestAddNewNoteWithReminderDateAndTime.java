package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import androidx.test.filters.LargeTest;

import com.nononsenseapps.notepad.R;

import org.junit.*;

@LargeTest
public class TestAddNewNoteWithReminderDateAndTime extends BaseTestClass {

	private String noteName1;

	@Before
	public void initStrings() {
		noteName1 = "prepare food";
	}

	@Test
	public void testAddNewNoteWithReminderDateAndTime() {
		EspressoHelper.hideShowCaseViewIfShown();

		EspressoHelper.createNoteWithName(noteName1);

		//add reminder
		onView(withId(R.id.notificationAdd)).perform(click());
		EspressoHelper.waitUi();

		//add date
		onView(withId(R.id.notificationDate)).perform(click());
		EspressoHelper.waitUi();
		onView(withId(android.R.id.button1)).perform(click());

		//add time
		onView(withId(com.nononsenseapps.notepad.R.id.notificationTime)).perform(click());
		onView(withId(android.R.id.button1)).perform(click());

		EspressoHelper.navigateUp();

		//check that the date field is visible
		onView(withText(noteName1)).perform(click());
		onView(withId(R.id.notificationDate)).check(matches(isDisplayed()));

		// maybe we should also check someting like
		// onView(withId(R.id.notificationDate)).check(matches(withText("november 10")));
	}
}
