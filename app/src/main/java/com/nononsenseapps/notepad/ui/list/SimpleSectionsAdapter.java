package com.nononsenseapps.notepad.ui.list;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.databinding.TasklistHeaderBinding;
import com.nononsenseapps.notepad.databinding.TasklistItemRichBinding;

class SimpleSectionsAdapter extends RecyclerView.Adapter<ViewHolder> {
    final static int itemType = 0;
    final static int headerType = 1;
    private static final String TAG = "SimpleSectionsAdapter";
    private TaskListFragment taskListFragment;
    final SharedPreferences prefs;
    final Context context;
    Cursor cursor = null;

    public SimpleSectionsAdapter(TaskListFragment taskListFragment, Context context) {
        super();
        this.taskListFragment = taskListFragment;
        setHasStableIds(true);
        this.context = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);

    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case headerType:
                return new HeaderViewHolder(taskListFragment, (TasklistHeaderBinding) DataBindingUtil.inflate(LayoutInflater.from(context),
                        R.layout.tasklist_header, parent, false));
            case itemType:
            default:
                return new ItemViewHolder(taskListFragment,
                        (TasklistItemRichBinding) DataBindingUtil.inflate(LayoutInflater.from(context),
                                R.layout.tasklist_item_rich, parent, false), taskListFragment.getListId());
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if (!cursor.moveToPosition(position)) {
            return;
        }
        holder.onBind(cursor);
    }

    @Override
    public int getItemViewType(final int position) {
        // If the id is invalid, it's a header
        if (getItemId(position) < 1) {
            return headerType;
        } else {
            return itemType;
        }
    }

    @Override
    public long getItemId(int position) {
        cursor.moveToPosition(position);
        return cursor.getLong(0);
    }

    @Override
    public int getItemCount() {
        if (cursor == null) {
            return 0;
        } else {
            return cursor.getCount();
        }
    }

    public void swapCursor(final Cursor cursor) {
        this.cursor = cursor;
        notifyDataSetChanged();
    }

    public Cursor getCursor(final int position) {
        if (cursor != null) {
            cursor.moveToPosition(position);
        }
        return cursor;
    }
}
