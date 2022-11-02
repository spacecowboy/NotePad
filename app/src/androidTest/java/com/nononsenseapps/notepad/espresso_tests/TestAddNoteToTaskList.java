package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;

@LargeTest
public class TestAddNoteToTaskList extends BaseTestClass {

	private String taskListName, noteName1;

	@Before
	public void initStrings() {
		taskListName = "a random task list";
		noteName1 = "prepare food";
	}

	@Test
	public void testAddNoteToTaskList() {

		EspressoHelper.hideShowCaseViewIfShown();
		EspressoHelper.createTaskList(taskListName);

		//make sure the correct task list is opened
		EspressoHelper.openDrawer();
		onView(allOf(withText(taskListName), withId(android.R.id.text1))).perform(click());

		//add the note
		EspressoHelper.createNoteWithName(noteName1);
		EspressoHelper.navigateUp();

		//make sure that the number of notes for the task list is actually 1
		EspressoHelper.openDrawer();
		onView(allOf(withText(taskListName), hasSibling(withText("1")))).check(matches(withText(taskListName)));

	}
}
