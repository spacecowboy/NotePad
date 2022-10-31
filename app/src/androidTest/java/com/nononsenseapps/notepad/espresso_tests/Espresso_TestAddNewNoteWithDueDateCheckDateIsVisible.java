package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import androidx.test.filters.LargeTest;

import com.nononsenseapps.notepad.R;

import org.junit.*;

@LargeTest
public class Espresso_TestAddNewNoteWithDueDateCheckDateIsVisible extends BaseTestClass{

    private String noteName1;

//    @Rule
//    public ActivityTestRule<ActivityList> myActivityRule =
//            new ActivityTestRule<ActivityList>(ActivityList.class);

    @Before
    public void initStrings() {
        noteName1 = "prepare food";

    }

    @Test
    public void testAddNewNoteWithDueDateCheckDateIsVisible() {

        Helper.closeDrawer();

        Helper.createNoteWithName(noteName1);
        onView(withId(R.id.dueDateBox)).perform(click());
        onView(withId(R.id.done)).perform(click());

        Helper.navigateUp();
        onView(withId(R.id.date)).check(matches(isDisplayed()));
    }
}