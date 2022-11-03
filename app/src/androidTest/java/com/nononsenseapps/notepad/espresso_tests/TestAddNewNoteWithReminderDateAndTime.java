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
public class TestAddNewNoteWithReminderDateAndTime extends BaseTestClass{

    private String noteName1;

    @Before
    public void initStrings(){
        noteName1 = "prepare food";
    }

    @Test
    public void testAddNewNoteWithReminderDateAndTime() {

        EspressoHelper.closeDrawer();
        EspressoHelper.hideShowCaseViewIfShown();

        EspressoHelper.createNoteWithName(noteName1);

        //add reminder
        onView(withId(com.nononsenseapps.notepad.R.id.notificationAdd)).perform(click());

        //add date
        onView(withId(com.nononsenseapps.notepad.R.id.notificationDate)).perform(click());
        onView(withId(com.nononsenseapps.notepad.R.id.done)).perform(click());

        //add time
        onView(withId(com.nononsenseapps.notepad.R.id.notificationTime)).perform(click());
        onView(withId(com.nononsenseapps.notepad.R.id.done_button)).perform(click());

        EspressoHelper.navigateUp();

        //check that the date field is visible
        onView(withText(noteName1)).perform(click());
        onView(withId(R.id.notificationDate)).check(matches(isDisplayed()));
    }
}
