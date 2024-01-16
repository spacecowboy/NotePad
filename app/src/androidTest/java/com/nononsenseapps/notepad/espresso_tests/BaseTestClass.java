package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.notepad.activities.main.ActivityMain_;
import com.nononsenseapps.notepad.database.DatabaseHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

public class BaseTestClass {

	/**
	 * A JUnit {@link Rule @Rule} to launch your activity under test. This replaces
	 * ActivityInstrumentationTestCase2. Rules are executed for each test method and will run
	 * before any of your setup code in the @Before method. To get a reference to the activity
	 * you can use: {@link IntentsTestRule#getActivity()}
	 * <br/>
	 * NOTE: the newer alternative, {@link ActivityScenarioRule}, <b>DOES NOT WORK</b>
	 */
	@SuppressWarnings("deprecation")
	@Rule
	public IntentsTestRule<ActivityMain_> mActRule;


	/**
	 * Since API 33 we need permission for notifications
	 */
	@Rule
	public GrantPermissionRule mNotifRule;

	/**
	 * @return a string with the content of the given resourceId
	 */
	public String getStringResource(int resourceId) {
		return mActRule.getActivity().getString(resourceId);
	}

	@After
	public void clearAppData() {
		if (mActRule != null) mActRule.finishActivity();
		Context context = ApplicationProvider.getApplicationContext();

		// clear the app's data as the test is starting & finishing
		PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
		DatabaseHandler.resetDatabase(context);
	}

	/**
	 * Tries in many ways to give notification permission for OS versions that need it
	 */
	private void giveNotifyPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			// this permission works only on API >= 33, it crashes on older versions!
			mNotifRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS);

			String command = "pm grant " +
					ApplicationProvider.getApplicationContext().getPackageName() + " " +
					Manifest.permission.POST_NOTIFICATIONS;

			// this one is more likely to work
			InstrumentationRegistry
					.getInstrumentation()
					.getUiAutomation()
					.executeShellCommand(command);
		} else {
			mNotifRule = null;
		}
	}

	/**
	 * Many times, on the github VM, the tests fail with RootViewWithoutFocusException,
	 * I think it's due to the emulator being slow. Let's launch the activity and wait
	 * for it to load before starting the real test
	 */
	@Before
	public void launchAndWait() {
		// ensure that this is called BEFORE trying to start the activity
		clearAppData();

		// first, acquire all the required permissions ...
		giveNotifyPermission();

		// ... then, create and run the entry point to the app
		mActRule = new IntentsTestRule<>(ActivityMain_.class);
		Intent launchApp = new Intent(ApplicationProvider.getApplicationContext(), ActivityMain_.class);
		mActRule.launchActivity(launchApp);

		try {
			// it responds => we can return now
			onView(isRoot()).check(matches(isDisplayed()));
			return;
		} catch (Exception e) {
			NnnLogger.error(this.getClass(), "Activity isn't responsive:");
			NnnLogger.exception(e);
		}

		// maybe we just have to wait
		EspressoHelper.waitUi();

		// if it's still not enough, let's crash here
		try {
			onView(isRoot()).check(matches(isDisplayed()));
		} catch (Exception e) {
			NnnLogger.error(this.getClass(), "Can't launch activity:");
			NnnLogger.exception(e);
			throw e;
		}
	}
}
