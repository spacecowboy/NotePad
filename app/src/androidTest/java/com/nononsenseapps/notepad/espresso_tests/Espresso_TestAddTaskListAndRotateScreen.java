package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.filters.LargeTest;

import org.junit.*;

import static com.nononsenseapps.notepad.espresso_tests.OrientationChangeAction.*;

@LargeTest
public class Espresso_TestAddTaskListAndRotateScreen extends BaseTestClass{

    private String taskListName;

//    @Rule
//    public ActivityTestRule<ActivityList> myActivityRule =
//            new ActivityTestRule<ActivityList>(ActivityList.class);

    @Before
    public void initStrings(){
        taskListName = "a random task list";
    }

    @Test
    public void testAddTaskListAndRotateScreen(){

        Helper.createTaskList(taskListName);

        Helper.openDrawer();

        // rotate to landscape and back to portrait
        onView(isRoot()).perform(orientationLandscape());
        onView(isRoot()).perform(orientationPortrait());

        //make sure the task list is still visible

        RecyclerViewActions.scrollTo(hasDescendant(withText(taskListName)));
        onView(withText(taskListName)).check(matches(isDisplayed()));

    }
}
