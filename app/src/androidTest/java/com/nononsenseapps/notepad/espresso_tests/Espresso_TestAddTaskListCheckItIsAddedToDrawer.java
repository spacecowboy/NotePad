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
public class Espresso_TestAddTaskListCheckItIsAddedToDrawer extends BaseTestClass{


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
    public void testAddTaskListCheckItIsAddedToDrawer(){

        Helper.createTaskList(taskListName);
        Helper.openDrawer();

        //check that the new note is found and has the correct text
        onView(withText(taskListName)).check(matches(isDisplayed()));
    }




}

