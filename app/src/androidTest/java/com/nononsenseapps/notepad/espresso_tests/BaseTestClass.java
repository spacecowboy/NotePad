package com.nononsenseapps.notepad.espresso_tests;

import android.content.Context;
import android.preference.PreferenceManager;

import androidx.test.rule.ActivityTestRule;

import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.database.DatabaseHandler;

import org.junit.After;
import org.junit.Rule;

public class BaseTestClass {

    // INFO: ActivityMain_ was renamed to ActivityList in release 6.0.0 beta,
    // it has to do with getting rid of the annotations library
    @Rule
    public ActivityTestRule<ActivityMain_> myActivityRule
            = new ActivityTestRule<>(ActivityMain_.class);

    @After
    public void tearDown(){
        Context context = myActivityRule.getActivity().getApplicationContext();

        //clear the app's data as the test is finishing
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        DatabaseHandler.resetDatabase(context);
    }

}
