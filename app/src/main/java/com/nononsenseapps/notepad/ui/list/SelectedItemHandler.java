package com.nononsenseapps.notepad.ui.list;

import java.util.Set;
import java.util.TreeSet;

public class SelectedItemHandler {
    private final TreeSet<Long> selected = new TreeSet<>();

    public boolean isItemSelected(long id) {
        return selected.contains(id);
    }

    public void toggleSelection(long id) {
        if (selected.contains(id)) {
            selected.remove(id);
        } else {
            selected.add(id);
        }
    }

    public Set<Long> getSelected() {
        return selected;
    }
}
