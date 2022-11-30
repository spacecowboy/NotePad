package com.nononsenseapps.notepad.espresso_tests;

import static android.view.View.FIND_VIEWS_WITH_TEXT;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;

import android.view.View;
import android.widget.ListView;

import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.matcher.CursorMatchers;
import androidx.test.filters.LargeTest;

import com.mobeta.android.dslv.DragSortListView;
import com.nononsenseapps.notepad.R;

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

	/**
	 * credit to Chemouna @ GitHub https://gist.github.com/chemouna/00b10369eb1d5b00401b
	 */
	private static ViewAssertion doesNotHaveViewWithText(final String text) {
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

	@Test
	public void testAddBigNumberOfNotesScrollDownAndDeleteOne() {
		EspressoHelper.closeDrawer();
		EspressoHelper.hideShowCaseViewIfShown();

		EspressoHelper.createNotes(noteNameList);

		// exit the note editing mode
		EspressoHelper.navigateUp();

		// click on the bottom-most note
		onData(CursorMatchers.withRowString("title", noteNameList[0]))
				.inAdapterView(allOf(
						hasMinimumChildCount(1),
						instanceOf(DragSortListView.class)))
				.perform(scrollTo())
				.perform(click());

		// delete the note
		onView(withId(R.id.menu_delete)).perform(click());
		EspressoHelper.waitUi();
		onView(withId(android.R.id.button1)).perform(click());

		// check that the 1° note added was deleted
		onView(allOf(withId(android.R.id.list), isDisplayed()))
				.check(doesNotHaveViewWithText(noteNameList[0]));

		// if the showcaseview is visible when closing the app, there will be a crash
		EspressoHelper.hideShowCaseViewIfShown();
	}


}
