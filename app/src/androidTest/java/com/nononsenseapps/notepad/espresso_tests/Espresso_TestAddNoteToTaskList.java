package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import androidx.test.filters.LargeTest;

import org.junit.*;

import static org.hamcrest.Matchers.*;

@LargeTest
public class Espresso_TestAddNoteToTaskList extends BaseTestClass{

    private String taskListName, noteName1;

//    @Rule
//    public ActivityTestRule<ActivityList> myActivityRule =
//            new ActivityTestRule<ActivityList>(ActivityList.class);

    @Before
    public void initStrings(){
        taskListName = "a random task list";
        noteName1 = "prepare food";
    }

    @Test
    public void testAddNoteToTaskList(){

        Helper.createTaskList(taskListName);

        //make sure the correct task list is opened
        Helper.openDrawer();
        onView(withText(taskListName)).perform(click());

        //add the note
        Helper.createNoteWithName(noteName1);
        Helper.navigateUp();

        //make sure that the number of notes for the task list is actually 1
        Helper.openDrawer();
        onView(allOf(withText(taskListName), hasSibling(withText("1")))).check(matches(withText(taskListName)));

    }
}
