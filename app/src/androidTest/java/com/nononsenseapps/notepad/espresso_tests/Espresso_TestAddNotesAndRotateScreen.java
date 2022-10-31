package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.nononsenseapps.notepad.espresso_tests.OrientationChangeAction.*;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import androidx.test.filters.LargeTest;

import org.junit.*;



@LargeTest
public class Espresso_TestAddNotesAndRotateScreen extends BaseTestClass{

    String noteName1, noteName2, noteName3, noteName4;

    @Before
    public void initStrings(){
        noteName1 = "prepare food";
        noteName2 = "take dogs out";
        noteName3 = "water plants";
        noteName4 = "sleep";
    }

//    @Rule
//    public ActivityTestRule<ActivityList> myActivityRule =
//            new ActivityTestRule<ActivityList>(ActivityList.class);


    @Test
    public void testAddNotesAndRotateScreen(){


        String[] noteNames = {noteName1, noteName2, noteName3, noteName4};

        Helper.closeDrawer();
        Helper.createNotes(noteNames);

        // rotate screen
        onView(isRoot()).perform(orientationLandscape());
        onView(isRoot()).perform(orientationPortrait());

        //check that textviews still show up
        onView(withText(noteNames[0])).check(matches(isDisplayed()));
        onView(withText(noteNames[1])).check(matches(isDisplayed()));
        onView(withText(noteNames[2])).check(matches(isDisplayed()));
        onView(withText(noteNames[3])).check(matches(isDisplayed()));

    }
}


