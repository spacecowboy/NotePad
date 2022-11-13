package com.nononsenseapps.notepad.espresso_tests;

import android.content.Context;
import android.preference.PreferenceManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.rule.ActivityTestRule;

import com.nononsenseapps.notepad.ActivityMain_;
import com.nononsenseapps.notepad.database.DatabaseHandler;

import org.junit.After;
import org.junit.Rule;

public class BaseTestClass {

	/**
	 * A JUnit {@link Rule @Rule} to launch your activity under test. This is a replacement
	 * for ActivityInstrumentationTestCase2.
	 *
	 * Rules are interceptors which are executed for each test method and will run before
	 * any of your setup code in the @Before method.
	 *
	 * {@link ActivityTestRule} will create and launch of the activity for you and also expose
	 * the activity under test. To get a reference to the activity you can use
	 * the {@link ActivityTestRule#getActivity()} method.
	 *
	 * <br/>
	 * NOTE: the replacement, {@link androidx.test.ext.junit.rules.ActivityScenarioRule},
	 * is used as in {@link com.nononsenseapps.notepad.test.FragmentTaskDetailTest}
	 *
	 */
	@Rule
	// INFO: ActivityMain_ was renamed to ActivityList in release 6.0.0 beta,
	// it has to do with getting rid of the annotations library
	public ActivityTestRule<ActivityMain_> myActivityRule
			= new ActivityTestRule<>(ActivityMain_.class);

	@After
	public void tearDown() {
		Context context = ApplicationProvider.getApplicationContext();

		//clear the app's data as the test is finishing
		PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
		DatabaseHandler.resetDatabase(context);
	}

}
