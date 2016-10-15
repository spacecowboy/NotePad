package com.nononsenseapps.notepad.ui.list;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;

import java.util.Set;
import java.util.TreeSet;

public class SelectedItemHandler {
    private final TreeSet<Long> selected = new TreeSet<>();
    private final AppCompatActivity activity;
    private final ActionMode.Callback actionModeCallback;
    private ActionMode actionMode = null;

    public SelectedItemHandler(AppCompatActivity activity, ActionMode.Callback actionModeCallback) {
        this.activity = activity;
        this.actionModeCallback = actionModeCallback;
    }

    public boolean isItemSelected(long id) {
        return selected.contains(id);
    }

    public void toggleSelection(long id) {
        if (actionMode == null) {
            actionMode = activity.startSupportActionMode(actionModeCallback);
        }

        if (selected.contains(id)) {
            selected.remove(id);
        } else {
            selected.add(id);
        }

        if (selected.isEmpty()) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(selected.size()));
            actionMode.invalidate();
        }
    }

    public Set<Long> getSelected() {
        return selected;
    }

    public void clear() {
        selected.clear();
        if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }
    }

    public boolean isActiveSelectionMode() {
        return actionMode != null;
    }
}
