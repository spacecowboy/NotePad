package com.nononsenseapps.notepad.espresso_tests;

import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.notepad.R;


import android.content.Context;

import android.database.Cursor;
import androidx.annotation.NonNull;

import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.RecyclerViewActions;

import java.text.DateFormat;

import java.util.Date;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;


public class Helper {

	public static String NAVIGATE_UP_TEXT = "Navigate up";

	public static Task getATask(final Context context) {
		Cursor c = context.getContentResolver().query(Task.URI, Task.Columns.FIELDS, null, null, null);	
		Task result =  null;
		if (c.moveToFirst())
			result = new Task(c);
		return result;
	}
	
	public static TaskList getATaskList(final Context context) {
		Cursor c = context.getContentResolver().query(TaskList.URI, TaskList.Columns.FIELDS, null, null, null);	
		TaskList result =  null;
		if (c.moveToFirst())
			result = new TaskList(c);
		return result;
	}

	public static void closeDrawer() {
		//use the Espresso helper DrawerActions
		onView(withId(com.nononsenseapps.notepad.R.id.drawer_layout)).perform(DrawerActions.close());
	}

	public static void openDrawer(){
		//use the Espresso helper DrawerActions
		onView(withId(com.nononsenseapps.notepad.R.id.drawer_layout)).perform(DrawerActions.open());
	}

	public static void createNoteWithName(String noteName) {
		onView(withId(com.nononsenseapps.notepad.R.id.fab)).perform(click());
		onView(withId(com.nononsenseapps.notepad.R.id.taskText)).perform(typeText(noteName));
	}

	public static void createNotes(String[] noteNames){
		for (int i = 0; i < noteNames.length; i++){
			createNoteWithName(noteNames[i]);
			navigateUp();
		}
	}

	/**
	 * Add a new task list. The drawer should be open when this is called
	 * @param taskListName name of the task list
     */
	public static void createTaskList(String taskListName){
		onView(withId(R.id.navigation_drawer)).perform(RecyclerViewActions.actionOnItem(
				hasDescendant(withText("Create new")), click()
		));

		onView(withId(com.nononsenseapps.notepad.R.id.titleField)).perform(typeText(taskListName));
		onView(withId(com.nononsenseapps.notepad.R.id.dialog_yes)).perform(click());
	}

	public static void navigateUp(){
		onView(withContentDescription(Helper.NAVIGATE_UP_TEXT)).perform(click());
	}

	public static String getMonthAndYear(){

		String date = DateFormat.getDateInstance(DateFormat.LONG).format(new Date());
		//June 27, 2016
		String month = date.substring(0, date.indexOf(" "));
//        String day = date.substring(date.indexOf(" ")+1, date.indexOf(","));

		//not the neatest way to do this, but should work until 2100 period
		String year =  date.substring(date.indexOf("20"), date.indexOf("20")+4);


		return month + " " + year;
		//16 June 2016
	}

	@NonNull
	public static String getCurrentMonthName() {
		String date = DateFormat.getDateInstance(DateFormat.LONG).format(new Date());

		int firstSpaceIndex = date.indexOf(" ");
		String dateReturned = date.substring(0, firstSpaceIndex);

		dateReturned += date.substring(firstSpaceIndex, date.lastIndexOf(" "));

		//trim the trailing , from the string
		dateReturned = dateReturned.substring(0, dateReturned.length()-1);

		return dateReturned;
	}







}
