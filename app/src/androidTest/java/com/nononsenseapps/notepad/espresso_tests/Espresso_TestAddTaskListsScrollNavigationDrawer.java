package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.matcher.CursorMatchers;
import androidx.test.filters.LargeTest;

import com.nononsenseapps.notepad.R;

import org.junit.Before;
import org.junit.Test;

@LargeTest
public class Espresso_TestAddTaskListsScrollNavigationDrawer extends BaseTestClass {

	String[] taskListNames = { "Lorem", "ipsum ", "dolor ", "sit ", "amet", "consectetur ",
			"adipiscing ", "elit", "sed ", "do ", "eiusmod ", "tempor ", "incididunt ",
			"ut ", "labore " };

	String SETTINGS_TEXT, SETTINGS_APPEARANCE_TEXT;

	@Before
	public void initStrings() {
		SETTINGS_TEXT = myActivityRule.getActivity().getString(R.string.menu_preferences);
		SETTINGS_APPEARANCE_TEXT = myActivityRule.getActivity().getString(R.string.settings_cat_appearance);
	}

	@Test
	public void testAddTaskListsScrollNavigationDrawer() {
		EspressoHelper.hideShowCaseViewIfShown();

		for (String name : taskListNames) {
			EspressoHelper.createTaskList(name);
			EspressoHelper.openDrawer();
		}

		EspressoHelper.openDrawer();

		onData(anything())
				.inAdapterView(hasMinimumChildCount(11))
				.atPosition(21)
				.perform(scrollTo());

		onData(CursorMatchers.withRowString("title", "ut "))
				.inAdapterView(withId(R.id.leftDrawer))
				.check(matches(isDisplayed()));

		// open the preferences page and check that it is visible
		openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());
		onView(withText(SETTINGS_TEXT)).perform(click());
		onView(withText(SETTINGS_APPEARANCE_TEXT)).check(matches(isDisplayed()));
	}
}
