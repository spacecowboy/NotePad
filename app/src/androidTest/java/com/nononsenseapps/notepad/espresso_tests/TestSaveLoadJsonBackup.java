package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.nononsenseapps.notepad.R;

import org.junit.Test;

@LargeTest
public class TestSaveLoadJsonBackup extends BaseTestClass {

	final String noteText1 = "random note";
	final String noteText2 = "other random note";

	private static Instrumentation.ActivityResult createResponseIntent() {
		// TODO you're supposed to replicate the uri given by the filepicker.
		//  but i have NO idea how. See FilePickerHelper.java it may help

		// TODO see https://github.com/ViliusSutkus89/TestingStorageAccessFrameworkClients

		Uri uri1 = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3Agoogletest%2Ftest_outputfiles");
		var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		var docDir = DocumentFile.fromTreeUri(context, uri1);
		var uri2 = docDir.getUri();

		var odtr = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE, uri2)
				.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
				.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		return new Instrumentation.ActivityResult(Activity.RESULT_OK, odtr);
	}

	@Test
	public void testSaveLoadBackup() {
		EspressoHelper.hideShowCaseViewIfShown();

		// add 2 notes
		EspressoHelper.createNoteWithName(noteText1);
		EspressoHelper.createNoteWithName(noteText2);

		EspressoHelper.navigateUp();

		// make a backup
		openContextualActionModeOverflowMenu();
		String SETTINGS_TEXT = getStringResource(R.string.menu_preferences);
		onView(withText(SETTINGS_TEXT))
				.perform(scrollTo()) // in case the keyboard is covering the menu
				.perform(click());

		String SETTINGS_BACKUP_TEXT = getStringResource(R.string.backup);
		onView(withText(SETTINGS_BACKUP_TEXT)).perform(click());

		// choose a backup directory
		intending(hasAction(Intent.ACTION_OPEN_DOCUMENT_TREE))
				.respondWith(createResponseIntent());

		String CHOOSE_DIR = getStringResource(R.string.choose_backup_folder);
		onView(withText(CHOOSE_DIR)).perform();

		// then click export
		String EXPORT_BACKUP_TEXT = getStringResource(R.string.backup_export);
		onView(withText(EXPORT_BACKUP_TEXT)).perform(click());
		onView(withId(android.R.id.button1)).check(matches(isDisplayed()));
		onView(withId(android.R.id.button1))
				// .inRoot(isDialog())
				.perform(click());
		EspressoHelper.waitUi();

		// return to the notes list
		EspressoHelper.exitPrefsActivity();

		// check & delete both notes
		clickCheckBoxAt(0);
		clickCheckBoxAt(1);

		// clear completed notes
		openContextualActionModeOverflowMenu();
		String CLEAR_COMPLETED = getStringResource(R.string.menu_clearcompleted);
		onView(withText(CLEAR_COMPLETED)).perform(click());
		onView(withId(android.R.id.button1)).inRoot(isDialog()).check(matches(isDisplayed()));
		onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());

		// restore the backup
		openContextualActionModeOverflowMenu();
		onView(withText(SETTINGS_TEXT)).perform(click());
		onView(withText(SETTINGS_BACKUP_TEXT)).perform(click());

		String IMPORT_BACKUP_TEXT = getStringResource(R.string.backup_import);
		onView(withText(IMPORT_BACKUP_TEXT)).perform(click());
		EspressoHelper.onViewWithIdInDialog(android.R.id.button1).perform(click());
		EspressoHelper.waitUi();

		// return to the notes list
		EspressoHelper.exitPrefsActivity();

		// ensure both notes were restored
		onView(withText(noteText1)).check(matches(isDisplayed()));
		onView(withText(noteText2)).check(matches(isDisplayed()));
	}

	// TODO explore accessibility tests. See
	//  https://developer.android.com/training/testing/espresso/accessibility-checking

	// this one expects the list to have 2 children
	private void clickCheckBoxAt(int position) {
		var i = onData(anything())
				.inAdapterView(allOf(withId(android.R.id.list), hasChildCount(2)))
				.atPosition(position)
				.onChildView(withId(R.id.checkbox));
		i.perform(click());
	}

}
