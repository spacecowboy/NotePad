package com.nononsenseapps.notepad.sync.googleapi;

import java.util.ArrayList;

public class GoogleTaskList {

	public String id = null;
	public String etag = null;
	public String title = null;
	public String selfLink = null;
	
	private GoogleAPITalker api;

	public GoogleTaskList(GoogleAPITalker api) {
		this.api = api;
	}

	/*
	 * Tasks
	 */
	public ArrayList<GoogleTask> getTasks() {
		return null;
	}

	public GoogleTask getTask() {
		return null;
	}

	public GoogleTask insertTask(GoogleTask task) {
		return null;
	}

	public GoogleTask updateTask(GoogleTask task) {
		return null;
	}

	public void deleteTask(GoogleTask task) {

	}

	public void move(GoogleTask task, GoogleTask newParent) {

	}
}
