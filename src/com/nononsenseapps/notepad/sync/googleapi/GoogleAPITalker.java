package com.nononsenseapps.notepad.sync.googleapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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

	protected static final String APIKEY = "AIzaSyBCQyr-OSPQsMwU2tyCIKZG86Wb3WM1upw";// jonas@kalderstam.se
	protected static final String AUTH_URL_END = "key=" + APIKEY;
	protected static final String BASE_URL = "https://www.googleapis.com/tasks/v1/users/@me/lists";
	protected static final String BASE_LIST = BASE_URL + "/";
	// protected static final String LISTS = "/lists";
	protected static final String TASKS = "/tasks"; // Must be preceeded by
													// list-id
	private static final String TAG = "GoogleAPITalker";

	// A URL is alwasy constructed as: BASE_URL + ["/" + LISTID [+ TASKS [+ "/"
	// + TASKID]]] + "?" + [POSSIBLE FIELDS + "&"] + AUTH_URL_END
	// Where each enclosing parenthesis is optional

	protected String authToken;

	protected HttpClient client;

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

		HttpGet httpget = new HttpGet(BASE_URL + "?" + AUTH_URL_END);
		httpget.setHeader("Authorization", "OAuth " + authToken);
		Log.d(TAG, "request: " + BASE_URL + "?" + AUTH_URL_END);

		JSONObject jsonResponse = (JSONObject) new JSONTokener(
				parseResponse(client.execute(httpget))).nextValue();
		Log.d(TAG, jsonResponse.toString());

		JSONArray lists = jsonResponse.getJSONArray("items");

		int size = lists.length();
		int i;

		// All lists will not carry etags, must fetch them individually
		for (i = 0; i < size; i++) {
			JSONObject jsonList = lists.getJSONObject(i);
			list.add(getList(jsonList.getString("id")));
		}

		return list;
	}

	public GoogleTaskList getList(String listID)
			throws ClientProtocolException, JSONException,
			PreconditionException, NotModifiedException, IOException {
		GoogleTaskList result = null;
		HttpGet httpget = new HttpGet(BASE_URL + "/" + listID + "?"
				+ AUTH_URL_END);
		httpget.setHeader("Authorization", "OAuth " + authToken);
		Log.d(TAG, "request: " + BASE_URL + "?" + AUTH_URL_END);

		JSONObject jsonResponse = (JSONObject) new JSONTokener(
				parseResponse(client.execute(httpget))).nextValue();
		Log.d(TAG, jsonResponse.toString());
		result = new GoogleTaskList(jsonResponse);

		return result;
	}

	// TODO fix this comment to what works
	/**
	 * Because Google Tasks API does not return etags for every list, if there
	 * is a change we'll have to check each individually.
	 * 
	 */
	public ArrayList<GoogleTaskList> getModifiedLists(String etag) {
		// Use a header as:
		// If-None-Match: W/"D08FQn8-eil7ImA9WxZbFEw."

		// ArrayList<GoogleTaskList> localUnmodifiedLists =
		// dbTalker.getModifiedLists(false);

		return null;
	}

	/**
	 * Given a time, will fetch all tasks which were modified afterwards
	 */
	public ArrayList<GoogleTask> getModifiedTasks(String etag) {
		// Use a header as:
		// W/ indicates a weak match (semantically equal, compared to
		// byte-to-byte equal)
		// If-None-Match: W/"D08FQn8-eil7ImA9WxZbFEw."

		// Remember to set true in showDeleted
		// Make updatedMin URL friendly
		return null;
	}

	/**
	 * Returns an object if all went well. Returns null if a conflict was
	 * detected.
	 */
	public GoogleTask uploadTask(GoogleTask task) {
		return null;
	}

	/**
	 * Returns an object if all went well. Returns null if a conflict was
	 * detected.
	 * If the list has deleted set to 1, will call the server and delete the list instead of updating it.
	 * @throws IOException 
	 * @throws NotModifiedException 
	 * @throws PreconditionException 
	 * @throws JSONException 
	 * @throws ClientProtocolException 
	 */
	public GoogleTaskList uploadList(GoogleTaskList list) throws ClientProtocolException, JSONException, PreconditionException, NotModifiedException, IOException {
		HttpUriRequest httppost;
		if (list.id != null) {
			if (list.deleted == 1) {
				httppost = new HttpDelete(BASE_LIST + list.id + "?" + AUTH_URL_END);
			} else {
				httppost = new HttpPut(BASE_LIST + list.id + "?" + AUTH_URL_END);
			}
		} else {
			httppost = new HttpPost(BASE_URL + "?" + AUTH_URL_END);
		}
		
		if (list.etag != null)
			setHeaderStrongEtag(httppost, list.etag);
		
		if (list.deleted != 1) {
			setPostBody(httppost, list);
		}

		String stringResponse = parseResponse(client.execute(httppost));
		// If we deleted the note, we will get an empty response. Return the same element back.
		if (list.deleted == 1) {
			Log.d(TAG, "deleted and Stringresponse: " + stringResponse);
		}
		else {
			JSONObject jsonResponse = new JSONObject(
					);
			Log.d(TAG, jsonResponse.toString());
	
			// Will return a list, containing id and etag. always update fields
			list.etag = jsonResponse.getString("etag");
			list.id = jsonResponse.getString("id");
			list.title = jsonResponse.getString("title");
		}

		return list;
	}

	/*
	 * Communication methods
	 */

	/**
	 * Sets the authorization header
	 * @param url
	 * @return
	 */
	private void setAuthHeader(HttpUriRequest request) {
		if (request != null)
			request.setHeader("Authorization", "OAuth " + authToken);
	}
	
	/**
	 * Does nothing if etag is null or ""
	 * Sets an if-match header for strong etag comparisons.
	 * @param etag
	 */
	private void setHeaderStrongEtag(HttpUriRequest httppost, String etag) {
		if (etag !=null && !etag.equals("")) {
			httppost.setHeader("If-Match", etag);
			Log.d(TAG, "If-Match: " + etag);
		}
	}
	
	/**
	 * Does nothing if etag is null or ""
	 * Sets an if-none-match header for weak etag comparisons.
	 * @param etag
	 */
	private void setHeaderWeakEtag(HttpUriRequest httpget, String etag) {
		if (etag !=null && !etag.equals("")) {
			httpget.setHeader("If-None-Match", "W/" + etag);
			Log.d(TAG, "If-None-Match: " + "W/" + etag);
		}
	}

	/**
	 * SUpports Post and Put. Anything else will not have any effect
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

	public static String getJSONResponse(DefaultHttpClient client, String URL,
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
		// httppost.setHeader("Content-Type", "application/json");

		try {
			response = parseResponse(client.execute(httppost));
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
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

	public static String parseResponse(HttpResponse response)
			throws PreconditionException, NotModifiedException,
			ClientProtocolException {
		String page = "";
		BufferedReader in = null;

		if (response.getStatusLine().getStatusCode() == 403) {
			// Invalid authtoken
			throw new ClientProtocolException("Status: 403, Invalid authcode");
		} else if (response.getStatusLine().getStatusCode() == 412) {
			// Precondition failed. Object has been modified on server, can't do update
			throw new PreconditionException("Etags don't match, can not perform update. Resolv the conflict then update withou etag");
		} else if (response.getStatusLine().getStatusCode() == 304) {
			throw new NotModifiedException();
		}  
		else {

			try {
				in = new BufferedReader(new InputStreamReader(response
						.getEntity().getContent()));
				StringBuffer sb = new StringBuffer("");
				String line = "";
				String NL = System.getProperty("line.separator");
				while ((line = in.readLine()) != null) {
					sb.append(line + NL);
				}
				in.close();
				page = sb.toString();
				System.out.println(page);
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
