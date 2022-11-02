package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;

@LargeTest
public class Espresso_TestAddTaskListCheckItIsAddedToDrawer extends BaseTestClass {

	private String taskListName;

	@Before
	public void initStrings() {
		taskListName = "a random task list";
	}

	@Test
	public void testAddTaskListCheckItIsAddedToDrawer() {
		EspressoHelper.hideShowCaseViewIfShown();

		EspressoHelper.createTaskList(taskListName);
		EspressoHelper.openDrawer();

		//check that the new note is found and has the correct text
		onView(allOf(withText(taskListName), withId(android.R.id.text1)))
				.check(matches(isDisplayed()));
	}


}

