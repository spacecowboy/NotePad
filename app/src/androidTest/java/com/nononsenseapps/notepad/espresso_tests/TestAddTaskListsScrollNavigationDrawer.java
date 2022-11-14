package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import androidx.test.espresso.matcher.CursorMatchers;
import androidx.test.filters.LargeTest;

import com.nononsenseapps.notepad.R;

import org.junit.Before;
import org.junit.Test;

@LargeTest
public class TestAddTaskListsScrollNavigationDrawer extends BaseTestClass {

	String[] taskListNames = { "Lorem", "ipsum ", "dolor ", "sit ", "amet", "consectetur ",
			"adipiscing ", "elit", "sed ", "do ", "eiusmod ", "tempor ", "incididunt ",
			"ut ", "labore " };

	@Test
	public void testAddTaskListsScrollNavigationDrawer() {
		String SETTINGS_TEXT = getStringResource(R.string.menu_preferences);
		String SETTINGS_APPEARANCE_TEXT = getStringResource(R.string.settings_cat_appearance);

		EspressoHelper.hideShowCaseViewIfShown();

		for (String name : taskListNames) {
			EspressoHelper.createTaskList(name);
			EspressoHelper.openDrawer();
		}
		EspressoHelper.openDrawer();

		// onData() can scroll to the item, but can't click it
		onData(CursorMatchers.withRowString("title", "ut "))
				.inAdapterView(withId(R.id.leftDrawer))
				.perform(scrollTo())
				.check(matches(isDisplayed()));

		// onView() can click on the item, but can't scroll to it
		onView(allOf(withText("ut "), withId(android.R.id.text1)))
				.perform(click());


		EspressoHelper.openDrawer();

		// open the preferences page and check that it is visible
		openContextualActionModeOverflowMenu();
		onView(withText(SETTINGS_TEXT)).perform(click());
		onView(withText(SETTINGS_APPEARANCE_TEXT))
				.check(matches(isDisplayed()));
	}
}
