package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import android.content.Context;
import android.preference.PreferenceManager;

import com.nononsenseapps.notepad.database.DatabaseHandler;

import org.junit.After;
import org.junit.Rule;

public class BaseTestClass {

    @Rule
    public ActivityTestRule<ActivityList> myActivityRule =
            new ActivityTestRule<ActivityList>(ActivityList.class);


    @After
    public void tearDown(){
        Context context = myActivityRule.getActivity().getApplicationContext();

        //clear the app's data as the test is finishing
        PreferenceManager.
                getDefaultSharedPreferences(
                        context).edit().clear().commit();
        DatabaseHandler.resetDatabase(context);
    }



}
