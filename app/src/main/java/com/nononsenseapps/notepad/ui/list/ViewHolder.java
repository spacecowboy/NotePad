package com.nononsenseapps.notepad.ui.list;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.View;

abstract class ViewHolder extends RecyclerView.ViewHolder {
    ViewHolder(View itemView) {
        super(itemView);
    }

    public abstract void onBind(Cursor cursor);
}
