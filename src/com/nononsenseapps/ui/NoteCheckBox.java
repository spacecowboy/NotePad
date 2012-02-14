package com.nononsenseapps.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;

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
