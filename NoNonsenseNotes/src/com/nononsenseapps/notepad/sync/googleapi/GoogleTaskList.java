/*
 * Copyright (C) 2012 Jonas Kalderstam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.notepad.sync.googleapi;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import com.nononsenseapps.notepad.NotePad;
import com.nononsenseapps.notepad.database.RemoteTaskList;
import com.nononsenseapps.notepad.database.TaskList;
import com.nononsenseapps.util.BiMap;
import com.nononsenseapps.utils.time.RFC3339Date;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.RemoteException;
import com.nononsenseapps.helpers.Log;

public class GoogleTaskList extends RemoteTaskList {

	private static final String TAG = "nononsenseapps";
	public static final String SERVICENAME = "googletasks";
	//public String id = null;
	public String title = null;
	public boolean deleted = false;
	// public String selfLink = null;
	//public JSONObject json = null;
	//public String updated = null;

	//public boolean didRemoteInsert = false;
	//public int modified = 0;
	// Intended for when default list is deleted. When that fails, redownload it and its contents
	public boolean redownload = false;

	// private GoogleAPITalker api;

	public GoogleTaskList(final JSONObject jsonList, final String accountName) throws JSONException {
		super();
		this.service = SERVICENAME;
		// this.api = ;

		remoteId = jsonList.getString("id");
		title = jsonList.getString("title");
		account = accountName;
		
		try {
			updated = RFC3339Date.parseRFC3339Date(jsonList.getString("updated")).getTime();
		}
		catch (Exception e) {
			Log.d(TAG, e.getLocalizedMessage());
			updated = 0L;
		}

		//json = jsonList;
	}
	
	public GoogleTaskList(final TaskList dbList, final String accountName) {
		super();
		this.title = dbList.title;
		this.dbid = dbList._id;
		this.account = accountName;
		this.service = SERVICENAME;
	}

	public GoogleTaskList(final String accountName) {
		super();
		this.account = accountName;
		this.service = SERVICENAME;
	}
	
	public GoogleTaskList(final Cursor c) {
		super(c);
		this.service = SERVICENAME;
	}

//	public String toString() {
//		String res = "";
//		JSONObject json = new JSONObject();
//		try {
//			json.put("title", title);
//			json.put("id", remoteId);
//			// json.put("etag", etag);
//			json.put("dbid", dbId);
//			json.put("deleted", deleted);
//			json.put("updated", updated);
//
//			res = json.toString(2);
//		} catch (JSONException e) {
//			Log.d(TAG, e.getLocalizedMessage());
//		}
//		return res;
//	}

	public GoogleTaskList(final Long dbid, final String remoteId, final Long updated, final String account) {
		super(dbid, remoteId, updated, account);
		this.service = SERVICENAME;
	}

	/**
	 * Returns a JSON formatted version of this list. Includes title and not id
	 * 
	 * @return
	 * @throws JSONException
	 */
	public String toJSON() {
		JSONObject json = new JSONObject();
		try {
			json.put("title", title);

			// if (id != null)
			// json.put("id", id);

		} catch (JSONException e) {
			Log.d(TAG, e.getLocalizedMessage());
		}

		return json.toString();
	}

	/**
	 * Returns true if the TaskList has the same remote id or the same database
	 * id.
	 */
	@Override
	public boolean equals(Object o) {
		boolean equal = false;
		if (GoogleTaskList.class.isInstance(o)) {
			// It's a list!
			GoogleTaskList list = (GoogleTaskList) o;
			if (dbid != -1 && dbid == list.dbid) {
				equal = true;
			}
			if (remoteId != null && remoteId.equals(list.remoteId)) {
				equal = true;
			}
		}
		return equal;
	}
}
