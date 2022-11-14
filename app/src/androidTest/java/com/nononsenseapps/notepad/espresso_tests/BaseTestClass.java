package com.nononsenseapps.notepad.espresso_tests;

import android.content.Context;
import android.preference.PreferenceManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

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
	 * This will create and launch of the activity for you and also expose
	 * the activity under test. To get a reference to the activity you can use: <br/>
	 *
	 * {@code myActivityRule.getScenario().onActivity(a -> { ... }); }
	 */
	@Rule
	ActivityScenarioRule<ActivityMain_> myActivityRule
			= new ActivityScenarioRule<>(ActivityMain_.class);

	/**
	 * @return a string with the content of the given resourceId
	 */
	public String getStringResource(int resourceId) {
		final String[] val = new String[1];
		myActivityRule.getScenario().onActivity(a -> {
			val[0] = a.getString(resourceId);
		});
		return val[0];
	}

	@After
	public void tearDown() {
		Context context = ApplicationProvider.getApplicationContext();

		//clear the app's data as the test is finishing
		PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
		DatabaseHandler.resetDatabase(context);
	}

}
