/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.notepad;

import android.app.SearchManager;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines a contract between the Note Pad content provider and its clients. A contract defines the
 * information that a client needs to access the provider as one or more data tables. A contract
 * is a public, non-extendable (final) class that contains constants defining column names and
 * URIs. A well-written client depends only on the constants in the contract.
 */
public final class NotePad {
    public static final String AUTHORITY = "com.nononsenseapps.NotePad";

    // This class cannot be instantiated
    private NotePad() {
    }

    /**
     * Notes table contract
     */
    public static final class Notes implements BaseColumns {

        // This class cannot be instantiated
        private Notes() {}

        /**
         * The table name offered by this provider
         */
        public static final String TABLE_NAME = "notes";
        public static final String KEY_WORD = SearchManager.SUGGEST_COLUMN_TEXT_1;

        /*
         * URI definitions
         */

        /**
         * The scheme part for this provider's URI
         */
        private static final String SCHEME = "content://";

        /**
         * Path parts for the URIs
         */

        /**
         * Path part for the Notes URI
         */
        private static final String PATH_NOTES = "/notes";
        // Visible notes
        private static final String PATH_VISIBLE_NOTES = "/visiblenotes";
        // Complete note entry including stuff in GTasks table
        private static final String PATH_JOINED_NOTES = "/joinednotes";

        /**
         * Path part for the Note ID URI
         */
        private static final String PATH_NOTE_ID = "/notes/";
        public static final String PATH_VISIBLE_NOTE_ID = "/visiblenotes/";

        /**
         * 0-relative position of a note ID segment in the path part of a note ID URI
         */
        public static final int NOTE_ID_PATH_POSITION = 1;

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =  Uri.parse(SCHEME + AUTHORITY + PATH_NOTES);
        public static final Uri CONTENT_VISIBLE_URI =  Uri.parse(SCHEME + AUTHORITY + PATH_VISIBLE_NOTES);
        public static final Uri CONTENT_JOINED_URI =  Uri.parse(SCHEME + AUTHORITY + PATH_JOINED_NOTES);
        
        /**
         * The content URI base for a single note. Callers must
         * append a numeric note id to this Uri to retrieve a note
         */
        public static final Uri CONTENT_ID_URI_BASE
            = Uri.parse(SCHEME + AUTHORITY + PATH_NOTE_ID);
        public static final Uri CONTENT_VISIBLE_ID_URI_BASE
        = Uri.parse(SCHEME + AUTHORITY + PATH_VISIBLE_NOTE_ID);
        
        /**
         * The content URI match pattern for a single note, specified by its ID. Use this to match
         * incoming URIs or to construct an Intent.
         */
        public static final Uri CONTENT_ID_URI_PATTERN
            = Uri.parse(SCHEME + AUTHORITY + PATH_NOTE_ID + "/#");
        public static final Uri CONTENT_VISIBLE_ID_URI_PATTERN
        = Uri.parse(SCHEME + AUTHORITY + PATH_VISIBLE_NOTE_ID + "/#");

        /*
         * MIME type definitions
         */

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        //public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nononsenseapps.note";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps.note";
        
        public static final String CONTENT_TYPE = CONTENT_ITEM_TYPE;

        

        /*
         * Column definitions
         */

        /**
         * Column name for the title of the note
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_TITLE = "title";

        /**
         * Column name of the note content
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_NOTE = "note";

        /**
         * Column name for the creation timestamp
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String COLUMN_NAME_CREATE_DATE = "created";

        /**
         * Column name for the modification timestamp
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String COLUMN_NAME_MODIFICATION_DATE = "modified";
        
        /**
         * Due date of the task (as an RFC 3339 timestamp) formatted as String.
         */
        public static final String COLUMN_NAME_DUE_DATE = "duedate";
        
        /**
         * Status of task, such as "completed"
         */
        public static final String COLUMN_NAME_GTASKS_STATUS = "gtaskstatus";
        
        /**
         * INTEGER, id of entry in lists table
         */
        public static final String COLUMN_NAME_LIST = "list";
        
        /**
         * Deleted flag
         */
        public static final String COLUMN_NAME_DELETED = "deleted";
        
        /**
         * Modified flag
         */
        public static final String COLUMN_NAME_MODIFIED = "modifiedflag";
        
        //parent position hidden
        
        public static final String COLUMN_NAME_PARENT = "gtasks_parent";
        public static final String COLUMN_NAME_POSITION = "gtasks_position";
        public static final String COLUMN_NAME_HIDDEN = "hiddenflag";
        
        // server side sorting and local hiding
        public static final String COLUMN_NAME_ABCSUBSORT = "abcsubsort";
        public static final String COLUMN_NAME_POSSUBSORT = "possubsort";
        public static final String COLUMN_NAME_LOCALHIDDEN = "localhidden";
        
        /**
         * The default sort order for this table
         */
        
        public static final String DEFAULT_NAME = "";
		
      //public static final String DEFAULT_SORT_TYPE = COLUMN_NAME_TITLE + " COLLATE NOCASE";
      public static final String ALPHABETIC_SORT_TYPE = COLUMN_NAME_TITLE + " COLLATE NOCASE";
      public static final String DUEDATE_SORT_TYPE = COLUMN_NAME_DUE_DATE;
      public static final String POSITION_SORT_TYPE = COLUMN_NAME_POSITION;
      
      public static final String ASCENDING_SORT_ORDERING = "ASC";
      public static final String DESCENDING_SORT_ORDERING = "DESC";
      public static final String ALPHABETIC_ASC_ORDER = COLUMN_NAME_TITLE + " COLLATE NOCASE ASC";
      public static final String POSITION_ASC_ORDER = COLUMN_NAME_POSITION + " ASC";
      
      public static final String DEFAULT_SORT_TYPE = ALPHABETIC_SORT_TYPE;
      public static final String DEFAULT_SORT_ORDERING = ASCENDING_SORT_ORDERING;
      
      public static String SORT_ORDER = ALPHABETIC_ASC_ORDER;
    }
    
    /**
     * Lists table contract
     */
    public static final class Lists implements BaseColumns {

        // This class cannot be instantiated
        private Lists() {}
        
        public static final String DEFAULT_LIST_NAME = "Notes";

        /**
         * The table name offered by this provider
         */
        public static final String TABLE_NAME = "lists";
        public static final String KEY_WORD = SearchManager.SUGGEST_COLUMN_TEXT_1;

        /*
         * URI definitions
         */

        /**
         * The scheme part for this provider's URI
         */
        private static final String SCHEME = "content://";

        /**
         * Path parts for the URIs
         */
        
        /**
         * Path part for the Lists URI
         */
        private static final String PATH_LISTS = "/lists";
        private static final String PATH_VISIBLE_LISTS = "/visiblelists";
        // Complete entry gotten with a join with GTasksLists table
        private static final String PATH_JOINED_LISTS = "/joinedlists";

        /**
         * Path part for the List ID URI
         */
        private static final String PATH_LIST_ID = "/lists/";
        public static final String PATH_VISIBLE_LIST_ID = "/visiblelists/";

        /**
         * 0-relative position of a ID segment in the path part of a ID URI
         */
        public static final int ID_PATH_POSITION = 1;

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =  Uri.parse(SCHEME + AUTHORITY + PATH_LISTS);
        public static final Uri CONTENT_VISIBLE_URI =  Uri.parse(SCHEME + AUTHORITY + PATH_VISIBLE_LISTS);
        public static final Uri CONTENT_JOINED_URI =  Uri.parse(SCHEME + AUTHORITY + PATH_JOINED_LISTS);

        /**
         * The content URI base for a single note. Callers must
         * append a numeric note id to this Uri to retrieve a note
         */
        public static final Uri CONTENT_ID_URI_BASE
        = Uri.parse(SCHEME + AUTHORITY + PATH_LIST_ID);
        public static final Uri CONTENT_VISIBLE_ID_URI_BASE
        = Uri.parse(SCHEME + AUTHORITY + PATH_VISIBLE_LIST_ID);

        /**
         * The content URI match pattern for a single note, specified by its ID. Use this to match
         * incoming URIs or to construct an Intent.
         */
        public static final Uri CONTENT_ID_URI_PATTERN
        = Uri.parse(SCHEME + AUTHORITY + PATH_LIST_ID + "/#");
        public static final Uri CONTENT_VISIBLE_ID_URI_PATTERN
        = Uri.parse(SCHEME + AUTHORITY + PATH_VISIBLE_LIST_ID + "/#");


        /*
         * MIME type definitions
         */

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory.
         */
        //public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nononsenseapps.list";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * item.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps.list";

        public static final String CONTENT_TYPE = CONTENT_ITEM_TYPE;
        

        /*
         * Column definitions
         */

        /**
         * Column name for the title of the note
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_TITLE = "title";
        
        /**
         * Deleted flag
         */
        public static final String COLUMN_NAME_DELETED = "deleted";
        
        /**
         * Modified flag
         */
        public static final String COLUMN_NAME_MODIFIED = "modifiedflag";
        
        /**
         * Column name for the modification timestamp
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String COLUMN_NAME_MODIFICATION_DATE = "modified";
        
        /**
         * The default sort order for this table
         */
        
        public static final String DEFAULT_SORT_TYPE = COLUMN_NAME_MODIFICATION_DATE;
        public static final String DEFAULT_SORT_ORDERING = "DESC";
        public static final String MODIFIED_DESC_ORDER = COLUMN_NAME_MODIFICATION_DATE + " DESC";
        public static final String ALPHABETIC_ASC_ORDER = COLUMN_NAME_TITLE + " COLLATE NOCASE ASC";
        
        public static String SORT_ORDER = ALPHABETIC_ASC_ORDER;
    }
    
    /**
     * GoogleTasks table contract
     */
    public static final class GTasks implements BaseColumns {

        // This class cannot be instantiated
        private GTasks() {}

        /**
         * The table name offered by this provider
         */
        public static final String TABLE_NAME = "gtasks";
        public static final String KEY_WORD = SearchManager.SUGGEST_COLUMN_TEXT_1;

        /*
         * URI definitions
         */

        /**
         * The scheme part for this provider's URI
         */
        private static final String SCHEME = "content://";

        /**
         * Path parts for the URIs
         */
        
        /**
         * Path part for the Lists URI
         */
        private static final String PATH = "/gtasks";

        /**
         * Path part for the List ID URI
         */
        private static final String PATH_ID = "/gtasks/";

        /**
         * 0-relative position of a note ID segment in the path part of a note ID URI
         */
        public static final int ID_PATH_POSITION = 1;

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =  Uri.parse(SCHEME + AUTHORITY + PATH);

        /**
         * The content URI base for a single note. Callers must
         * append a numeric note id to this Uri to retrieve a note
         */
        public static final Uri CONTENT_ID_URI_BASE
            = Uri.parse(SCHEME + AUTHORITY + PATH_ID);

        /**
         * The content URI match pattern for a single note, specified by its ID. Use this to match
         * incoming URIs or to construct an Intent.
         */
        public static final Uri CONTENT_ID_URI_PATTERN
            = Uri.parse(SCHEME + AUTHORITY + PATH_ID + "/#");


        /*
         * MIME type definitions
         */

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nononsenseapps.gtask";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps.gtask";

        

        /*
         * Column definitions
         */

        /**
         * <P>Type: INTEGER, database ID</P>
         */
        public static final String COLUMN_NAME_DB_ID = "dbid";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_GTASKS_ID = "googleid";
        
        /**
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_GOOGLE_ACCOUNT = "googleaccount";
        
        /**
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_ETAG = "etag";
        /**
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_UPDATED = "updated";
    }
    
    /**
     * GoogleTaskLists table contract
     */
    public static final class GTaskLists implements BaseColumns {

        // This class cannot be instantiated
        private GTaskLists() {}

        /**
         * The table name offered by this provider
         */
        public static final String TABLE_NAME = "gtasklists";
        public static final String KEY_WORD = SearchManager.SUGGEST_COLUMN_TEXT_1;

        /*
         * URI definitions
         */

        /**
         * The scheme part for this provider's URI
         */
        private static final String SCHEME = "content://";

        /**
         * Path parts for the URIs
         */
        
        /**
         * Path part for the Lists URI
         */
        private static final String PATH = "/gtasklists";

        /**
         * Path part for the List ID URI
         */
        private static final String PATH_ID = "/gtasklists/";

        /**
         * 0-relative position of a note ID segment in the path part of a note ID URI
         */
        public static final int ID_PATH_POSITION = 1;

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =  Uri.parse(SCHEME + AUTHORITY + PATH);

        /**
         * The content URI base for a single note. Callers must
         * append a numeric note id to this Uri to retrieve a note
         */
        public static final Uri CONTENT_ID_URI_BASE
            = Uri.parse(SCHEME + AUTHORITY + PATH_ID);

        /**
         * The content URI match pattern for a single note, specified by its ID. Use this to match
         * incoming URIs or to construct an Intent.
         */
        public static final Uri CONTENT_ID_URI_PATTERN
            = Uri.parse(SCHEME + AUTHORITY + PATH_ID + "/#");


        /*
         * MIME type definitions
         */

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nononsenseapps.gtasklist";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nononsenseapps.gtasklist";

        

        /*
         * Column definitions
         */

        /**
         * <P>Type: INTEGER, database ID</P>
         */
        public static final String COLUMN_NAME_DB_ID = "dbid";

        /**
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_GTASKS_ID = "googleid";
        
        /**
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_GOOGLE_ACCOUNT = "googleaccount";
        
        /**
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_ETAG = "etag";
        /**
         * <P>Type: TEXT</P>
         */
        public static final String COLUMN_NAME_UPDATED = "updated";
    }
}
