package com.nononsenseapps.notepad.providercontract;

import android.content.UriMatcher;

import androidx.annotation.NonNull;

/**
 * The URIs a ContentProvider is supposed to understand. Not all are required.
 */
public class UriContract {

    public static final int URI_FEATURES = 1000;

    public static final int URI_ACCOUNTS = 1001;
    public static final int URI_ACCOUNTS_ID = 1002;

    public static final int URI_ITEM = 2001;
    public static final int URI_ITEM_ID = 2002;
    public static final int URI_ITEM_ID_MOVE = 2003;

    public static final int URI_SUBITEM = 3001;
    public static final int URI_SUBITEM_ID = 3002;
    public static final int URI_SUBITEM_ID_MOVE = 3003;

    public static void addUrisToMatcher(@NonNull final UriMatcher uriMatcher,
                                        @NonNull final String authority) {
        uriMatcher.addURI(authority, "features", URI_FEATURES);
        uriMatcher.addURI(authority, "accounts", URI_ACCOUNTS);
        uriMatcher.addURI(authority, "accounts/*", URI_ACCOUNTS_ID);
        uriMatcher.addURI(authority, "accounts/*/items", URI_ITEM);
        uriMatcher.addURI(authority, "accounts/*/items/*", URI_ITEM_ID);
        uriMatcher.addURI(authority, "accounts/*/items/*/move", URI_ITEM_ID_MOVE);
        uriMatcher.addURI(authority, "accounts/*/items/*/subitems", URI_SUBITEM);
        uriMatcher.addURI(authority, "accounts/*/items/*/subitems/*", URI_SUBITEM_ID);
        uriMatcher.addURI(authority, "accounts/*/items/*/subitems/*/move", URI_SUBITEM_ID_MOVE);
    }
}
