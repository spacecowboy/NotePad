package com.nononsenseapps.notepad.sync.googleapi;

import java.util.ArrayList;

public class GoogleTasks {

	private GoogleAPITalker api;

	public GoogleTasks(GoogleAPITalker api) {
		this.api = api;
	}
	
	/*
	 * Lists
	 */
	public ArrayList<GoogleTaskList> getLists() {
		return null;
	}
	
	public GoogleTaskList getList() {
		return null;
	}
	
	public void deleteList(GoogleTaskList list) {
		
	}
	
	public GoogleTaskList insertList(GoogleTaskList list) {
		return null;
	}
	
	public GoogleTaskList updateList(GoogleTaskList list) {
		return null;
	}
	
	public void clearCompletedTasks(GoogleTaskList list) {
		
	}
}
