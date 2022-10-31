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
public class Espresso_TestAddNewNoteWithReminderDateAndTime extends BaseTestClass{

    private String noteName1;

    @Before
    public void initStrings(){
        noteName1 = "prepare food";
    }

//    @Rule
//    public ActivityTestRule<ActivityList> myActivityRule =
//            new ActivityTestRule<ActivityList>(ActivityList.class);



    @Test
    public void testAddNewNoteWithReminderDateAndTime(){

        Helper.closeDrawer();
        Helper.createNoteWithName(noteName1);

        //add reminder
        onView(withId(com.nononsenseapps.notepad.R.id.notificationAdd)).perform(click());

        //add date
        onView(withId(com.nononsenseapps.notepad.R.id.notificationDate)).perform(click());
        onView(withId(com.nononsenseapps.notepad.R.id.done)).perform(click());

        //add time
        onView(withId(com.nononsenseapps.notepad.R.id.notificationTime)).perform(click());
        onView(withId(com.nononsenseapps.notepad.R.id.done_button)).perform(click());

        Helper.navigateUp();

        //check that the date field is visible
        onView(withText(noteName1)).perform(click());
        onView(withId(R.id.notificationDate)).check(matches(isDisplayed()));
        /*
        //check that current month is visible
        String currentMonth = Helper.getCurrentMonthName();

        //todo should we just make sure in here that the text field is not empty, not worry about correct date?
        onView(withText(currentMonth)).check(matches(isDisplayed()));
        */



    }
}
