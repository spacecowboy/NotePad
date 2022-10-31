package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.filters.LargeTest;

import com.nononsenseapps.notepad.R;

import org.hamcrest.Matcher;
import org.junit.*;

@LargeTest
public class Espresso_TestCompletedTasksAreCleared extends BaseTestClass{

    String noteName1, noteName2, noteName3, noteName4;

//    @Rule
//    public ActivityTestRule<ActivityList> myActivityRule =
//            new ActivityTestRule<ActivityList>(ActivityList.class);

    @Before
    public void initStrings(){
        noteName1 = "prepare food";
        noteName2 = "take dogs out";
        noteName3 = "water plants";
        noteName4 = "sleep";
    }

    @Test
    public void testCompletedTasksAreCleared(){

        Helper.closeDrawer();

        String[] noteNames = {noteName1, noteName2, noteName3, noteName4};
        Helper.createNotes(noteNames);

        clickCheckBoxAt(1);
        clickCheckBoxAt(3);

        //clear notes
        openContextualActionModeOverflowMenu();
        onView(withText("Clear completed")).perform(click());
        onView(withText("OK")).perform(click());

        //check that the notes do not exist any more
        onView(withText(noteNames[0]))
                .check(doesNotExist());
        onView(withText(noteNames[2]))
                .check(doesNotExist());
    }

    private void clickCheckBoxAt(int position) {
        onView(withId(android.R.id.list)).perform(
                RecyclerViewActions.actionOnItemAtPosition(
                        position, MyViewAction.clickChildViewWithId(
                            R.id.checkbox
                ))
        );
    }

    private static class MyViewAction{

        public static ViewAction clickChildViewWithId(final int id){
            return new ViewAction() {
                @Override
                public Matcher<View> getConstraints() {
                    return null;
                }

                @Override
                public String getDescription() {
                    return "Click on a child view with specified id.";
                }

                @Override
                public void perform(UiController uiController, View view) {
                    View v = view.findViewById(id);
                    v.performClick();

                }
            };
        }
    }


}
