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
public class Espresso_TestAddTaskListsScrollNavigationDrawer extends BaseTestClass{

    String[] taskListNames = {"Lorem", "ipsum ", "dolor ", "sit ", "amet", "consectetur ",
            "adipiscing ", "elit", "sed ", "do ", "eiusmod ", "tempor ", "incididunt ",
            "ut ", "labore "};

    String SETTINGS_TEXT, SETTINGS_APPEARANCE_TEXT;

//    @Rule
//    public ActivityTestRule<ActivityList> myActivityRule =
//            new ActivityTestRule<ActivityList>(ActivityList.class);

    @Before
    public void initStrings(){
        SETTINGS_TEXT =
                myActivityRule.getActivity().getString(R.string.menu_preferences);
        SETTINGS_APPEARANCE_TEXT =
                myActivityRule.getActivity().getString(R.string.settings_cat_appearance);
    }

    @Test
    public void testAddTaskListsScrollNavigationDrawer(){

        for(String name : taskListNames){
            Helper.createTaskList(name);
            Helper.openDrawer();
        }

        onView(withId(R.id.navigation_drawer)).perform(RecyclerViewActions.actionOnItem(
                hasDescendant(withText(SETTINGS_TEXT)), click()
        ));

        onView(withText(SETTINGS_APPEARANCE_TEXT)).check(matches(isDisplayed()));
    }
}
