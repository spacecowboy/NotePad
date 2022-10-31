package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import androidx.test.filters.LargeTest;

import com.nononsenseapps.notepad.R;

import org.junit.Before;
import org.junit.Test;

@LargeTest
public class Espresso_FaultyTests extends com.nononsenseapps.notepad.espresso_tests.BaseTestClass {

    private String noteName1;
    private String createNewText;


//    @Rule
//    public ActivityTestRule<ActivityList> myActivityRule =
//            new ActivityTestRule<ActivityList>(ActivityList.class);

    @Before
    public void initStrings(){
        noteName1 = "prepare food";
        createNewText = myActivityRule.getActivity().getResources().getString(R.string.menu_createnew);
    }



    @Test
    public void testAddNewNoteSearchForFaultyNoteName(){

        Helper.closeDrawer();

        Helper.createNoteWithName(noteName1);
        Helper.navigateUp();

        onView(withText(noteName1)).check(matches(withText(noteName1 + "asdf")));
        assertFalse("should have failed before this", true);
    }

    @Test
    public void testSearchForElementWithTextShouldFailOnView(){
        Helper.closeDrawer();
        onView(withText(createNewText)).perform(click());
        assertFalse("should have failed before this", true);
    }

    @Test
    public void testSearchForElementWithIDShouldFailOnView(){
        Helper.closeDrawer();
        onView(withId(R.id.fab)).perform(click());
        onView(withId(R.id.fab)).perform(click());
        assertFalse("should have failed before this", true);
    }

    @Test
    public void testSearchForElementWithFaultyID(){
//        onView(withId(R.id.faulty_id)).perform(click());
        fail();
    }

    @Test
    public void testSearchForElementWithAmbiguousIdentifier(){
        Helper.closeDrawer();

        Helper.createNoteWithName(noteName1);
        Helper.navigateUp();
        Helper.createNoteWithName(noteName1);
        Helper.navigateUp();

        onView(withText(noteName1)).perform(click());
        assertFalse("should have failed before this", true);
    }

}
