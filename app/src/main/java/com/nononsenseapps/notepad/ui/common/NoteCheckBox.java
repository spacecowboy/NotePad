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

package com.nononsenseapps.notepad.ui.common;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;

/**
 * Convenience class to use in listviews. Bind the id to the checkbox inorder to
 * use a onCheckedChangeListener more easily.
 * 
 */
public class NoteCheckBox extends CheckBox {
	private long noteId = -1;

	public NoteCheckBox(Context context) {
		super(context);
	}

	public NoteCheckBox(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public NoteCheckBox(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public long getNoteId() {
		return noteId;
	}

	public void setNoteId(long noteId) {
		this.noteId = noteId;
	}
}
