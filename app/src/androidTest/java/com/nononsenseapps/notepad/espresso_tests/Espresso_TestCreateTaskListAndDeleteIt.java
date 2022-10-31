package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import androidx.test.filters.LargeTest;

import com.nononsenseapps.notepad.R;

import org.junit.*;

@LargeTest
public class Espresso_TestCreateTaskListAndDeleteIt extends BaseTestClass{

    private String taskListName;

//    @Rule
//    public ActivityTestRule<ActivityList> myActivityRule =
//            new ActivityTestRule<ActivityList>(ActivityList.class);

    @Before
    public void initStrings(){
        taskListName = "a random task list";

    }

    @Test
    public void testCreateTaskListAndDeleteIt(){

        Helper.createTaskList(taskListName);

        Helper.openDrawer();

        onView(withText(taskListName))
                .perform(longClick());

        onView(withId(R.id.deleteButton))
                .perform(click());

        onView(withText("OK"))
                .perform(click());

        onView(withText(taskListName))
                .check(doesNotExist());
    }

}
