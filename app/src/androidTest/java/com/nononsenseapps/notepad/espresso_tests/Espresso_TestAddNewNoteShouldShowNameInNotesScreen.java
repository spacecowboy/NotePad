package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import androidx.test.filters.LargeTest;

import org.junit.*;

@LargeTest
public class Espresso_TestAddNewNoteShouldShowNameInNotesScreen extends BaseTestClass{

    private String noteName1;


//    @Rule
//    public ActivityTestRule<ActivityList> myActivityRule =
//            new ActivityTestRule<ActivityList>(ActivityList.class);

    @Before
    public void initStrings(){
        noteName1 = "prepare food";
    }

//    @After
//    public void tearDown(){
//        Context context = myActivityRule.getActivity().getApplicationContext();
//
//        PreferenceManager.
//                getDefaultSharedPreferences(
//                        context).edit().clear().commit();
//
//        context.deleteDatabase(DatabaseHandler.DATABASE_NAME);
//
//        DatabaseHandler.getInstance(context).getReadableDatabase();
//    }

    @Test
    public void testAddNewNoteShouldShowNameInNotesScreen(){

        Helper.closeDrawer();

        Helper.createNoteWithName(noteName1);
        Helper.navigateUp();

        //onView(withText(noteName1)).check(matches(withText(noteName1)));
        onView(withText(noteName1)).check(matches(isDisplayed()));
    }

}
