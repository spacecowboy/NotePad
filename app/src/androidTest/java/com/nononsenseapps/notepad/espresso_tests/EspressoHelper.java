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

import androidx.test.espresso.AmbiguousViewMatcherException;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.platform.app.InstrumentationRegistry;

import com.getkeepsafe.taptargetview.TapTargetView;
import com.nononsenseapps.notepad.R;

import org.junit.Assert;


public class EspressoHelper {

	public static void closeDrawer() {
		//use the Espresso helper DrawerActions
		onView(withId(R.id.drawerLayout)).perform(DrawerActions.close());
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
		if (isInTabletMode()) {
			// we are in tablet mode: press "+" to make a note appear in the list
			onView(withId(R.id.menu_add)).perform(click());
		} else {
			// we are in phone mode: close the keyboard & press the back button
			onView(isRoot()).perform(closeSoftKeyboard());
			Espresso.pressBack();
		}
	}

	/**
	 * @return TRUE if the {@link TapTargetView} is currently shown, FALSE otherwise
	 */
	private static Boolean isShowCaseOverlayVisible() {
		try {
			onView(instanceOf(TapTargetView.class))
					.check(matches(isDisplayed()));
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

}
