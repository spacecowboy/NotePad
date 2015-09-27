/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.notepad.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DummyProvider extends ContentProvider {
    // TODO change authority and add corresponding manifest entry
    public static final String AUTHORITY = "com.nononsenseapps.notepad.DUMMYPROVIDER";
    public static final String SCHEME = "content://";
    private static final String TAG = "DummyProvider";

    private static final String TYPE_NONONSENSENOTES_ITEM = "vnd.android.cursor.item/item";

    private List<DummyItem> mData;

    public DummyProvider() {
    }


    @Override
    public boolean onCreate() {
        // TODO change this to actual initialization code for your own data backend
        mData = initDummyData();
        return true;
    }

    /**
     * @return initial dummy data
     */
    private List<DummyItem> initDummyData() {
        ArrayList<DummyItem> items = new ArrayList<>();

        // The uri is simply the position in the array(s)
        for (int i = 0; i < 3; i++) {
            DummyItem top = new DummyItem("/" + i, "Top item " + i);
            items.add(top);

            for (int j = 0; j < 3; j++) {
                DummyItem sub = new DummyItem(top.getPath() + "/" + j, "Sub item " + j);
                top.children.add(sub);

                for (int k = 0; k < 3; k++) {
                    DummyItem subsub = new DummyItem(sub.getPath() + "/" + k, "Subsub item " + k);
                    sub.children.add(subsub);
                }
            }
        }

        return items;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (ProviderHelper.matchUri(uri)) {
            case ProviderHelper.URI_NOMATCH:
                throw new IllegalArgumentException("Unknown path: " + uri.getPath());
            default:
                return TYPE_NONONSENSENOTES_ITEM;

        }
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        String path;
        switch (ProviderHelper.matchUri(uri)) {
            case ProviderHelper.URI_LIST:
                path = ProviderHelper.getRelativePath(uri);

                final String parentPath;
                final List<DummyItem> list;
                if ("/".equals(path)) {
                    parentPath = "/";
                    list = mData;
                } else {
                    DummyItem parent = getNestedItem(path);
                    parentPath = parent.path;
                    list = parent.children;
                }

                DummyItem item = new DummyItem(ProviderHelper.join(parentPath, Integer.toString(list.size())),
                        values);
                list.add(item);

                notifyOnChange(uri);
                return ProviderHelper.getDetailsUri(ProviderHelper.getBase(uri), item.path);
            default:
                throw new IllegalArgumentException("Can't perform insert at: " + uri.toString());
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "Uri: " + uri.toString());

        String path;
        MatrixCursor mc = new MatrixCursor(ProviderContract.sMainListProjection);

        switch (ProviderHelper.matchUri(uri)) {
            case ProviderHelper.URI_ROOT:
                setNotificationUri(mc, ProviderHelper.getListUri(ProviderHelper.getBase(uri), ""));
                for (DummyItem item : mData) {
                    mc.addRow(item.asRow());
                }
                break;
            case ProviderHelper.URI_LIST:
                setNotificationUri(mc, uri);
                path = ProviderHelper.getRelativePath(uri);

                for (DummyItem item : getNestedList(path)) {
                    mc.addRow(item.asRow());
                }
                break;
            case ProviderHelper.URI_DETAILS:
                setNotificationUri(mc, uri);
                path = ProviderHelper.getRelativePath(uri);
                mc.addRow(getNestedItem(path).asRow());
                break;
            default:
                throw new IllegalArgumentException("Unknown path: " + uri.toString());
        }

        return mc;
    }

    /**
     * Sets the notifcation uri on the cursor.
     *
     * @param c
     * @param uri
     */
    protected void setNotificationUri(Cursor c, Uri uri) {
        Context context = getContext();
        if (context != null) {
            c.setNotificationUri(context.getContentResolver(), uri);
        }
    }

    /**
     * Walk the tree, decomposing the path as we walk
     *
     * @param path like /1/2/3/4
     * @return the list of children in item /1/2/3/4
     */
    private List<DummyItem> getNestedList(String path) {
        List<DummyItem> items = mData;
        String first = ProviderHelper.firstPart(path);
        path = ProviderHelper.restPart(path);
        while (!first.isEmpty()) {
            int index = Integer.parseInt(first);
            items = items.get(index).children;

            first = ProviderHelper.firstPart(path);
            path = ProviderHelper.restPart(path);
        }

        return items;
    }

    /**
     * Walk the tree, decomposing the path as we walk
     *
     * @param path like /1/2/3/4
     * @return the item /1/2/3/4
     */
    private DummyItem getNestedItem(String path) {
        return getNestedItem(path, false);
    }

    /**
     * Walk the tree, decomposing the path as we walk
     *
     * @param path like /1/2/3/4
     * @param popItem true if item should be removed from its parent list also
     * @return the item /1/2/3/4
     */
    private DummyItem getNestedItem(String path, boolean popItem) {
        List<DummyItem> items = mData;
        DummyItem item = null;
        String first = ProviderHelper.firstPart(path);
        path = ProviderHelper.restPart(path);
        while (!first.isEmpty()) {
            int index = Integer.parseInt(first);
            item = items.get(index);
            items = item.children;

            first = ProviderHelper.firstPart(path);
            path = ProviderHelper.restPart(path);
        }

        if (popItem) {
            items.remove(item);
        }

        return item;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        String path;
        switch (ProviderHelper.matchUri(uri)) {
            case ProviderHelper.URI_DETAILS:
                path = ProviderHelper.getRelativePath(uri);

                // Any queries?
                if (uri.getQuery().isEmpty()) {
                    // Update values
                    getNestedItem(path).update(values);
                } else {
                    // Move query
                    String previous = uri.getQueryParameter(ProviderContract.QUERY_MOVE_PREVIOUS);
                    String parent = uri.getQueryParameter(ProviderContract.QUERY_MOVE_PARENT);

                    if (previous!= null || parent != null) {
                        moveDummyItem(path, previous, parent);
                    }
                }

                notifyOnChange(uri);
                break;
            default:
                throw new IllegalArgumentException("Can't perform insert at: " + uri.toString());
        }

        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     *
     * @param path relativepath to item to move
     * @param previous relativepath to sibling which should be placed before item
     * @param parent relativepath to parent item
     */
    private void moveDummyItem(String path, String previous, String parent) {
        // Pop the item
        DummyItem item = getNestedItem(path, true);

        List<DummyItem> parentList;
        if (parent == null || parent.isEmpty()) {
            parentList = mData;
        } else {
            parentList = getNestedList(parent);
        }

        int prevIndex = -1;
        if (previous != null && !previous.isEmpty()) {
            DummyItem prevItem = getNestedItem(previous);
            prevIndex = parentList.indexOf(prevItem);
        }

        // Insert into parentList at correct position
        parentList.add(prevIndex + 1, item);
    }

    /**
     * Call this after the data changes
     * @param uri to notify updates on
     */
    protected void notifyOnChange(@NonNull Uri uri) {
        Context context = getContext();
        if (context != null) {
            context.getContentResolver().notifyChange(uri, null);
        }
    }

    /**
     * Just some helpers item that represent the data backing this provider.
     */
    private static class DummyItem {
        public ArrayList<DummyItem> children = new ArrayList<>();
        protected String path;
        protected long typemask = 0;
        protected String title = "";
        protected String description = null;
        protected String status = null;
        protected String due = null;
        protected boolean deleted = false;

        public DummyItem(@NonNull String path, @NonNull String title) {
            this(path, title, ProviderContract.getTypeMask(ProviderContract.TYPE_DATA,
                    ProviderContract.TYPE_FOLDER));
        }

        public DummyItem(@NonNull String path, @NonNull String title, long bitmask) {
            this.path = path;
            this.title = title;
            this.typemask = bitmask;
        }

        public DummyItem(@NonNull String path, @NonNull ContentValues values) {
            this.path = path;
            this.title = values.getAsString(ProviderContract.COLUMN_TITLE);
            this.typemask = ProviderContract.getTypeMask(ProviderContract.TYPE_DATA,
                    ProviderContract.TYPE_FOLDER);
        }

        public Object[] asRow() {
            // For insertion into matrixcursor
            return new Object[]{path, typemask, title, description, status, due};
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(@NonNull String title) {
            this.title = title;
        }

        public long getTypemask() {
            return typemask;
        }

        public void setTypemask(long typemask) {
            this.typemask = typemask;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(@Nullable String description) {
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(@Nullable String status) {
            this.status = status;
        }

        public String getDue() {
            return due;
        }

        public void setDue(@Nullable String due) {
            this.due = due;
        }

        public boolean isDeleted() {
            return deleted;
        }

        public void setDeleted(boolean deleted) {
            this.deleted = deleted;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public void update(ContentValues values) {
            this.title = values.getAsString(ProviderContract.COLUMN_TITLE);
        }
    }
}
