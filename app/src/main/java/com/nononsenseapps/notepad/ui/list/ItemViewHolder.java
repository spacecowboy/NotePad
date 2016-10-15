package com.nononsenseapps.notepad.ui.list;

import android.database.Cursor;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.data.model.sql.Task;
import com.nononsenseapps.notepad.databinding.TasklistItemRichBinding;
import com.nononsenseapps.notepad.ui.common.NoteCheckBox;

class ItemViewHolder extends ViewHolder implements View.OnClickListener,
        View.OnLongClickListener {

    private static final String TAG = "ViewHolder";
    private final SelectedItemHandler selectedItemHandler;
    private TaskListFragment taskListFragment;
    private final TasklistItemRichBinding binding;
    private final NoteCheckBox checkbox;
    private final CompoundButton.OnCheckedChangeListener checkBoxListener;
    private final long listId;
    long id = -1;

    public ItemViewHolder(final TaskListFragment taskListFragment,
                          final TasklistItemRichBinding binding,
                          final long listId) {
        super(binding.getRoot());
        this.taskListFragment = taskListFragment;
        this.listId = listId;
        this.binding = binding;
        this.selectedItemHandler = taskListFragment.getSelectedItemHandler();
        checkbox = (NoteCheckBox) binding.cardSection.getRoot().findViewById(R.id.checkbox);

        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
        itemView.setLongClickable(true);

        binding.dragHandle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View v, final MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                    taskListFragment.getTouchHelper().startDrag(ItemViewHolder.this);
                }
                return false;
            }
        });

        checkBoxListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Task.setCompleted(ItemViewHolder.this.binding.getRoot().getContext(),
                        isChecked, ((NoteCheckBox) buttonView).getNoteId());
            }
        };
    }

    @Override
    public void onBind(final Cursor cursor) {
        id = cursor.getLong(0);

        // Title
        binding.cardSection.text.setMaxLines(taskListFragment.getRowCount());
        binding.cardSection.text
                .useSecondaryColor(!cursor.isNull(cursor.getColumnIndex(Task.Columns.COMPLETED)));
        binding.cardSection.text.setTextTitle(cursor.getString(cursor.getColumnIndex(Task.Columns.TITLE)));

        // Note
        // Only if task it not locked
        // or only one line
        if (cursor.getInt(9) != 1 && taskListFragment.getRowCount() > 1) {
            binding.cardSection.text.setTextRest(cursor.getString(cursor.getColumnIndex(Task.Columns.NOTE)));
        } else {
            binding.cardSection.text.setTextRest("");
        }

        // Checkbox
        checkbox.setOnCheckedChangeListener(null);
        checkbox.setChecked(!cursor.isNull(cursor.getColumnIndex(Task.Columns.COMPLETED)));
        checkbox.setNoteId(cursor.getLong(0));
        checkbox.setOnCheckedChangeListener(checkBoxListener);
        //holder.checkbox.setVisibility(mHideCheckbox ? View.GONE : View.VISIBLE);

        // Due
        if (cursor.isNull(cursor.getColumnIndex(Task.Columns.DUE))) {
            binding.cardSection.date.setVisibility(View.GONE);
        } else {
            binding.cardSection.date.setVisibility(View.VISIBLE);
            binding.cardSection.date.setTimeText(cursor.getLong(cursor.getColumnIndex(Task.Columns.DUE)));
        }

        if (taskListFragment.getSortType() != null && taskListFragment.getString(R.string.const_possubsort).equals(taskListFragment.getSortType())) {
            binding.dragHandle.setVisibility(View.VISIBLE);
            binding.cardSection.dragPadding.setVisibility(View.VISIBLE);
        } else {
            binding.dragHandle.setVisibility(View.GONE);
            binding.cardSection.dragPadding.setVisibility(View.GONE);
        }

        binding.getRoot().setActivated(selectedItemHandler.isItemSelected(id));
    }

    @Override
    public void onClick(final View v) {
        if (selectedItemHandler.isActiveSelectionMode()) {
            // same as long press
            onLongClick(v);
            return;
        }
        if (taskListFragment.getListener() != null && id > 0) {
            taskListFragment.getListener().openTask(Task.getUri(id), listId, v);
        }
    }

    @Override
    public boolean onLongClick(final View v) {
        if (id < 1) {
            return false;
        }

        selectedItemHandler.toggleSelection(id);
        binding.getRoot().setActivated(selectedItemHandler.isItemSelected(id));

        return true;
    }
}
