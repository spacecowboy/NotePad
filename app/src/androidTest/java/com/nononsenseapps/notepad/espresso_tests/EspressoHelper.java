package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;

import android.app.UiAutomation;
import android.os.SystemClock;

import androidx.annotation.IdRes;
import androidx.test.espresso.AmbiguousViewMatcherException;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.platform.app.InstrumentationRegistry;

import com.getkeepsafe.taptargetview.TapTargetView;
import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.notepad.ActivityMain;
import com.nononsenseapps.notepad.R;

import org.junit.Assert;

public class EspressoHelper {

	// TODO useless ? the drawer is already closed when this is called
	public static void closeDrawer() {
		if (true) return;
		// use the Espresso helper DrawerActions
		try {
			onView(withId(R.id.drawerLayout)).perform(DrawerActions.close());
		} catch (Exception ignored) {
			NnnLogger.error(EspressoHelper.class, "Can't close drawer");
		}
	}

	/**
	 * Wait for 500ms to work around timing issues on slow emulators. Every time
	 * this function is called was to solve issues with flaky tests on github runners.
	 * Keep it: sometime tests need it, sometimes they don't, but you can't know.
	 * Ideally, it should go after every call to {@link ViewInteraction#perform}
	 */
	public static void waitUi() {
		InstrumentationRegistry.getInstrumentation().waitForIdleSync();
		SystemClock.sleep(500);
	}

	/**
	 * open the drawer on the left
	 */
	public static void openDrawer() {
		try {
			onView(withId(R.id.drawerLayout)).check(matches(isDisplayed()));
		} catch (Exception e) {
			Assert.fail("Can't find the drawerLayout, maybe a dialog is still open?");
			return;
		}
		onView(withId(R.id.drawerLayout)).perform(DrawerActions.open());
	}

	public static void createNoteWithName(String noteName) {
		onView(withId(R.id.menu_add))
				.check(matches(isDisplayed()))
				.check(matches(isClickable()));
		onView(withId(R.id.menu_add)).perform(click());
		EspressoHelper.hideShowCaseViewIfShown();
		onView(withId(R.id.taskText)).perform(typeText(noteName));
	}

	/**
	 * Presses "+" and writes text for each note given in noteNames
	 */
	public static void createNotes(String[] noteNames) {
		for (String noteName : noteNames) {
			createNoteWithName(noteName);
		}
	}

	/**
	 * Add a new task list. The drawer should be open when this is called
	 *
	 * @param taskListName name of the task list
	 */
	public static void createTaskList(String taskListName) {
		EspressoHelper.openDrawer();

		// dismiss the other showcase view
		EspressoHelper.hideShowCaseViewIfShown();

		onView(withId(R.id.drawer_menu_createlist)).check(matches(isDisplayed()));
		onView(withId(R.id.drawer_menu_createlist)).perform(click());
		waitUi(); // the popup may need time to load
		try {
			// did the popup load already ?
			onView(withId(R.id.titleField)).check(matches(isDisplayed()));
		} catch (Exception ex) {
			// maybe the 1° click didn't work, retry
			onView(withId(R.id.drawer_menu_createlist)).perform(click());
		}

		// TODO regularly crashes here in the google_apis - API23 emulator image
		onViewWithIdInDialog(R.id.titleField).check(matches(isDisplayed()));

		// fill the popup
		onViewWithIdInDialog(R.id.titleField).perform(typeText(taskListName));
		onViewWithIdInDialog(R.id.dialog_yes).check(matches(isDisplayed()));
		onViewWithIdInDialog(R.id.dialog_yes).perform(click());
		try {
			// check if the dialog is still visible (it shouldn't be)
			onViewWithIdInDialog(R.id.dialog_yes).check(matches(not(isDisplayed())));
		} catch (Exception ex) {
			NnnLogger.exception(ex);
		}
	}

	/**
	 * shorthand for onView(withId(viewId)).inRoot(isDialog())
	 *
	 * @param viewId it's R.id.something
	 */
	public static ViewInteraction onViewWithIdInDialog(@IdRes int viewId) {
		return onView(withId(viewId)).inRoot(isDialog());
	}

	/**
	 * @return TRUE if the app is in tablet mode, FALSE in phone mode
	 */
	public static boolean isInTabletMode() {
		boolean isInPortraitMode = InstrumentationRegistry
				.getInstrumentation()
				.getTargetContext()
				.getResources()
				.getBoolean(R.bool.fillEditor);
		return !isInPortraitMode;
	}

	public static void navigateUp() {
		onView(isRoot()).perform(closeSoftKeyboard());
		if (isInTabletMode()) {
			// we are in tablet mode: press "+" to make a note appear in the list
			onView(withId(R.id.menu_add)).perform(click());
		} else {
			// we are in phone mode: close the keyboard & press the back button
			Espresso.pressBack();
		}
	}

	/**
	 * Exits the "settings" activity, going back to {@link ActivityMain}
	 */
	public static void exitPrefsActivity() {

		String label = InstrumentationRegistry
				.getInstrumentation()
				.getTargetContext()
				.getString(R.string.menu_preferences);
		try {
			// TODO improve & return if necessary
			onView(withText(label)).check(matches(isDisplayed()));
		} catch (Exception e) {
			NnnLogger.warning(EspressoHelper.class, "Can't determine if PrefsActivity is shown:");
			NnnLogger.exception(e);
		}

		// for now, assume this function is called only when a fragment of PrefsActivity is shown

		if (isInTabletMode()) {
			// tablets show menu & category => press back only once
			Espresso.pressBack();
		} else {
			// in phones, go back to the menu, then back to ActivityMain
			Espresso.pressBack();
			Espresso.pressBack();
		}
	}

	/**
	 * @return TRUE if the {@link TapTargetView} is currently shown, FALSE otherwise
	 */
	private static Boolean isShowCaseOverlayVisible() {
		try {
			onView(instanceOf(TapTargetView.class)).check(matches(isDisplayed()));
			return true;
		} catch (AmbiguousViewMatcherException ignored) {
			// we have at least one, so it counts as visible
			return true;
		} catch (Throwable ignored) {
			// it has to be "Throwable", not "Exception"
			return false;
		}
	}

	/**
	 * If the {@link TapTargetView} is shown, touch the screen to hide it, so that tests
	 * can then interact with the app.
	 */
	public static void hideShowCaseViewIfShown() {
		if (!EspressoHelper.isShowCaseOverlayVisible()) return;

		// click anywhere to dismiss it
		try {
			onView(instanceOf(TapTargetView.class)).perform(click());
		} catch (Exception ignored) {
			Assert.fail("Could not dismiss the TapTargetView");
			return;
		}
		waitUi();
	}

	/**
	 * Rotate the screen twice, waiting ~4 seconds for the animations to finish.
	 * It automatically understands if the phone or tablet is "naturally" held in
	 * landscape or portrait mode, so test should be done with the emulator's default
	 * settings: phones in portrait mode and tablets in landscape mode
	 */
	public static void rotateScreenAndWait() {
		var uiAuto = InstrumentationRegistry
				.getInstrumentation()
				.getUiAutomation();
		// rotate it
		uiAuto.setRotation(UiAutomation.ROTATION_FREEZE_270);
		// wait 1s for the rotation to finish
		waitUi();
		waitUi();

		// rotating the screen sometimes makes the taptargetview appear in the wrong place.
		// I have no idea why. In any case, we have to close it now, or else the next
		// screen rotation will make the app crash. Yes it's incomprehensible, but now it works
		EspressoHelper.hideShowCaseViewIfShown();

		// rotate it more
		uiAuto.setRotation(UiAutomation.ROTATION_FREEZE_0);
		waitUi();
		waitUi();

		// unfreeze it and let it go back to its default state
		uiAuto.setRotation(UiAutomation.ROTATION_UNFREEZE);
		waitUi();
		waitUi();
		waitUi();
	}

}
