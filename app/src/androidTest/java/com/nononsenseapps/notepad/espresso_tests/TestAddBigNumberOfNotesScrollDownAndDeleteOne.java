package com.nononsenseapps.notepad.espresso_tests;

import static android.view.View.FIND_VIEWS_WITH_TEXT;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;

import android.view.View;
import android.widget.ListView;

import androidx.test.espresso.ViewAssertion;
import androidx.test.filters.LargeTest;

import com.nononsenseapps.notepad.R;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import java.util.ArrayList;

@LargeTest
public class TestAddBigNumberOfNotesScrollDownAndDeleteOne extends BaseTestClass {

	private final String[] noteNameList = {
			"prepare food", "take dogs out", "water plants", "sleep",
			"go for a jog", "do some work", "play with the dog",
			"work out", "do weird stuff", "read a book", "drink water",
			"write a book", "proofread the book", "publish the book",
			"ponder life", "build a house", "repair the house", "call contractor",
			"write another book", "scrap the book project", "start a blog",
			"  ", "     "
	};

	@Test
	public void testAddBigNumberOfNotesScrollDownAndDeleteOne() {
		EspressoHelper.closeDrawer();
		EspressoHelper.hideShowCaseViewIfShown();

		if (getNumberOfNotesInList() < noteNameList.length) {
			EspressoHelper.createNotes(noteNameList);
		}

		onData(anything())
				.inAdapterView(allOf(hasMinimumChildCount(10), withId(android.R.id.list)))
				.atPosition(getNumberOfNotesInList() - 1) // last note in list
				.perform(scrollTo())
				.perform(click());

		// delete the note
		onView(withId(R.id.menu_delete)).perform(click());
		onView(withId(android.R.id.button1)).perform(click());

		// check that the 1° note added was deleted
		onView(allOf(withId(android.R.id.list), isDisplayed()))
				.check(doesntHaveViewWithText(noteNameList[0]));
	}

	private static int getNumberOfNotesInList() {
		final int[] numberOfAdapterItems = new int[1];
		onView(allOf(isDisplayed(), withId(android.R.id.list))).check(matches(new TypeSafeMatcher<>() {
			@Override
			public boolean matchesSafely(View view) {
				ListView listView = (ListView) view;

				//here we assume the adapter has been fully loaded already
				numberOfAdapterItems[0] = listView.getAdapter().getCount();

				return true;
			}

			@Override
			public void describeTo(Description description) {}
		}));
		return numberOfAdapterItems[0];
	}

	/**
	 * credit to Chemouna @ GitHub https://gist.github.com/chemouna/00b10369eb1d5b00401b
	 */
	private static ViewAssertion doesntHaveViewWithText(final String text) {
		return (view, e) -> {
			if (!(view instanceof ListView)) {
				throw e;
			}
			ListView rv = (ListView) view;
			ArrayList<View> outviews = new ArrayList<>();
			for (int index = 0; index < rv.getAdapter().getCount(); index++) {
				rv
						//.findViewHolderForAdapterPosition(index)
						//.itemView
						.findViewsWithText(outviews, text, FIND_VIEWS_WITH_TEXT);
				if (outviews.size() > 0) break;
			}
			assertTrue(outviews.isEmpty());
		};
	}


}
