package com.nononsenseapps.notepad.sync.googleapi;

public class GoogleTask {

	public String id = null;
	public String etag = null;
	public String title = null;
	public String updated = null;
	public String selfLink = null;
	public String parent = null;
	public String position = null;
	public String notes = null;
	public String status = null;
	public String due = null;
	public String completed = null;
	public boolean deleted = false;
	public boolean hidden = false;
	
	private GoogleAPITalker api;

	public GoogleTask(GoogleAPITalker api) {
		this.api = api;
	}
}
