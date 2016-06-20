package com.nononsenseapps.notepad.providercontract;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import static com.nononsenseapps.notepad.providercontract.UriContract.*;

/**
 * A base class for any provider that wishes to implement the contract to NoNonsense Notes. It is offered as a
 * convenience only, and is not required. You are free to write your own provider from scratch/modify your existing/this
 * one.
 */
public abstract class ProviderBase extends ContentProvider {

    protected UriMatcher uriMatcher;

    protected abstract String getAuthority();

    public ProviderBase() {
        super();

        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        addContractUrisToMatcher(uriMatcher);
    }

    private void addContractUrisToMatcher(@NonNull final UriMatcher uriMatcher) {
        addUrisToMatcher(uriMatcher, getAuthority());
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        switch (uriMatcher.match(uri)) {
            case URI_FEATURES:
                return queryFeatures(uri, projection, selection, selectionArgs, sortOrder);
            case URI_ACCOUNTS:
                return queryAccounts(uri, projection, selection, selectionArgs, sortOrder);
            case URI_ACCOUNTS_ID:
                return queryAccountsId(uri, projection, selection, selectionArgs, sortOrder);
            case URI_ITEM:
                return queryItems(uri, projection, selection, selectionArgs, sortOrder);
            case URI_ITEM_ID:
                return queryItemsId(uri, projection, selection, selectionArgs, sortOrder);
            case URI_SUBITEM:
                return querySubItems(uri, projection, selection, selectionArgs, sortOrder);
            case URI_SUBITEM_ID:
                return querySubItemsId(uri, projection, selection, selectionArgs, sortOrder);
            case UriMatcher.NO_MATCH:
                throw new IllegalArgumentException("Unknown URI: " + uri);
            default:
                throw new IllegalArgumentException("This URI does not support query: " + uri);
        }
    }

    protected abstract Cursor querySubItemsId(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);

    protected abstract Cursor querySubItems(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);

    protected abstract Cursor queryItemsId(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);

    protected abstract Cursor queryItems(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);

    protected abstract Cursor queryAccountsId(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);

    protected abstract Cursor queryAccounts(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);

    protected abstract Cursor queryFeatures(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);


    @Override
    public String getType(final Uri uri) {
        throw new IllegalArgumentException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        switch (uriMatcher.match(uri)) {
            case URI_ACCOUNTS:
                return insertAccount(uri, values);
            case URI_ITEM:
                return insertItem(uri, values);
            case URI_SUBITEM:
                return insertSubItem(uri, values);
            case UriMatcher.NO_MATCH:
                throw new IllegalArgumentException("Unknown URI: " + uri);
            default:
                throw new IllegalArgumentException("This URI does not support insert: " + uri);
        }
    }

    protected abstract Uri insertSubItem(Uri uri, ContentValues values);

    protected abstract Uri insertItem(Uri uri, ContentValues values);

    protected abstract Uri insertAccount(Uri uri, ContentValues values);

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        switch (uriMatcher.match(uri)) {
            case URI_ACCOUNTS:
                return deleteAccounts(uri, selection, selectionArgs);
            case URI_ACCOUNTS_ID:
                return deleteAccountsId(uri, selection, selectionArgs);
            case URI_ITEM:
                return deleteItem(uri, selection, selectionArgs);
            case URI_ITEM_ID:
                return deleteItemId(uri, selection, selectionArgs);
            case URI_SUBITEM:
                return deleteSubItem(uri, selection, selectionArgs);
            case URI_SUBITEM_ID:
                return deleteSubItemId(uri, selection, selectionArgs);
            case UriMatcher.NO_MATCH:
                throw new IllegalArgumentException("Unknown URI: " + uri);
            default:
                throw new IllegalArgumentException("This URI does not support delete: " + uri);
        }
    }

    protected abstract int deleteSubItemId(Uri uri, String selection, String[] selectionArgs);

    protected abstract int deleteSubItem(Uri uri, String selection, String[] selectionArgs);

    protected abstract int deleteItemId(Uri uri, String selection, String[] selectionArgs);

    protected abstract int deleteItem(Uri uri, String selection, String[] selectionArgs);

    protected abstract int deleteAccountsId(Uri uri, String selection, String[] selectionArgs);

    protected abstract int deleteAccounts(Uri uri, String selection, String[] selectionArgs);

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        switch (uriMatcher.match(uri)) {
            case URI_ACCOUNTS:
                return updateAccounts(uri, selection, selectionArgs);
            case URI_ACCOUNTS_ID:
                return updateAccountsId(uri, selection, selectionArgs);
            case URI_ITEM:
                return updateItem(uri, selection, selectionArgs);
            case URI_ITEM_ID:
                return updateItemId(uri, selection, selectionArgs);
            case URI_ITEM_ID_MOVE:
                return moveItemId(uri, selection, selectionArgs);
            case URI_SUBITEM:
                return updateSubItem(uri, selection, selectionArgs);
            case URI_SUBITEM_ID:
                return updateSubItemId(uri, selection, selectionArgs);
            case URI_SUBITEM_ID_MOVE:
                return moveSubItemId(uri, selection, selectionArgs);
            case UriMatcher.NO_MATCH:
                throw new IllegalArgumentException("Unknown URI: " + uri);
            default:
                throw new IllegalArgumentException("This URI does not support update: " + uri);
        }
    }

    protected abstract int moveSubItemId(Uri uri, String selection, String[] selectionArgs);

    protected abstract int updateSubItemId(Uri uri, String selection, String[] selectionArgs);

    protected abstract int updateSubItem(Uri uri, String selection, String[] selectionArgs);

    protected abstract int moveItemId(Uri uri, String selection, String[] selectionArgs);

    protected abstract int updateItemId(Uri uri, String selection, String[] selectionArgs);

    protected abstract int updateItem(Uri uri, String selection, String[] selectionArgs);

    protected abstract int updateAccountsId(Uri uri, String selection, String[] selectionArgs);

    protected abstract int updateAccounts(Uri uri, String selection, String[] selectionArgs);
}
