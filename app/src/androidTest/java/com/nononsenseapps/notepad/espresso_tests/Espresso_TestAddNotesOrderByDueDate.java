package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import androidx.test.filters.LargeTest;

import com.nononsenseapps.notepad.R;

import org.junit.*;

import static org.hamcrest.Matchers.endsWith;

@LargeTest
public class Espresso_TestAddNotesOrderByDueDate extends BaseTestClass{

    private String noteName1;
    private String noteName2;
    private String noteName3;
    private String noteName4;
    private String day04, day05, day15, day23;

//    @Rule
//    public ActivityTestRule<ActivityList> myActivityRule =
//            new ActivityTestRule<ActivityList>(ActivityList.class);

    @Before
    public void initStrings(){
        noteName1 = "prepare food";
        noteName2 = "take dogs out";
        noteName3 = "water plants";
        noteName4 = "sleep";

        String currentMonthAndYear = Helper.getMonthAndYear();

        day04 = "04 " + currentMonthAndYear;
        day05 = "05 " + currentMonthAndYear;
        day15 = "15 " + currentMonthAndYear;
        day23 = "23 " + currentMonthAndYear;
    }



    @Test
    public void testAddNotesOrderByDueDate(){


        Helper.closeDrawer();

        Helper.createNoteWithName(noteName1);

//        onView(withText("Due date")).perform(click());
        onView(withId(R.id.dueDateBox)).perform(click());

        onData(withContentDescription(day04))
                .inAdapterView(withClassName(endsWith("SimpleDayPickerView")))
                .perform(click());

//        onData(allOf(
////                withClassName(start"SimpleDayPickerView"),
////                isCompletelyDisplayed(),
////                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
////                isDisplayed(),
//                withContentDescription(day04)))
//                .perform(click());



//        onData(withContentDescription(day04)).perform(click());
//        onView(withContentDescription(day04)).perform(click());


    }
}
