package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;

import com.nononsenseapps.notepad.R;

import org.hamcrest.Matchers;
import org.junit.Test;
import static android.view.View.FIND_VIEWS_WITH_TEXT;

import java.util.ArrayList;

@LargeTest
public class Espresso_TestAddBigNumberOfNotesScrollDownAndDeleteOne extends BaseTestClass{

    private String[] noteNameList =
            {"prepare food", "take dogs out", "water plants", "sleep",
            "go for a jog", "do some work", "play with the dog",
            "work out", "do weird stuff", "read a book", "drink water",
            "write a book", "proofread the book", "publish the book",
            "ponder life", "build a house", "repair the house", "call contractor",
            "write another book", "scrap the book project", "start a blog",
            "  ", "     "
            };



    @Test
    public void testAddBigNumberOfNotesScrollDownAndDeleteOne(){

        Helper.closeDrawer();
        //create the notes
        Helper.createNotes(noteNameList);

        onView(withId(android.R.id.list)).perform(RecyclerViewActions.actionOnItem(
                hasDescendant(withText(noteNameList[0])), click()
        ));

        //delete the last note
        onView(withId(R.id.menu_delete)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());

        onView(withId(android.R.id.list)).check(doesntHaveViewWithText(noteNameList[0]));

    }

    //credit to Chemouna @ GitHub,
    // https://gist.github.com/chemouna/00b10369eb1d5b00401b
    private static ViewAssertion doesntHaveViewWithText(final String text) {
        return new ViewAssertion() {
            @Override public void check(View view, NoMatchingViewException e) {
                if (!(view instanceof RecyclerView)) {
                    throw e;
                }
                RecyclerView rv = (RecyclerView) view;
                ArrayList<View> outviews = new ArrayList<>();
                for (int index = 0; index < rv.getAdapter().getItemCount(); index++) {
                    rv.findViewHolderForAdapterPosition(index).itemView.findViewsWithText(outviews, text,
                            FIND_VIEWS_WITH_TEXT);
                    if (outviews.size() > 0) break;
                }
                // assertThat(outviews).isEmpty(); unspecified where assertThat() is supposed to be
                assertTrue(outviews.isEmpty());
            }
        };
    }


}
