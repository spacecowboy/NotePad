package com.nononsenseapps.notepad.test;

import com.nononsenseapps.notepad.legacydatabase.Task;
import com.nononsenseapps.notepad.legacydatabase.TaskList;

import android.content.Context;
import android.database.Cursor;

public class Helper {

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
}
