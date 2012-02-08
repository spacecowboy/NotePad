package com.nononsenseapps.notepad.sync.googleapi;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

public class GoogleTaskList {

	public String id = null;
	public String etag = null;
	public String title = null;
	//public String selfLink = null;
	public JSONObject json = null;
	
	//private GoogleAPITalker api;

	public GoogleTaskList(JSONObject jsonList) throws JSONException {
		//this.api = ;
		
		id = jsonList.getString("id");
		etag = jsonList.getString("etag");
		title = jsonList.getString("title");
		
		json = jsonList;
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
