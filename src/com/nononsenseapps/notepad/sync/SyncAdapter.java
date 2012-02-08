/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.nononsenseapps.notepad.sync;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import com.nononsenseapps.notepad.sync.googleapi.GoogleAPITalker;
import com.nononsenseapps.notepad.sync.googleapi.GoogleAPITalker.NotModifiedException;
import com.nononsenseapps.notepad.sync.googleapi.GoogleAPITalker.PreconditionException;
import com.nononsenseapps.notepad.sync.googleapi.GoogleDBTalker;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTask;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTaskList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.List;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider. This sample shows a basic 2-way sync
 * between the client and a sample server. It also contains an example of how to
 * update the contacts' status messages, which would be useful for a messaging
 * or social networking client.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

	private static final String TAG = "SyncAdapter";
	//public static final String AUTH_TOKEN_TYPE = "oauth2:https://www.googleapis.com/auth/tasks";
	public static final String AUTH_TOKEN_TYPE = "Manage your tasks"; // Alias for above
	private static final String SYNC_MARKER_KEY = "com.nononsenseapps.notepad.sync.marker";
	public static final boolean NOTIFY_AUTH_FAILURE = false;

	private static final String API_KEY = "AIzaSyBCQyr-OSPQsMwU2tyCIKZG86Wb3WM1upw";// jonas@kalderstam.se

	private final AccountManager accountManager;

	private final Context mContext;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
		// mAccountManager = AccountManager.get(context);
		accountManager = AccountManager.get(context);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		// Initialize necessary stuff
		GoogleDBTalker dbTalker = new GoogleDBTalker(account.name, provider, syncResult);
		GoogleAPITalker apiTalker = GoogleAPITalker.getInstance();
		
		boolean connected = apiTalker.initialize(accountManager, account, AUTH_TOKEN_TYPE, NOTIFY_AUTH_FAILURE);
		
		if (connected) {
			Log.d(TAG, "We got an authToken atleast");
			
			try {
				for (GoogleTaskList list: apiTalker.getAllLists()) {
					Log.d(TAG, list.json.toString());
				}
			} catch (ClientProtocolException e) {
				Log.d(TAG, "ClientProtocolException: " + e.getLocalizedMessage());
			} catch (JSONException e) {
				Log.d(TAG, "JSONException: " + e.getLocalizedMessage());
			} catch (PreconditionException e) {
				Log.d(TAG, "PreconditionException");
			} catch (NotModifiedException e) {
				Log.d(TAG, "NotModifiedException");
			} catch (IOException e) {
				Log.d(TAG, "IOException: " + e.getLocalizedMessage());
			}
			
			// Upload local changes
			//List<GoogleTask> modifiedTasks = dbTalker.getModifiedTasks();
			//List<GoogleTaskList> modifiedLists = dbTalker.getModifiedLists(true);
			
//			for (GoogleTask task: modifiedTasks) {
//				GoogleTask result = apiTalker.uploadTask(task);
//				if (result != null)
//					dbTalker.uploaded(result);
//				else
//					handleConflict(dbTalker, apiTalker, task);
//			}
			
//			for (GoogleTaskList list: modifiedLists) {
//				GoogleTaskList result = apiTalker.uploadList(list);
//				if (result != null)
//					dbTalker.uploaded(result);
//				else
//					handleConflict(dbTalker, apiTalker, list);
//			}
			
			// Save remote changes
//			modifiedTasks = apiTalker.getModifiedTasks("List ETAG here");
//			for (GoogleTask task: modifiedTasks) {
//				dbTalker.SaveToDatabase(task);
//			}
//			modifiedLists = apiTalker.getModifiedLists("ETAG here");
//			for (GoogleTaskList list: modifiedLists) {
//				dbTalker.SaveToDatabase(list);
//			}
			
			// Erase deleted stuff
			//dbTalker.clearDeleted();
			
			//-----------------------------------

//		} catch (final AuthenticatorException e) {
//			Log.e(TAG, "AuthenticatorException", e);
//			syncResult.stats.numParseExceptions++;
//		} catch (final OperationCanceledException e) {
//			Log.e(TAG, "OperationCanceledExcetpion", e);
//		} catch (final IOException e) {
//			Log.e(TAG, "IOException", e);
//			syncResult.stats.numIoExceptions++;
//		}
//		// catch (final AuthenticationException e) {
//		// Log.e(TAG, "AuthenticationException", e);
//		// syncResult.stats.numAuthExceptions++;
//		// }
//		catch (final ParseException e) {
//			Log.e(TAG, "ParseException", e);
//			syncResult.stats.numParseExceptions++;
//		}
//		// catch (final JSONException e) {
//		// Log.e(TAG, "JSONException", e);
//		// syncResult.stats.numParseExceptions++;
//		// }
		}
		else {
			// return real failure
			syncResult.stats.numAuthExceptions++;
		}
	}

	private void handleConflict(GoogleDBTalker dbTalker,
			GoogleAPITalker apiTalker, GoogleTask task) {
		// TODO Auto-generated method stub
		
	}

	private void handleConflict(GoogleDBTalker dbTalker,
			GoogleAPITalker apiTalker, GoogleTaskList list) {
		// TODO Auto-generated method stub
		
	}
}
