package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;

import androidx.test.filters.LargeTest;

import com.nononsenseapps.notepad.R;

import org.junit.Before;
import org.junit.Test;

@LargeTest
public class TestAddNewNoteWithDueDateCheckDateIsVisible extends BaseTestClass {

	private String noteName1;

	@Before
	public void initStrings() {
		noteName1 = "prepare food";

	}

	@Test
	public void testAddNewNoteWithDueDateCheckDateIsVisible() {

		EspressoHelper.closeDrawer();
		EspressoHelper.hideShowCaseViewIfShown();

		EspressoHelper.createNoteWithName(noteName1);
		onView(withId(R.id.dueDateBox)).perform(click());
		onView(withId(R.id.done)).perform(click());

		EspressoHelper.navigateUp();

		// target only the dateview in the note shown in the "tasks" list
		var dateView = onView(allOf(withId(R.id.date), isCompletelyDisplayed()));
		dateView.check(matches(isDisplayed()));
	}
}