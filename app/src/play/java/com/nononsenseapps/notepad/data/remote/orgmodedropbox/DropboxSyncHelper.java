/*
 * Copyright (c) 2015 Jonas Kalderstam
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.notepad.data.remote.orgmodedropbox;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;
import com.nononsenseapps.build.Config;

/**
 * This class has some utility functions for dealing with Dropbox. You need
 * to input your API keys below.
 * See Dropbox for more information:
 * https://www.dropbox.com/developers/core/start/android
 * <p/>
 * You also need to drop your APP_KEY in the manifest in
 * com.dropbox.client2.android.AuthActivity
 * See here for info:
 * https://www.dropbox.com/developers/core/sdks/android
 */
public class DropboxSyncHelper {

    public static final String PREF_DROPBOX_TOKEN = "dropboxtoken";
    private final Context mContext;

    public DropboxAPI<AndroidAuthSession> mDBApi = null;

    public DropboxSyncHelper(Context context) {
        mContext = context;
    }

    public static DropboxAPI<AndroidAuthSession> getDBApi(
            final Context context) {
        final DropboxAPI<AndroidAuthSession> mDBApi;

        final AppKeyPair appKeys = new AppKeyPair(Config.getKeyDropboxAPI(context),  Config.getKeyDropboxAPISecret(context));
        final AndroidAuthSession session;

        if (PreferenceManager.getDefaultSharedPreferences(context)
                .contains(PREF_DROPBOX_TOKEN)) {
            session = new AndroidAuthSession(appKeys,
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .getString(PREF_DROPBOX_TOKEN, ""));
        } else {
            session = new AndroidAuthSession(appKeys);
        }
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        return mDBApi;
    }

    /**
     * Save the dropbox oauth token so we can reuse the session without
     * logging in again.
     * @param context
     * @param token
     */
    public static void saveToken(final Context context, final String token) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PREF_DROPBOX_TOKEN, token).apply();
    }

    /**
     * Must add a call to handleLinkResult in your onResume method.
     *
     * @return true if already linked, else false (and opens auth-window)
     */
    public boolean linkAccount() {
        if (mDBApi == null) {
            mDBApi = getDBApi(mContext);
        }

        // If not authorized, then ask user for login/permission
        if (!mDBApi.getSession().isLinked()) {
            mDBApi.getSession().startOAuth2Authentication(mContext);
            return false;
        } else {
            return true;
        }
    }

    /**
     *
     * @return true if user has authorized connection to dropbox
     */
    public boolean isLinked() {
        if (mDBApi == null) {
            mDBApi = getDBApi(mContext);
        }

        // If not authorized, then ask user for login/permission
        return mDBApi.getSession().isLinked();
    }

    /**
     *
     * @return Account string if linked and configured
     */
    public @NonNull String getAccount() {
        if (isLinked()) {
            try {
                return mDBApi.accountInfo().email;
            } catch (DropboxException e) {
                return "";
            }
        } else {
            return "";
        }
    }

    /**
     *
     * @return true if successfully linked, and token saved. False otherwise.
     */
    public boolean handleLinkResult() {
        if (mDBApi != null && mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();

                String accessToken = mDBApi.getSession().getOAuth2AccessToken();
                saveToken(mContext, accessToken);
                return true;
            } catch (IllegalStateException e) {
                //Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
        return false;
    }

    /**
     * Does nothing is already unlinked.
     */
    public void unlinkAccount() {
        if (mDBApi == null) {
            mDBApi = getDBApi(mContext);
        }

        if (mDBApi.getSession().isLinked()) {
            mDBApi.getSession().unlink();
        }
    }
}
