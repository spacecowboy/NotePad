package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.filters.LargeTest;

import com.nononsenseapps.notepad.R;

import org.junit.Before;
import org.junit.Test;

@LargeTest
public class TestCreateNoteAndDeleteIt extends BaseTestClass {

	private String noteName1;

	@Before
	public void initStrings() {
		noteName1 = "prepare food";
	}

	@Test
	public void testCreateNoteAndDeleteIt() {

		EspressoHelper.closeDrawer();
		EspressoHelper.hideShowCaseViewIfShown();

		EspressoHelper.createNoteWithName(noteName1);
		EspressoHelper.navigateUp();

		onView(withText(noteName1)).perform(click());
		onView(withId(R.id.menu_delete)).perform(click());
		onView(withId(android.R.id.button1)).perform(click());

		// assert that we're back in the list
		onView(withId(R.id.menu_search)).check(matches(isDisplayed()));

		//check that the view is not visible
		onView(withText(noteName1)).check(doesNotExist());
	}

}
