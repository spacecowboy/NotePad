/*
 * Copyright (C) 2012 Jonas Kalderstam
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

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import android.util.Log;

/**
 * Handles parsing of attributes which are saved inside the note in order to get
 * them to sync. These attributes all are enclosed in [square brackets]. As a
 * convention they must be located at the end of the note. Ending/leading
 * whitespace will be ignored but character falling outside square brackets are
 * ignored and will terminate the parsing.
 * 
 * These are the attributes so far: [locked] - indicates that the note requires
 * verification by password to be opened.
 * 
 * [r:12:00] - short hand for a reminder set on the same date as the due date
 * [r:31.01.12 20:00] - defines a reminder 31st january 2012 at 8PM.
 * 
 * @author Jonas
 * 
 */
public class NoteAttributes {
	// Shorthand for newline
	private static final String NL = "\n";
	// Full locked string
	public static final String LOCKED = "[locked]";
	// \s without newline
	public static final Pattern LOCKED_REGEX = Pattern.compile("\\n?[ \\t\\x0B\\f\\r]*\\[locked\\]");
	
	String noteText;
	ArrayList<String> reminders;
	boolean locked = false;

	public NoteAttributes() {
		noteText = "";
		reminders = new ArrayList<String>();
	}
	
	/**
	 * Returns the result of a previously parsed note
	 * @return
	 */
	public String getNoteText() {
		return noteText;
	}

	/**
	 * Returns the noteText with attributes removed.
	 */
	public String parseNote(String fullNote) {
		if (fullNote == null)
			return "";
		noteText = fullNote;
		
		// Find the locked string
		Matcher mLocked = LOCKED_REGEX.matcher(fullNote);
		if (mLocked.find()) {
			// This note is locked
			locked = true;
			// Remove the occurrence of the locked string
			noteText = mLocked.replaceFirst("");
			Log.d("NoteAttr", fullNote + ".. is now .." + noteText);
		}
		
		Log.d("NoteAttr", "returning noteText.." + noteText);
		return noteText;
	}

	/**
	 * Calls getFullNote with argument being the noteText stored in this object.
	 * 
	 */
	public String getFullNote() {
		return getFullNote(noteText);
	}

	/**
	 * Given a note text (say from the editor), append the attributes contain
	 * here to the end of that text and return it.
	 * 
	 */
	public String getFullNote(String note) {
		String fullNote = "";
		if (note != null)
			fullNote += note;
		
		if (locked)
			fullNote += (NL + LOCKED);
		
		Log.d("NoteAttr", "returning full.." + fullNote);
		return fullNote;
	}
}
