package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import androidx.test.filters.LargeTest;

import com.nononsenseapps.notepad.R;

import org.junit.*;

@LargeTest
public class Espresso_TestCreateNoteAndDeleteIt extends BaseTestClass{

    private String noteName1;

//    @Rule
//    public ActivityTestRule<ActivityList> myActivityRule =
//            new ActivityTestRule<ActivityList>(ActivityList.class);

    @Before
    public void initStrings(){
        noteName1 = "prepare food";
    }

    @Test
    public void testCreateNoteAndDeleteIt() {

        Helper.closeDrawer();

        Helper.createNoteWithName(noteName1);
        Helper.navigateUp();

//        Helper.scrollRecyclerViewToText(noteName1)
//                .check(matches(withText(noteName1)));


        onView(withText(noteName1)).perform(click());
        onView(withId(R.id.menu_delete)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());

        //assert that we're back in the list
        onView(withText("Notes")).check(matches(isDisplayed()));

        //check that the view is not visible
        onView(withText(noteName1)).check(doesNotExist());

    }




}
