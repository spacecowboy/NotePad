package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.nononsenseapps.notepad.espresso_tests.OrientationChangeAction.orientationLandscape;
import static com.nononsenseapps.notepad.espresso_tests.OrientationChangeAction.orientationPortrait;
import static org.hamcrest.Matchers.allOf;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;

@LargeTest
public class TestAddTaskListAndRotateScreen extends BaseTestClass {

	private String taskListName;

	@Before
	public void initStrings() {
		taskListName = "a random task list";
	}

	@Test
	public void testAddTaskListAndRotateScreen() {
		EspressoHelper.hideShowCaseViewIfShown();
		EspressoHelper.createTaskList(taskListName);

		EspressoHelper.openDrawer();

		// rotate to landscape and back to portrait
		onView(isRoot()).perform(orientationLandscape(myActivityRule.getActivity()));
		onView(isRoot()).perform(orientationPortrait(myActivityRule.getActivity()));

		// make sure the task list is still visible
		RecyclerViewActions.scrollTo(hasDescendant(withText(taskListName)));
		onView(allOf(withText(taskListName), withId(android.R.id.text1)))
				.check(matches(isDisplayed()));
	}
}
