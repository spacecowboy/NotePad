package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;

import androidx.test.filters.LargeTest;

import com.nononsenseapps.notepad.R;

import org.junit.Before;
import org.junit.Test;

@LargeTest
public class TestCompletedTasksAreCleared extends BaseTestClass {

	String noteName1, noteName2, noteName3, noteName4;

	@Before
	public void initStrings() {
		noteName1 = "prepare food";
		noteName2 = "take dogs out";
		noteName3 = "water plants";
		noteName4 = "sleep";
	}

	@Test
	public void testCompletedTasksAreCleared() {

		EspressoHelper.closeDrawer();
		EspressoHelper.hideShowCaseViewIfShown();

		String[] noteNames = { noteName1, noteName2, noteName3, noteName4 };
		EspressoHelper.createNotes(noteNames);

		EspressoHelper.navigateUp();
		clickCheckBoxAt(1);
		clickCheckBoxAt(3);

		//clear notes
		openContextualActionModeOverflowMenu();
		String CLEAR_COMPLETED = myActivityRule.getActivity().getString(R.string.menu_clearcompleted);
		onView(withText(CLEAR_COMPLETED)).perform(click());
		onView(withId(android.R.id.button1)).perform(click());

		//check that the notes do not exist any more
		onView(withText(noteNames[0]))
				.check(doesNotExist());
		onView(withText(noteNames[2]))
				.check(doesNotExist());
	}

	// this one expects the list to have 4 children
	private void clickCheckBoxAt(int position) {
		var i = onData(anything())
				.inAdapterView(allOf(withId(android.R.id.list), hasChildCount(4)))
				.atPosition(position)
				.onChildView(withId(R.id.checkbox));
		i.perform(click());
	}

}
