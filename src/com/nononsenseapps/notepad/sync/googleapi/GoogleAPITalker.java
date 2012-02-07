package com.nononsenseapps.notepad.sync.googleapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
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

/**
 * Helper class that sorts out all XML, JSON, HTTP bullshit for other classes.
 * Also keeps track of APIKEY and AuthToken
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

	protected static final String APIKEY = "AIzaSyBCQyr-OSPQsMwU2tyCIKZG86Wb3WM1upw";// jonas@kalderstam.se
	protected static final String APIKEY_URL_END = "&key=" + APIKEY;
	protected static final String BASE_URL = "https://www.googleapis.com/tasks/v1/users/@me/lists";
	//protected static final String LISTS = "/lists"; 
	protected static final String TASKS = "/tasks"; // Must be preceeded by list-id
	
	// A URL is alwasy constructed as: BASE_URL + ["/" + LISTID [+ TASKS [+ "/" + TASKID]]]
	// Where each enclosing parenthesis is optional
	
	protected String authToken;
	
	private static GoogleAPITalker instance;
	
	public static GoogleAPITalker getInstance() {
		if (instance == null) {
			instance = new GoogleAPITalker();
		}
		return instance;
	}
	
	public static String getAuthToken(AccountManager accountManager, Account account, String authTokenType, boolean notifyAuthFailure) {
		authToken = "";
		try {
			authToken = accountManager.blockingGetAuthToken(
					account, authTokenType, notifyAuthFailure);
		} catch (OperationCanceledException e) {
		} catch (AuthenticatorException e) {
		} catch (IOException e) {
		}
		return authToken;
	}
	
	public boolean initialize(AccountManager accountManager, Account account, String authTokenType, boolean notifyAuthFailure) {
		authToken = getAuthToken(accountManager, account, authTokenType, notifyAuthFailure);
		
		if (authToken != null && !authToken.equals("")) {
			return true;
		} else {
			return false;
		}
	}
	
	/*
	 * User methods
	 */
	
	// TODO fix this comment to what works
	/**
	 * Because Google Tasks API does not return etags for every list, if there is a change we'll
	 * have to check each individually.
	 * 
	 */
	public ArrayList<GoogleTaskList> getModifiedLists(String etag) {
		// Use a header as:
		// If-None-Match: W/"D08FQn8-eil7ImA9WxZbFEw."
		
		//ArrayList<GoogleTaskList> localUnmodifiedLists = dbTalker.getModifiedLists(false);
		
		return null;
	}
	
	/**
	 * Given a time, will fetch all tasks which were modified afterwards
	 */
	public ArrayList<GoogleTask> getModifiedTasks(String etag) {
		// Use a header as:
		// If-None-Match: W/"D08FQn8-eil7ImA9WxZbFEw."
		
		// Remember to set true in showDeleted
		// Make updatedMin URL friendly
		return null;
	}
	
	/**
	 * Returns an object if all went well.
	 * Returns null if a conflict was detected.
	 */
	public GoogleTask uploadTask(GoogleTask task) {
		return null;
	}
	
	/**
	 * Returns an object if all went well.
	 * Returns null if a conflict was detected.
	 */
	public GoogleTaskList uploadList(GoogleTaskList list) {
		return null;
	}
	
	
	/*
	 * Communication methods
	 */
	
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
    
    public static String getJSONResponse(DefaultHttpClient client, String URL, String postJSON) throws PreconditionException {
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
    
    public static Document getXMLResponse(DefaultHttpClient client, String authToken, String URL, String post) throws PreconditionException {
		HttpPost httppost = new HttpPost(URL);

		StringEntity se = null;
		try {
			se = new StringEntity(post, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
		}

		se.setContentType("text/xml");
		httppost.setEntity(se);

		httppost.setHeader("Authorization", "GoogleLogin auth=" + authToken);

		Document doc = null;
		try {
			doc = XMLfromString(parseResponse(client.execute(httppost)));
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}

		return doc;
	}
    
    public static Document getXMLResponse(DefaultHttpClient client, String authToken, String URL) throws PreconditionException {
		HttpGet request = new HttpGet(URL);
		request.setHeader("Authorization", "GoogleLogin auth=" + authToken);

		Document doc = null;
		try {
			doc = XMLfromString(parseResponse(client.execute(request)));
		} catch (ClientProtocolException e) {
			
		} catch (IOException e) {
			
		}

		return doc;
	}
    
    public static String parseResponse(HttpResponse response) throws PreconditionException {
		String page = "";
		BufferedReader in = null;
		
		if (response.getStatusLine().getStatusCode() == 403) {
			// Invalid authtoken
		}
		else if (response.getStatusLine().getStatusCode() == 412) {
			// Precondition failed. Object has been modified on server, can't do partial update
			throw new PreconditionException();
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
			//System.out.println("XML parse error: " + e.getMessage());
			return null;
		} catch (SAXException e) {
			//System.out.println("Wrong XML file structure: " + e.getMessage());
			return null;
		} catch (IOException e) {
			//System.out.println("I/O exeption: " + e.getMessage());
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
