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
import com.google.api.client.auth.oauth2.draft10.AccessProtectedResource;
import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
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
		GoogleDBTalker dbTalker = new GoogleDBTalker(account.getAccountName(), provider, syncResult);
		GoogleAPITalker apiTalker = GoogleAPITalker.getInstance();
		
		boolean connected = apiTalker.initialize(accountManager, account, AUTH_TOKEN_TYPE, NOTIFY_AUTH_FAILURE);
		
		if (connected) {
			// Upload local changes
			List<GoogleTask> modifiedTasks = dbTalker.getModifiedTasks();
			List<GoogleTaskList> modifiedLists = dbTalker.getModifiedLists(true);
			
			for (GoogleTask task: modifiedTasks) {
				if (apiTalker.uploadTask(task))
					dbTalker.uploaded(task);
				else
					handleConflict(dbTalker, apiTalker, task);
			}
			
			for (GoogleTaskList list: modifiedLists) {
				if (apiTalker.uploadList(list))
					dbTalker.uploaded(list);
				else
					handleConflict(dbTalker, apiTalker, list);
			}
			
			// Save remote changes
			modifiedTasks = apiTalker.getModifiedTasks();
			for (GoogleTask task: modifiedTasks) {
				dbTalker.saveToDatabase(task);
			}
			modifiedLists = apiTalker.getModifiedLists();
			for (GoogleTaskList list: modifiedLists) {
				dbTalker.saveToDatabase(list);
			}
			
			// Erase deleted stuff
			dbTalker.clearDeleted();

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

	/*
	 * private void gotAccount(final Account account) {
	 * accountManager.manager.getAuthToken(account, AUTH_TOKEN_TYPE, true, new
	 * AccountManagerCallback<Bundle>() {
	 * 
	 * public void run(AccountManagerFuture<Bundle> future) { try { Bundle
	 * bundle = future.getResult(); if
	 * (bundle.containsKey(AccountManager.KEY_INTENT)) { Log.d(TAG,
	 * "future key intent");
	 * 
	 * Intent intent = bundle .getParcelable(AccountManager.KEY_INTENT);
	 * intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
	 * mContext.startActivity(intent); //startActivityForResult(intent,0); }
	 * else if (bundle .containsKey(AccountManager.KEY_AUTHTOKEN)) { Log.d(TAG,
	 * "future key authtoken"); accessProtectedResource .setAccessToken(bundle
	 * .getString(AccountManager.KEY_AUTHTOKEN)); String
	 * authToken=bundle.getString(AccountManager.KEY_AUTHTOKEN).toString();
	 * Log.d("TAG", "Auth Token ="+authToken); //onAuthToken(); } } catch
	 * (Exception e) { Log.d(TAG, "future exception"); } } }, null); }
	 */
}
