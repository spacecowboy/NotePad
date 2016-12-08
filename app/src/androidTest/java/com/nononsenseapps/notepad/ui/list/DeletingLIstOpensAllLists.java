package com.nononsenseapps.notepad.ui.list;


import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.model.sql.TaskList;
import com.nononsenseapps.notepad.test.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItem;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class DeletingLIstOpensAllLists {

    @Rule
    public ActivityTestRule<ActivityList> mActivityTestRule = new ActivityTestRule<>(ActivityList.class);

    @Before
    public void setup() {
        mActivityTestRule.getActivity().getContentResolver().delete(TaskList.URI, null, null);
    }

    @After
    public void teardown() {
        mActivityTestRule.getActivity().getContentResolver().delete(TaskList.URI, null, null);
    }

    @Test
    public void deletingListOpensAllLists() {
        openDrawer();
        // Forces test to succeed in case drawer is already open
        openDrawer();

        createList("A");
        createTask("A");

        openDrawer();

        createList("B");
        createTask("B");

        openDrawer();

        createList("C");
        createTask("C");

        openDrawer();

        deleteList("C");

        closeDrawer();

        // Should now display list of all tasks
        ViewInteraction recyclerView = onView(allOf(withId(android.R.id.list), isDisplayed()));

        recyclerView.check(matches(Utils.assertionOnItemAtPosition(0, hasDescendant(withText("A")))));
        recyclerView.check(matches(Utils.assertionOnItemAtPosition(1, hasDescendant(withText("B")))));
    }

    private void deleteList(String title) {
        ViewInteraction appCompatTextView4 = onView(
                allOf(withId(android.R.id.text1), withText(title), isDisplayed()));
        appCompatTextView4.perform(longClick());

        ViewInteraction appCompatTextView5 = onView(
                allOf(withId(R.id.deleteButton), withText("Delete list"), isDisplayed()));
        appCompatTextView5.perform(click());

        ViewInteraction appCompatButton = onView(
                allOf(withId(android.R.id.button1), withText("OK"),
                        withParent(allOf(withId(R.id.buttonPanel),
                                withParent(withId(R.id.parentPanel)))),
                        isDisplayed()));
        appCompatButton.perform(click());
    }

    private void closeDrawer() {
        ViewInteraction recyclerView = onView(
                allOf(withId(R.id.navigation_drawer),
                        withParent(allOf(withId(R.id.drawer_layout),
                                withParent(withId(android.R.id.content)))),
                        isDisplayed()));
        recyclerView.perform(swipeLeft());
    }

    private void openDrawer() {
        try {
            ViewInteraction imageButton = onView(
                    allOf(withContentDescription("Open navigation drawer"),
                            withParent(allOf(withId(R.id.toolbar),
                                    withParent(withId(R.id.appbar)))),
                            isDisplayed()));
            imageButton.perform(click());
        } catch (NoMatchingViewException ignored) {
            // probably already open
        }
    }

    private void createTask(String title) {
        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.fab),
                        withParent(allOf(withId(R.id.main_content),
                                withParent(withId(R.id.drawer_layout)))),
                        isDisplayed()));
        floatingActionButton.perform(click());

        ViewInteraction styledEditText = onView(
                allOf(withId(R.id.taskText), isDisplayed()));
        styledEditText.perform(replaceText(title), closeSoftKeyboard());

        ViewInteraction imageButton2 = onView(
                allOf(withContentDescription("Navigate up"),
                        withParent(allOf(withId(R.id.toolbar),
                                withParent(withId(R.id.appbar)))),
                        isDisplayed()));
        imageButton2.perform(click());
    }

    private void createList(String title) {
        ViewInteraction recyclerView = onView(
                allOf(withId(R.id.navigation_drawer),
                        withParent(allOf(withId(R.id.drawer_layout),
                                withParent(withId(android.R.id.content)))),
                        isDisplayed()));
        recyclerView.perform(actionOnItem(hasDescendant(withText(R.string.menu_createnew)), click()));

        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.titleField), isDisplayed()));
        appCompatEditText.perform(click());

        ViewInteraction appCompatEditText2 = onView(
                allOf(withId(R.id.titleField), isDisplayed()));
        appCompatEditText2.perform(replaceText(title), closeSoftKeyboard());

        ViewInteraction appCompatTextView = onView(
                allOf(withId(R.id.dialog_yes), withText("OK"),
                        withParent(withId(R.id.buttons)),
                        isDisplayed()));
        appCompatTextView.perform(click());
    }
}
