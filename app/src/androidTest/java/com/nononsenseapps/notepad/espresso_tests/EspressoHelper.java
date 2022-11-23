package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.instanceOf;

import android.app.UiAutomation;
import android.os.SystemClock;

import androidx.test.espresso.AmbiguousViewMatcherException;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.platform.app.InstrumentationRegistry;

import com.getkeepsafe.taptargetview.TapTargetView;
import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.notepad.R;

import org.junit.Assert;

public class EspressoHelper {

	// TODO useless ? the drawer is already closed when this is called
	public static void closeDrawer() {
		// use the Espresso helper DrawerActions
		try {
			onView(withId(R.id.drawerLayout)).perform(DrawerActions.close());
		} catch (Exception ignored) {
			NnnLogger.error(EspressoHelper.class, "Can't close drawer");
		}
	}

	/**
	 * open the drawer on the left
	 */
	public static void openDrawer() {
		onView(withId(R.id.drawerLayout)).perform(DrawerActions.open());
	}

	public static void createNoteWithName(String noteName) {
		onView(withId(R.id.menu_add)).perform(click());
		EspressoHelper.hideShowCaseViewIfShown();
		onView(withId(com.nononsenseapps.notepad.R.id.taskText)).perform(typeText(noteName));
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

		onView(withId(R.id.drawer_menu_createlist)).perform(click());

		// fill the popup
		onView(withId(com.nononsenseapps.notepad.R.id.titleField)).perform(typeText(taskListName));
		onView(withId(com.nononsenseapps.notepad.R.id.dialog_yes)).perform(click());
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

		// if it is there, it's highlighting one of the views click somewhere to dismiss it
		try {
			onView(isRoot()).perform(click());
		} catch (Exception ignored) {
			Assert.fail("Could not dismiss the TapTargetView");
		}
	}

	/**
	 * Rotate the screen twice, waiting ~5 seconds for the animations to finish.
	 * It automatically understands if the phone or tablet is "naturally" held in
	 * landscape or portrait mode, so test should be done with the emulator's default
	 * settings: phones in portrait mode and tablets in landscape mode
	 */
	public static void rotateScreen() {
		var uiAuto = InstrumentationRegistry
				.getInstrumentation()
				.getUiAutomation();
		// rotate it
		uiAuto.setRotation(UiAutomation.ROTATION_FREEZE_270);
		// wait for the rotation to finish (.waitForIdleSync(); does not work, use this instead):
		SystemClock.sleep(1800);

		// rotating the screen sometimes makes the taptargetview appear in the wrong place.
		// I have no idea why. In any case, we have to close it now, or else the next
		// screen rotation will make the app crash. Yes it's incomprehensible, but now it works
		EspressoHelper.hideShowCaseViewIfShown();

		// rotate it more
		uiAuto.setRotation(UiAutomation.ROTATION_FREEZE_0);
		SystemClock.sleep(1800);

		// unfreeze it and let it go back to its default state
		uiAuto.setRotation(UiAutomation.ROTATION_UNFREEZE);
		SystemClock.sleep(1800);
	}

}
