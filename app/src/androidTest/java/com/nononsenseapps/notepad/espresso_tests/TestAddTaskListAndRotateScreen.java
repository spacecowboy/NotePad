package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.equalToIgnoringCase;
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

		EspressoHelper.rotateScreenAndWait();

		// make sure the task list is still visible.
		// if the rotations didn't finish, it will crash here
		RecyclerViewActions
				.scrollTo(hasDescendant(withText(equalToIgnoringCase(taskListName))));
		onView(allOf(
					withText(equalToIgnoringCase(taskListName)),
					withId(android.R.id.text1)))
				.check(matches(isDisplayed()));
	}
}
