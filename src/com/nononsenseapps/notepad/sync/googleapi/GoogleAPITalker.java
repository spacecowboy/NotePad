package com.nononsenseapps.notepad.sync.googleapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.util.Log;

/**
 * Helper class that sorts out all XML, JSON, HTTP bullshit for other classes.
 * Also keeps track of APIKEY and AuthToken
 * 
 * @author Jonas
 * 
 */
public class GoogleAPITalker {

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

	public static final String APIKEY = "AIzaSyBCQyr-OSPQsMwU2tyCIKZG86Wb3WM1upw";// jonas@kalderstam.se
	public static final String AUTH_URL_END = "key=" + APIKEY;
	public static final String BASE_URL = "https://www.googleapis.com/tasks/v1/users/@me/lists";
	public static final String ALL_LISTS = BASE_URL + "?" + AUTH_URL_END;

	public static String ListURL(String id) {
		return BASE_URL + "/" + id + "?" + AUTH_URL_END;
	}

	// public static final String LISTS = "/lists";
	public static final String BASE_TASK_URL = "https://www.googleapis.com/tasks/v1/lists";
	public static final String TASKS = "/tasks"; // Must be preceeded by

	// only retrieve the fields we will save in the database or use
	// https://www.googleapis.com/tasks/v1/lists/MDIwMzMwNjA0MjM5MzQ4MzIzMjU6MDow/tasks?showDeleted=true&showHidden=true&pp=1&key={YOUR_API_KEY}
	// updatedMin=2012-02-07T14%3A59%3A05.000Z
	public static String AllTasks(String listId) {
		return BASE_TASK_URL + "/" + listId + TASKS + "?"
				+ "showDeleted=true&showHidden=true&" + AUTH_URL_END;
	}

	public static String AllTasksInsert(String listId) {
		return BASE_TASK_URL + "/" + listId + TASKS + "?" + AUTH_URL_END;
	}

	public static String TaskURL(String taskId, String listId) {
		return BASE_TASK_URL + "/" + listId + TASKS + "/" + taskId + "?"
				+ AUTH_URL_END;
	}

	public static String TaskURL_ETAG_ID(String taskId, String listId) {
		return BASE_TASK_URL + "/" + listId + TASKS + "/" + taskId
				+ "?fields=id,etag&" + AUTH_URL_END;
	}

	public static String AllTasksCompletedMin(String listId, String timestamp) {
		if (timestamp == null)
			return BASE_TASK_URL + "/" + listId + TASKS
					+ "?showDeleted=true&showHidden=true&fields=items&"
					+ AUTH_URL_END;
		else {
			try {
				return BASE_TASK_URL
						+ "/"
						+ listId
						+ TASKS
						+ "?showDeleted=true&showHidden=true&fields=items&updatedMin="
						+ URLEncoder.encode(timestamp, "UTF-8") + "&"
						+ AUTH_URL_END;
			} catch (UnsupportedEncodingException e) {
				Log.d(TAG, "Malformed timestamp: " + e.getLocalizedMessage());
				return BASE_TASK_URL + "/" + listId + TASKS
						+ "?showDeleted=true&showHidden=true&fields=items&"
						+ AUTH_URL_END;
			}
		}
	}

	// Tasks URL which inludes deleted tasks: /tasks?showDeleted=true
	// Also do showHidden=true?
	// Tasks returnerd will have deleted = true or no deleted field at all. Same
	// case for hidden.
	private static final String TAG = "GoogleAPITalker";

	// A URL is alwasy constructed as: BASE_URL + ["/" + LISTID [+ TASKS [+ "/"
	// + TASKID]]] + "?" + [POSSIBLE FIELDS + "&"] + AUTH_URL_END
	// Where each enclosing parenthesis is optional

	public String authToken;

	public HttpClient client;

	private static GoogleAPITalker instance;

	public static GoogleAPITalker getInstance() {
		if (instance == null) {
			instance = new GoogleAPITalker();
		}
		return instance;
	}

	public static String getAuthToken(AccountManager accountManager,
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
		} catch (OperationCanceledException e) {
		} catch (AuthenticatorException e) {
		} catch (IOException e) {
		}
		return authToken;
	}

	public boolean initialize(AccountManager accountManager, Account account,
			String authTokenType, boolean notifyAuthFailure) {
		Log.d(TAG, "initialize");
		HttpParams params = new BasicHttpParams();
		params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION,
				HttpVersion.HTTP_1_1);
		client = new DefaultHttpClient(params);

		authToken = getAuthToken(accountManager, account, authTokenType,
				notifyAuthFailure);

		Log.d(TAG, "authToken: " + authToken);
		if (authToken != null && !authToken.equals("")) {
			return true;
		} else {
			return false;
		}
	}

	/*
	 * User methods
	 */

	public ArrayList<GoogleTaskList> getAllLists()
			throws ClientProtocolException, JSONException,
			PreconditionException, NotModifiedException, IOException {
		ArrayList<GoogleTaskList> list = new ArrayList<GoogleTaskList>();

		// Lists will not carry etags, must fetch them individually
		for (GoogleTaskList gimpedList : getListOfLists()) {
			list.add(getList(gimpedList));
		}

		return list;
	}

	/**
	 * The entries in this does only one net-call, and such the list items do
	 * not contain e-tags. useful to get an id-list.
	 * 
	 * @return
	 * @throws IOException
	 * @throws NotModifiedException
	 * @throws PreconditionException
	 * @throws JSONException
	 * @throws ClientProtocolException
	 */
	private ArrayList<GoogleTaskList> getListOfLists()
			throws ClientProtocolException, JSONException, IOException {
		ArrayList<GoogleTaskList> list = new ArrayList<GoogleTaskList>();

		HttpGet httpget = new HttpGet(ALL_LISTS);
		httpget.setHeader("Authorization", "OAuth " + authToken);
		Log.d(TAG, "request: " + ALL_LISTS);

		JSONObject jsonResponse;
		try {
			jsonResponse = (JSONObject) new JSONTokener(
					parseResponse(client.execute(httpget))).nextValue();

			Log.d(TAG, jsonResponse.toString());

			JSONArray lists = jsonResponse.getJSONArray("items");

			int size = lists.length();
			int i;

			// All lists will not carry etags, must fetch them individually
			for (i = 0; i < size; i++) {
				JSONObject jsonList = lists.getJSONObject(i);
				list.add(new GoogleTaskList(jsonList));
			}
		} catch (PreconditionException e) {
			// Can not happen in this case since we don't have any etag!
		} catch (NotModifiedException e) {
			// Can not happen in this case since we don't have any etag!
		}

		return list;
	}

	/**
	 * If etag is present, will make a if-none-match request. Expects only id
	 * and possibly etag to be present in the object
	 * 
	 * @param gimpedTask
	 * @return
	 * @throws IOException
	 * @throws NotModifiedException
	 * @throws JSONException
	 * @throws ClientProtocolException
	 */
	public GoogleTask getTask(GoogleTask gimpedTask, GoogleTaskList list)
			throws ClientProtocolException, JSONException,
			NotModifiedException, IOException {
		GoogleTask result = null;
		HttpGet httpget = new HttpGet(TaskURL(gimpedTask.id, list.id));
		setAuthHeader(httpget);
		setHeaderWeakEtag(httpget, gimpedTask.etag);
		Log.d(TAG, "request: " + TaskURL(gimpedTask.id, list.id));

		JSONObject jsonResponse;
		try {
			jsonResponse = (JSONObject) new JSONTokener(
					parseResponse(client.execute(httpget))).nextValue();

			Log.d(TAG, jsonResponse.toString());
			result = new GoogleTask(jsonResponse);
		} catch (PreconditionException e) {
			// Can not happen since we are not doing a PUT/POST
		}

		result.listdbid = list.dbId;
		return result;
	}

	/**
	 * Takes a list object because the etag is optional here. If one is present,
	 * will make a if-none-match request.
	 * 
	 * @param gimpedList
	 * @return
	 * @throws ClientProtocolException
	 * @throws JSONException
	 * @throws PreconditionException
	 * @throws NotModifiedException
	 * @throws IOException
	 */
	public GoogleTaskList getList(GoogleTaskList gimpedList)
			throws ClientProtocolException, JSONException,
			NotModifiedException, IOException {
		GoogleTaskList result = null;
		HttpGet httpget = new HttpGet(ListURL(gimpedList.id));
		setAuthHeader(httpget);
		setHeaderWeakEtag(httpget, gimpedList.etag);
		Log.d(TAG, "request: " + ListURL(gimpedList.id));

		JSONObject jsonResponse;
		try {
			jsonResponse = (JSONObject) new JSONTokener(
					parseResponse(client.execute(httpget))).nextValue();

			Log.d(TAG, jsonResponse.toString());
			result = new GoogleTaskList(jsonResponse);
		} catch (PreconditionException e) {
			// Can not happen since we are not doing a PUT/POST
		}

		return result;
	}

	/**
	 * Because Google Tasks API does not return etags in its list of lists,
	 * we'll have to download each individually. Use our local lists' etags to
	 * only download lists which have changed (and new ones of course).
	 * 
	 * Also, because the API does not support deleted flags on lists, we have to
	 * compare with the local list to find missing (deleted) lists.
	 * 
	 * @throws IOException
	 * @throws JSONException
	 * @throws ClientProtocolException
	 * 
	 */
	public ArrayList<GoogleTaskList> getModifiedLists(
			ArrayList<GoogleTaskList> allLocalLists)
			throws ClientProtocolException, JSONException, IOException {
		ArrayList<GoogleTaskList> modifiedLists = new ArrayList<GoogleTaskList>();
		// Get list of lists
		for (GoogleTaskList gimpedList : getListOfLists()) {
			boolean retrieved = false;
			for (GoogleTaskList localList : allLocalLists) {
				if (gimpedList.id.equals(localList.id)) {
					// Remove from list as well. any that remains do not exist
					// on server, and hence should be deleted
					allLocalLists.remove(localList);
					// For any that exist in localList, use if-none-match header
					// to get
					try {
						// Locallist contains the ETAG, and the DBID
						GoogleTaskList updatedList = getList(localList);
						updatedList.dbId = localList.dbId;
						modifiedLists.add(updatedList);
					} catch (NotModifiedException e) {
						// We already have the newest version. nothing to do
					}
					retrieved = true;
					break; // Break first loop, since we found it
				}
			}
			if (!retrieved) {
				// for new items, just get, and save
				try {
					modifiedLists.add(getList(gimpedList));
				} catch (NotModifiedException e) {
					// Can't happen sinced gimpedList doesn't have an etag
					// Even if it did, it would mean we should do nothing here
				}
			}
		}

		// Any lists that remain in the original allLocalLists, could not be
		// found on server. Hence
		// they must have been deleted. Set them as deleted and add to modified
		// list
		for (GoogleTaskList deletedList : allLocalLists) {
			deletedList.deleted = 1;
			modifiedLists.add(deletedList);
		}

		return modifiedLists;
	}

	/**
	 * Given a time, will fetch all tasks which were modified afterwards
	 * 
	 * @param googleTaskList
	 */
	public ArrayList<GoogleTask> getModifiedTasks(String lastUpdated,
			GoogleTaskList list) {
		ArrayList<GoogleTask> moddedList = new ArrayList<GoogleTask>();

		HttpGet httpget = new HttpGet(
				AllTasksCompletedMin(list.id, lastUpdated));
		setAuthHeader(httpget);

		Log.d(TAG, httpget.getRequestLine().toString());
		for (Header header : httpget.getAllHeaders()) {
			Log.d(TAG, header.getName() + ": " + header.getValue());
		}

		String stringResponse;
		try {
			stringResponse = parseResponse(client.execute(httpget));

			JSONObject jsonResponse = new JSONObject(stringResponse);
			Log.d(TAG, jsonResponse.toString());
			// Will be an array of items
			JSONArray items = jsonResponse.getJSONArray("items");

			int i;
			int length = items.length();
			for (i = 0; i < length; i++) {
				JSONObject jsonTask = items.getJSONObject(i);
				moddedList.add(new GoogleTask(jsonTask));
			}
		} catch (PreconditionException e) {
			// Can't happen
			return null;
		} catch (ClientProtocolException e) {
			Log.d(TAG, e.getLocalizedMessage());
		} catch (NotModifiedException e) {
			Log.d(TAG, e.getLocalizedMessage());
		} catch (IOException e) {
			Log.d(TAG, e.getLocalizedMessage());
		} catch (JSONException e) {
			// Item list must have been empty
			Log.d(TAG, e.getLocalizedMessage());
		}

		return moddedList;
	}

	/**
	 * Returns an object if all went well. Returns null if a conflict was
	 * detected. Will return a partial result containing only id and etag
	 */
	public GoogleTask uploadTask(GoogleTask task, GoogleTaskList pList)
			throws ClientProtocolException, JSONException,
			NotModifiedException, IOException {

		HttpUriRequest httppost;
		if (task.id != null) {
			Log.d(TAG, "ID is not NULL!! " + TaskURL_ETAG_ID(task.id, pList.id));
			if (task.deleted == 1) {
				httppost = new HttpDelete(TaskURL_ETAG_ID(task.id, pList.id));
			} else {
				httppost = new HttpPost(TaskURL_ETAG_ID(task.id, pList.id));
				// apache does not include PATCH requests, but we can force a
				// post to be a PATCH request
				httppost.setHeader("X-HTTP-Method-Override", "PATCH");
			}
		} else {
			Log.d(TAG, "ID IS NULL: " + AllTasksInsert(pList.id));
			httppost = new HttpPost(AllTasksInsert(pList.id));
			task.didRemoteInsert = true; // Need this later
		}
		setAuthHeader(httppost);

		if (task.etag != null)
			setHeaderStrongEtag(httppost, task.etag);

		Log.d(TAG, httppost.getRequestLine().toString());
		for (Header header : httppost.getAllHeaders()) {
			Log.d(TAG, header.getName() + ": " + header.getValue());
		}

		if (task.deleted != 1) {
			setPostBody(httppost, task);
		}

		String stringResponse;
		try {
			stringResponse = parseResponse(client.execute(httppost));

			// If we deleted the note, we will get an empty response. Return the
			// same element back.
			if (task.deleted == 1) {
				Log.d(TAG, "deleted and Stringresponse: " + stringResponse);
			} else {
				JSONObject jsonResponse = new JSONObject(stringResponse);
				Log.d(TAG, jsonResponse.toString());

				// Will return a task, containing id and etag. always update
				// fields
				task.etag = jsonResponse.getString("etag");
				task.id = jsonResponse.getString("id");
			}
		} catch (PreconditionException e) {
			// There was a conflict, return null in that case
			return null;
		}

		return task;
	}

	/**
	 * Returns an object if all went well. Returns null if a conflict was
	 * detected. If the list has deleted set to 1, will call the server and
	 * delete the list instead of updating it.
	 * 
	 * @throws IOException
	 * @throws NotModifiedException
	 * @throws PreconditionException
	 * @throws JSONException
	 * @throws ClientProtocolException
	 */
	public GoogleTaskList uploadList(GoogleTaskList list)
			throws ClientProtocolException, JSONException,
			NotModifiedException, IOException {
		HttpUriRequest httppost;
		if (list.id != null) {
			Log.d(TAG, "ID is not NULL!! " + ListURL(list.id));
			if (list.deleted == 1) {
				httppost = new HttpDelete(ListURL(list.id));
			} else {
				httppost = new HttpPost(ListURL(list.id));
				// apache does not include PATCH requests, but we can force a
				// post to be a PATCH request
				httppost.setHeader("X-HTTP-Method-Override", "PATCH");
			}
		} else {
			Log.d(TAG, "ID IS NULL: " + ALL_LISTS);
			httppost = new HttpPost(ALL_LISTS);
			list.didRemoteInsert = true; // Need this later
		}
		setAuthHeader(httppost);

		if (list.etag != null)
			setHeaderStrongEtag(httppost, list.etag);

		Log.d(TAG, httppost.getRequestLine().toString());
		for (Header header : httppost.getAllHeaders()) {
			Log.d(TAG, header.getName() + ": " + header.getValue());
		}

		if (list.deleted != 1) {
			setPostBody(httppost, list);
		}

		String stringResponse;
		try {
			stringResponse = parseResponse(client.execute(httppost));

			// If we deleted the note, we will get an empty response. Return the
			// same element back.
			if (list.deleted == 1) {
				Log.d(TAG, "deleted and Stringresponse: " + stringResponse);
			} else {
				JSONObject jsonResponse = new JSONObject(stringResponse);
				Log.d(TAG, jsonResponse.toString());

				// Will return a list, containing id and etag. always update
				// fields
				list.etag = jsonResponse.getString("etag");
				list.id = jsonResponse.getString("id");
				list.title = jsonResponse.getString("title");
			}
		} catch (PreconditionException e) {
			// There was a conflict, return null in that case
			return null;
		}

		return list;
	}

	/*
	 * Communication methods
	 */

	/**
	 * Sets the authorization header
	 * 
	 * @param url
	 * @return
	 */
	private void setAuthHeader(HttpUriRequest request) {
		if (request != null)
			request.setHeader("Authorization", "OAuth " + authToken);
	}

	/**
	 * Does nothing if etag is null or "" Sets an if-match header for strong
	 * etag comparisons.
	 * 
	 * @param etag
	 */
	private void setHeaderStrongEtag(HttpUriRequest httppost, String etag) {
		if (etag != null && !etag.equals("")) {
			httppost.setHeader("If-Match", etag);
			Log.d(TAG, "If-Match: " + etag);
		}
	}

	/**
	 * Does nothing if etag is null or "" Sets an if-none-match header for weak
	 * etag comparisons.
	 * 
	 * If-None-Match: W/"D08FQn8-eil7ImA9WxZbFEw."
	 * 
	 * @param etag
	 */
	private void setHeaderWeakEtag(HttpUriRequest httpget, String etag) {
		if (etag != null && !etag.equals("")) {
			httpget.setHeader("If-None-Match", etag);
			Log.d(TAG, "If-None-Match: " + etag);
		}
	}

	/**
	 * SUpports Post and Put. Anything else will not have any effect
	 * 
	 * @param httppost
	 * @param list
	 */
	private void setPostBody(HttpUriRequest httppost, GoogleTaskList list) {
		StringEntity se = null;
		try {
			se = new StringEntity(list.toJSON(), HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		se.setContentType("application/json");
		if (httppost instanceof HttpPost)
			((HttpPost) httppost).setEntity(se);
		else if (httppost instanceof HttpPut)
			((HttpPut) httppost).setEntity(se);
	}

	/**
	 * SUpports Post and Put. Anything else will not have any effect
	 * 
	 * @param httppost
	 * @param list
	 */
	private void setPostBody(HttpUriRequest httppost, GoogleTask task) {
		StringEntity se = null;
		try {
			se = new StringEntity(task.toJSON(), HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		se.setContentType("application/json");
		if (httppost instanceof HttpPost)
			((HttpPost) httppost).setEntity(se);
		else if (httppost instanceof HttpPut)
			((HttpPut) httppost).setEntity(se);
	}

	public static String getValueFromJSON(String response, String key) {
		String value = null;
		JSONObject object;
		try {
			object = (JSONObject) new JSONTokener(response).nextValue();
			value = object.getString(key);
		} catch (JSONException e) {
		}

		return value;
	}

	private static String getJSONResponse(DefaultHttpClient client, String URL,
			String postJSON) throws PreconditionException, NotModifiedException {
		String response = null;
		HttpPost httppost = new HttpPost(URL);
		StringEntity se = null;
		try {
			se = new StringEntity(postJSON, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
		}

		se.setContentType("application/json");
		httppost.setEntity(se);
		httppost.setHeader("Content-Type", "application/json");

		try {
			response = parseResponse(client.execute(httppost));
			Log.d(TAG, "JSONRESPONSE: " + response);
		} catch (ClientProtocolException e) {
			Log.d(TAG, "JSONRESPONSE: ClientProtocolException");
		} catch (IOException e) {
			Log.d(TAG, "JSONRESPONSE: IOException");
		}

		return response;
	}

	public static Document getXMLResponse(DefaultHttpClient client,
			String authToken, String URL, String post)
			throws PreconditionException, NotModifiedException {
		HttpPost httppost = new HttpPost(URL);

		StringEntity se = null;
		try {
			se = new StringEntity(post, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
		}

		se.setContentType("text/xml");
		httppost.setEntity(se);

		httppost.setHeader("Authorization", "OAuth " + authToken);

		Document doc = null;
		try {
			doc = XMLfromString(parseResponse(client.execute(httppost)));
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}

		return doc;
	}

	public static Document getXMLResponse(DefaultHttpClient client,
			String authToken, String URL) throws PreconditionException,
			NotModifiedException {
		HttpGet request = new HttpGet(URL);
		request.setHeader("Authorization", "OAuth " + authToken);

		Document doc = null;
		try {
			doc = XMLfromString(parseResponse(client.execute(request)));
		} catch (ClientProtocolException e) {

		} catch (IOException e) {

		}

		return doc;
	}

	/**
	 * Parses a httpresponse and returns the string body of it. Throws
	 * exceptions for select status codes.
	 */
	public static String parseResponse(HttpResponse response)
			throws PreconditionException, NotModifiedException,
			ClientProtocolException {
		String page = "";
		BufferedReader in = null;

		Log.d(TAG, "HTTP Response Code: "
				+ response.getStatusLine().getStatusCode());

		if (response.getStatusLine().getStatusCode() == 403) {
			// Invalid authtoken
			throw new ClientProtocolException("Status: 403, Invalid authcode");
		} else if (response.getStatusLine().getStatusCode() == 412) {
			// Precondition failed. Object has been modified on server, can't do
			// update
			throw new PreconditionException(
					"Etags don't match, can not perform update. Resolv the conflict then update without etag");
		} else if (response.getStatusLine().getStatusCode() == 304) {
			throw new NotModifiedException();
		} else if (response.getStatusLine().getStatusCode() == 400) {
			// Warning: can happen for a legitimate case
			// This happens if you try to delete the default list.
			// Resolv it by considering the delete successful. List will still
			// exist on server, but all tasks will be deleted from it.
			// A successful delete returns an empty response.
			// Make a log entry about it anyway though
			Log.d(TAG,
					"Response was 400. Either we deleted the default list in app or did something really bad");
			return "";
		} else if (response.getStatusLine().getStatusCode() == 204) {
			// Successful delete of a tasklist. return empty string as that is
			// expected from delete
			Log.d(TAG, "Response was 204: Successful delete");
			return "";
		} else {

			try {
				if (response.getEntity() != null) {
					// Only call getContent ONCE
					InputStream content = response.getEntity().getContent();
					if (content != null) {
						in = new BufferedReader(new InputStreamReader(content));
						StringBuffer sb = new StringBuffer("");
						String line = "";
						String NL = System.getProperty("line.separator");
						while ((line = in.readLine()) != null) {
							sb.append(line + NL);
						}
						in.close();
						page = sb.toString();
						System.out.println(page);
					}
				}
			} catch (IOException e) {
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return page;
	}

	public static Document XMLfromString(String xml) {

		Document doc = null;

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {

			DocumentBuilder db = dbf.newDocumentBuilder();

			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xml));
			doc = db.parse(is);

		} catch (ParserConfigurationException e) {
			// System.out.println("XML parse error: " + e.getMessage());
			return null;
		} catch (SAXException e) {
			// System.out.println("Wrong XML file structure: " +
			// e.getMessage());
			return null;
		} catch (IOException e) {
			// System.out.println("I/O exeption: " + e.getMessage());
			return null;
		}

		return doc;

	}

	/**
	 * Returns element value
	 * 
	 * @param elem
	 *            element (it is XML tag)
	 * @return Element value otherwise empty String
	 */
	private final static String getElementValue(Node elem) {
		Node kid;
		if (elem != null) {
			if (elem.hasChildNodes()) {
				for (kid = elem.getFirstChild(); kid != null; kid = kid
						.getNextSibling()) {
					if (kid.getNodeType() == Node.TEXT_NODE) {
						return kid.getNodeValue();
					}
				}
			}
		}
		return "";
	}

	public static String getValue(Element item, String str) {
		NodeList n = item.getElementsByTagName(str);
		return getElementValue(n.item(0));
	}

	public static String getAttribute(Element item, String tag, String attr) {
		return ((Element) item.getElementsByTagName(tag).item(0))
				.getAttribute(attr);
	}
}
