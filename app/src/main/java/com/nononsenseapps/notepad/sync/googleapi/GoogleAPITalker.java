/*
 * Copyright (C) 2012 Jonas Kalderstam
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nononsenseapps.notepad.sync.googleapi;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.http.AndroidHttpClient;

import com.nononsenseapps.build.Config;
import com.nononsenseapps.helpers.Log;
import com.nononsenseapps.utils.time.RFC3339Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Random;

/**
 * Helper class that sorts out all XML, JSON, HTTP bullshit for other classes.
 * Also keeps track of APIKEY and AuthToken
 * 
 * @author Jonas
 * 
 */
public class GoogleAPITalker {

    /*private final Context context;

    *//**
     *
     * @param context Need context to load api key from file
     *//*
    public GoogleAPITalker(final Context context) {
        this.context = context;
    }

    public static class PreconditionException extends Exception {
		private static final long serialVersionUID = 7317567246857384353L;

		public PreconditionException() {
		}

		public PreconditionException(String detailMessage) {
			super(detailMessage);
		}

		public PreconditionException(Throwable throwable) {
			super(throwable);
		}

		public PreconditionException(String detailMessage, Throwable throwable) {
			super(detailMessage, throwable);
		}

	}

	public static class NotModifiedException extends Exception {
		private static final long serialVersionUID = -6736829980184373286L;

		public NotModifiedException() {
		}

		public NotModifiedException(String detailMessage) {
			super(detailMessage);
		}

		public NotModifiedException(Throwable throwable) {
			super(throwable);
		}

		public NotModifiedException(String detailMessage, Throwable throwable) {
			super(detailMessage, throwable);
		}

	}

	public static Random rand = new Random();

	private static final String NEXTPAGETOKEN = "nextPageToken";

	private String AuthUrlEnd() {
		return "key=" + Config.getGtasksApiKey(context);
	}

	// public static final String AUTH_URL_END = "key=" + APIKEY;

	private static final String BASE_URL = "https://www.googleapis.com/tasks/v1/users/@me/lists";

	private String AllLists(final String pageToken) {
		String result = BASE_URL + "?";

		if (pageToken != null && !pageToken.isEmpty()) {
			result += "pageToken=" + pageToken + "&";
		}

		result += AuthUrlEnd();
		return result;
	}

	private String InsertLists() {
		return BASE_URL + "?" + AuthUrlEnd();
	}

	// public static final String ALL_LISTS = BASE_URL + "?" + AUTH_URL_END;

	private String AllListsJustEtag() {
		return BASE_URL + "?fields=etag&" + AuthUrlEnd();
	}

	// public static final String ALL_LISTS_JUST_ETAG = BASE_URL +
	// "?fields=etag&"+ AUTH_URL_END;

	private String ListURL(String id) {
		return BASE_URL + "/" + id + "?" + AuthUrlEnd();
	}

	public static final String LISTS = "/lists";
	private static final String BASE_TASK_URL = "https://www.googleapis.com/tasks/v1/lists";
	private static final String TASKS = "/tasks"; // Must be preceeded by

	// only retrieve the fields we will save in the database or use
	// https://www.googleapis.com/tasks/v1/lists/MDIwMzMwNjA0MjM5MzQ4MzIzMjU6MDow/tasks?showDeleted=true&showHidden=true&pp=1&key={YOUR_API_KEY}
	// updatedMin=2012-02-07T14%3A59%3A05.000Z
	private String AllTasksInsert(String listId) {
		return BASE_TASK_URL + "/" + listId + TASKS + "?" + AuthUrlEnd();
	}

	private String TaskURL(String taskId, String listId) {
		return BASE_TASK_URL + "/" + listId + TASKS + "/" + taskId + "?"
				+ AuthUrlEnd();
	}

	private String TaskURL_ETAG_ID_UPDATED(final String taskId,
			final String listId) {
		String url = BASE_TASK_URL + "/" + listId + TASKS + "/" + taskId
				+ "?fields=id,etag,updated";
		url += ",position,parent&" + AuthUrlEnd();
		return url;
	}

	private String TaskMoveURL_ETAG_UPDATED(final String taskId,
			final String listId, final String remoteparent,
			final String remoteprevious) {
		String url = BASE_TASK_URL + "/" + listId + TASKS + "/" + taskId
				+ "/move?";
		if (remoteparent != null && !remoteparent.isEmpty())
			url += "parent=" + remoteparent + "&";
		if (remoteprevious != null && !remoteprevious.isEmpty())
			url += "previous=" + remoteprevious + "&";
		url += "fields=etag,updated,position,parent&" + AuthUrlEnd();
		return url;
	}

	*//**
	 * Set the pageToken to null to get the first page
	 *//*
	private String allTasksUpdatedMin(final String listId,
			final String timestamp, final String pageToken) {
		// items,nextPageToken
		String request = BASE_TASK_URL
				+ "/"
				+ listId
				+ TASKS
				+ "?showDeleted=true&showHidden=true&fields=items%2CnextPageToken&";

		// Comes into play if user has Many tasks
		if (pageToken != null && !pageToken.isEmpty()) {
			request += "pageToken=" + pageToken + "&";
		}

		if (timestamp != null && !timestamp.isEmpty()) {
			try {
				request += "updatedMin="
						+ URLEncoder.encode(timestamp, "UTF-8") + "&";
			}
			catch (UnsupportedEncodingException e) {
				// Is OK. Can request full sync.
				Log.d(TAG, "Malformed timestamp: " + e.getLocalizedMessage());
			}
		}

		request += AuthUrlEnd();
		return request;
	}

	// Tasks URL which inludes deleted tasks: /tasks?showDeleted=true
	// Also do showHidden=true?
	// Tasks returnerd will have deleted = true or no deleted field at all. Same
	// case for hidden.
	private static final String TAG = "nononsenseapps GoogleAPITalker";

	private static final String USERAGENT = "HoloNotes (gzip)";

	// A URL is alwasy constructed as: BASE_URL + ["/" + LISTID [+ TASKS [+ "/"
	// + TASKID]]] + "?" + [POSSIBLE FIELDS + "&"] + AUTH_URL_END
	// Where each enclosing parenthesis is optional

	private String authToken;
	
	public String accountName = null;

	private static String getAuthToken(AccountManager accountManager,
                                       Account account, String authTokenType, boolean notifyAuthFailure) {

		Log.d(TAG, "getAuthToken");
		String authToken = "";
		try {
			// Might be invalid in the cache
			authToken = accountManager.blockingGetAuthToken(account,
					authTokenType, notifyAuthFailure);
			accountManager.invalidateAuthToken("com.google", authToken);

			authToken = accountManager.blockingGetAuthToken(account,
					authTokenType, notifyAuthFailure);
		}
		catch (OperationCanceledException ignored) {
		}
		catch (AuthenticatorException ignored) {
		}
		catch (IOException ignored) {
		}
		return authToken;
	}

	public boolean initialize(AccountManager accountManager, Account account,
			String authTokenType, boolean notifyAuthFailure) {

		Log.d(TAG, "initialize");
		accountName = account.name;
		authToken = getAuthToken(accountManager, account, authTokenType,
				notifyAuthFailure);

		Log.d(TAG, "authToken: " + authToken);
        if ( authToken != null && !authToken.isEmpty()) {

        } else {

        }
	}

	public void closeClient() {
		if (client != null) {
			client.close();
		}
	}

	*//**
	 * The entries in this does only one net-call, and such the list items do
	 * not contain e-tags. useful to get an id-list.
	 * 
	 * E-tag is an amalgam of etags in all pages if user has more than 100
	 * lists.
	 * 
	 * @return
	 * @throws IOException
	 * @throws NotModifiedException
	 * @throws PreconditionException
	 * @throws ClientProtocolException
	 * @throws JSONException 
	 *//*
	public String getListOfLists(ArrayList<GoogleTaskList> list)
			throws IOException, JSONException {
		String eTag = "";
		String pageToken = null;
		do {
			HttpGet httpget = new HttpGet(AllLists(pageToken));
			httpget.setHeader("Authorization", "OAuth " + authToken);

			// Log.d(TAG, "request: " + AllLists());
			AndroidHttpClient.modifyRequestToAcceptGzipResponse(httpget);

			try {
				JSONObject jsonResponse = (JSONObject) new JSONTokener(
						parseResponse(client.execute(httpget))).nextValue();

				// Log.d(TAG, jsonResponse.toString());
				if (jsonResponse.isNull(NEXTPAGETOKEN)) {
					pageToken = null;
				}
				else {
					pageToken = jsonResponse.getString(NEXTPAGETOKEN);
				}
				// No lists
				if (jsonResponse.isNull("items")) {
					break;
				}

				eTag += jsonResponse.getString("etag");

				JSONArray lists = jsonResponse.getJSONArray("items");

				int size = lists.length();
				int i;

				// Lists will not carry etags, must fetch them individually if
				// that
				// is desired
				for (i = 0; i < size; i++) {
					JSONObject jsonList = lists.getJSONObject(i);
					//Log.d("nononsenseapps", jsonList.toString(2));
					list.add(new GoogleTaskList(jsonList, accountName));
				}
			}
			catch (PreconditionException e) {
				// // Can not happen in this case since we don't have any etag!
				// } catch (NotModifiedException e) {
				// // Can not happen in this case since we don't have any etag!
				// }
			}
		} while (pageToken != null);

		return eTag;
	}

	*//**
	 * Given a time, will fetch all tasks which were modified afterwards
	 * 
	 * @param list
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws JSONException 
	 *//*
	public ArrayList<GoogleTask> getModifiedTasks(String lastUpdated,
			GoogleTaskList list) throws IOException, JSONException {
		ArrayList<GoogleTask> moddedList = new ArrayList<GoogleTask>();

		// If user has many tasks, they will not all be returned in same request
		String pageToken = null;

		// Loop while we have a next page to go to. Always fetch the first page
		do {
			HttpGet httpget = new HttpGet(allTasksUpdatedMin(list.remoteId,
					lastUpdated, pageToken));
			setAuthHeader(httpget);
			AndroidHttpClient.modifyRequestToAcceptGzipResponse(httpget);

			String stringResponse;
			try {
				stringResponse = parseResponse(client.execute(httpget));

				JSONObject jsonResponse = new JSONObject(stringResponse);

				// Log.d(MainActivity.TAG, jsonResponse.toString());
				// If we have a next page, get that
				if (jsonResponse.isNull(NEXTPAGETOKEN)) {
					pageToken = null;
				}
				else {
					pageToken = jsonResponse.getString(NEXTPAGETOKEN);
				}
				
				// No modified tasks
				if (jsonResponse.isNull("items")) {
					break;
				}
				
				// Will be an array of items
				JSONArray items = jsonResponse.getJSONArray("items");

				int i;
				int length = items.length();
				for (i = 0; i < length; i++) {
					JSONObject jsonTask = items.getJSONObject(i);
//					Log.d(MainActivity.TAG,
//							"moddedJSONTask: " + jsonTask.toString());
					final GoogleTask gt = new GoogleTask(jsonTask, accountName);
					gt.listdbid = list.dbid;
					moddedList.add(gt);
				}
			}
			catch (PreconditionException e) {
				// // Can't happen
				return null;
				// } catch (NotModifiedException e) {
				//
				// Log.d(TAG, e.getLocalizedMessage());
			}
		} while (pageToken != null);

		return moddedList;
	}

	*//**
	 * Returns an object if all went well. Returns null if no upload was done.
	 * Will set only remote id, etag, position and parent fields.
	 * 
	 * Updates the task in place and also returns it.
	 * 
	 * @throws PreconditionException
	 * @throws JSONException 
	 *//*
	public GoogleTask uploadTask(final GoogleTask task,
			final GoogleTaskList pList) throws
            IOException, PreconditionException, JSONException {

		if (pList.remoteId == null || pList.remoteId.isEmpty()) {
			Log.d(TAG, "Invalid list ID found for uploadTask");
			return null; // Invalid list id
		}

		// If we are trying to upload a deleted task which does not exist on
		// server, we can ignore it. might happen with conflicts
		if (task.isDeleted() && (task.remoteId == null || task.remoteId.isEmpty())) {
			Log.d(TAG, "Trying to upload a deleted non-synced note, ignoring: "
					+ task.title);
			return null;
		}

		HttpUriRequest httppost;
		if (task.remoteId != null && !task.remoteId.isEmpty()) {
			if (task.isDeleted()) {
				httppost = new HttpPost(TaskURL(task.remoteId, pList.remoteId));
				httppost.setHeader("X-HTTP-Method-Override", "DELETE");
			}
			else {
				httppost = new HttpPost(TaskURL_ETAG_ID_UPDATED(task.remoteId,
						pList.remoteId));
				// apache does not include PATCH requests, but we can force a
				// post to be a PATCH request
				httppost.setHeader("X-HTTP-Method-Override", "PATCH");
			}
			// Always set ETAGS for tasks
			// setHeaderStrongEtag(httppost, task.etag);
		}
		else {
			if (task.isDeleted()) {
				return task; // Don't sync deleted items which do not exist on
								// the server
			}

			//Log.d(TAG, "ID IS NULL: " + task.title);
			httppost = new HttpPost(AllTasksInsert(pList.remoteId));
			// task.didRemoteInsert = true; // Need this later
		}
		setAuthHeader(httppost);
		AndroidHttpClient.modifyRequestToAcceptGzipResponse(httppost);

		// Log.d(TAG, httppost.getRequestLine().toString());
		// for (Header header : httppost.getAllHeaders()) {
		// Log.d(TAG, header.getName() + ": " + header.getValue());
		// }

		if (!task.isDeleted()) {
			setPostBody(httppost, task);
		}

		String stringResponse = parseResponse(client.execute(httppost));

		// If we deleted the note, we will get an empty response. Return the
		// same element back.
		if (task.isDeleted()) {

			Log.d(TAG, "deleted and Stringresponse: " + stringResponse);
		}
		else {
			JSONObject jsonResponse = new JSONObject(stringResponse);

				// Log.d(TAG, jsonResponse.toString());

				// Will return a task, containing id and etag. always update
				// fields
				task.remoteId = jsonResponse.getString(GoogleTask.ID);
				// task.etag = jsonResponse.getString("etag");
				if (jsonResponse.has(GoogleTask.UPDATED)) {
					try {
						task.updated = RFC3339Date.parseRFC3339Date(
								jsonResponse.getString(GoogleTask.UPDATED))
								.getTime();
					}
					catch (Exception e) {
						task.updated = 0L;
					}
				}
		}

		return task;
	}

	*//**
	 * Returns an object if all went well. Returns null if a conflict was
	 * detected. If the list has deleted set to 1, will call the server and
	 * delete the list instead of updating it.
	 * 
	 * @throws IOException
	 * @throws PreconditionException
	 * @throws JSONException
	 * @throws ClientProtocolException
	 *//*
	public GoogleTaskList uploadList(final GoogleTaskList list)
			throws IOException, PreconditionException, JSONException {
		final HttpUriRequest httppost;
		if (list.remoteId != null) {
			//Log.d(TAG, "ID is not NULL!! " + ListURL(list.remoteId));
			if (list.isDeleted()) {
				httppost = new HttpDelete(ListURL(list.remoteId));
			}
			else {
				httppost = new HttpPost(ListURL(list.remoteId));
				// apache does not include PATCH requests, but we can force a
				// post to be a PATCH request
				httppost.setHeader("X-HTTP-Method-Override", "PATCH");
			}
		}
		else {
			httppost = new HttpPost(InsertLists());
			// list.didRemoteInsert = true; // Need this later
		}
		setAuthHeader(httppost);
		AndroidHttpClient.modifyRequestToAcceptGzipResponse(httppost);

		// Log.d(TAG, httppost.getRequestLine().toString());
		// for (Header header : httppost.getAllHeaders()) {
		// Log.d(TAG, header.getName() + ": " + header.getValue());
		// }

		if (!list.isDeleted()) {
			setPostBody(httppost, list);
		}

		String stringResponse = parseResponse(client.execute(httppost));

		// If we deleted the note, we will get an empty response. Return the
		// same element back.
		if (list.isDeleted()) {
			Log.d(TAG, "deleted and Stringresponse: " + stringResponse);
		}
		else {
			JSONObject jsonResponse = new JSONObject(stringResponse);

				// Log.d(TAG, jsonResponse.toString());

				// Will return a list, containing id and etag. always update
				// fields
				// list.etag = jsonResponse.getString("etag");
				list.remoteId = jsonResponse.getString("id");
				list.title = jsonResponse.getString("title");
				try {
					list.updated = RFC3339Date.parseRFC3339Date(jsonResponse.getString("updated")).getTime();
				}
				catch (Exception e) {
					list.updated = 0L;
				}
		}

		return list;
	}*/
}
