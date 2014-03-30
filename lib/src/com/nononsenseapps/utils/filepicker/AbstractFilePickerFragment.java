/*
 * Copyright (c) 2014 Jonas Kalderstam.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nononsenseapps.utils.filepicker;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.nononsenseapps.utils.R;

import java.util.List;

/**
 * A fragment representing a list of Files.
 * <p/>
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFilePickedListener}
 * interface.
 */
public abstract class AbstractFilePickerFragment<T> extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<T>> {

    public static final String KEY_START_PATH = "KEY_START_PATH";
    private static final String KEY_CURRENT_PATH = "KEY_START_PATH";
    protected T currentPath = null;
    private OnFilePickedListener mListener;
    private ArrayAdapter<T> adapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AbstractFilePickerFragment() {
    }

    /**
     * Set before making the fragment visible.
     *
     * @param startPath
     */
    public void setStartPath(final String startPath) {
        if (startPath != null) {
            Bundle b = new Bundle();
            b.putString(KEY_START_PATH, startPath);
            setArguments(b);
        }
    }

    /**
     * Parse attributes during inflation from a view hierarchy into the
     * arguments we handle.
     */
    /*
    @Override public void onInflate(Activity activity, AttributeSet attrs,
                                    Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);

        TypedArray a = activity.obtainStyledAttributes(attrs,
                R.styleable.FilePickerArguments);
        if (a != null) {
            if (a.hasValue(R.styleable.FilePickerArguments_start_path)) {
                Bundle b = new Bundle();
                b.putString(KEY_START_PATH,
                        a.getString(R.styleable
                                .FilePickerArguments_start_path));
                setArguments(b);
            }
            a.recycle();
        }
    }*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            currentPath = getPath(savedInstanceState.getString
                    (KEY_CURRENT_PATH));
        } else if (getArguments() != null && getArguments().containsKey
                (KEY_START_PATH)) {
            currentPath = getPath(getArguments().getString(KEY_START_PATH));
        }
        else {
            currentPath = getRoot();
        }

        getLoaderManager().restartLoader(0, null, this);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_filepicker, null);

        view.findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mListener != null) {
                    mListener.onCancelled();
                }
            }
        });

        view.findViewById(R.id.button_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mListener != null) {
                    mListener.onFilePicked(currentPath);
                }
            }
        });

        View header = inflater.inflate(R.layout.filepicker_listheader, null);
        ((ListView) view.findViewById(android.R.id.list)).addHeaderView
                (header);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.filepicker, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFilePickedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle b) {
        b.putString(KEY_CURRENT_PATH, currentPath.toString());
    }

    /**
     * Return true if the path is a directory and not a file.
     * @param path
     */
    protected abstract boolean isDir(final T path);

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (position == 0) {
            // go up
            currentPath = goUp(currentPath);
        } else {
            currentPath = (T) getListAdapter().getItem(position - 1);
        }

        if (isDir(currentPath)) {
            getLoaderManager().restartLoader(0, null, this);
        } else {
            // TODO mark file
        }
    }

    /**
     * Return the path to the parent directory. Should return the root if
     * from is root.
     * @param from
     */
    protected abstract T goUp(final T from);

    /**
     * Convert the path to the type used.
     *
     * @param path
     */
    protected abstract T getPath(final String path);

    /**
     * Get the root path (lowest allowed).
     */
    protected abstract T getRoot();

    /**
     * Try to create a designated directory.
     * @param path Path to directory to create
     * @return true on success. false if failed.
     */
    protected abstract boolean createDir(final T path);

    /**
     * Get a loader that lists the files in the current path,
     * and monitors changes.
     */
    protected abstract Loader<List<T>> getLoader();

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<List<T>> onCreateLoader(final int id, final Bundle args) {
        Log.d("JONAS", "getting loader");
        return getLoader();
    }

    /**
     * Called when a previously created loader has finished its load.  Note
     * that normally an application is <em>not</em> allowed to commit fragment
     * transactions while in this call, since it can happen after an
     * activity's state is saved.  See {@link android.app.FragmentManager#beginTransaction()
     * FragmentManager.openTransaction()} for further discussion on this.
     * <p/>
     * <p>This function is guaranteed to be called prior to the release of
     * the last data that was supplied for this Loader.  At this point
     * you should remove all use of the old data (since it will be released
     * soon), but should not do your own release of the data since its Loader
     * owns it and will take care of that.  The Loader will take care of
     * management of its data so you don't have to.  In particular:
     * <p/>
     * <ul>
     * <li> <p>The Loader will monitor for changes to the data, and report
     * them to you through new calls here.  You should not monitor the
     * data yourself.  For example, if the data is a {@link android.database.Cursor}
     * and you place it in a {@link android.widget.CursorAdapter}, use
     * the {@link android.widget.CursorAdapter#CursorAdapter(android.content.Context,
     * android.database.Cursor, int)} constructor <em>without</em> passing
     * in either {@link android.widget.CursorAdapter#FLAG_AUTO_REQUERY}
     * or {@link android.widget.CursorAdapter#FLAG_REGISTER_CONTENT_OBSERVER}
     * (that is, use 0 for the flags argument).  This prevents the CursorAdapter
     * from doing its own observing of the Cursor, which is not needed since
     * when a change happens you will get a new Cursor throw another call
     * here.
     * <li> The Loader will release the data once it knows the application
     * is no longer using it.  For example, if the data is
     * a {@link android.database.Cursor} from a {@link android.content.CursorLoader},
     * you should not call close() on it yourself.  If the Cursor is being placed in a
     * {@link android.widget.CursorAdapter}, you should use the
     * {@link android.widget.CursorAdapter#swapCursor(android.database.Cursor)}
     * method so that the old Cursor is not closed.
     * </ul>
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(final Loader<List<T>> loader,
                               final List<T> data) {
        Log.d("JONAS", "loader finished");
        if (adapter == null) {
            adapter = new ArrayAdapter<T>(getActivity(),
                    R.layout.filepicker_listitem);
        } else {
            adapter.clear();
        }
        adapter.addAll(data);
        setListAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(final Loader<List<T>> loader) {
        Log.d("JONAS", "loader reset");
        setListAdapter(null);
        adapter = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFilePickedListener<T> {
        public void onFilePicked(T file);

        public void onCancelled();
    }

}
