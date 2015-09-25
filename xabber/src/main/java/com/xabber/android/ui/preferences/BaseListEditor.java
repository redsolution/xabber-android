/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui.preferences;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

import com.xabber.android.R;
import com.xabber.android.ui.adapter.BaseListEditorAdapter;
import com.xabber.android.ui.dialog.ConfirmDialogBuilder;
import com.xabber.android.ui.dialog.ConfirmDialogListener;
import com.xabber.android.ui.dialog.DialogBuilder;
import com.xabber.android.ui.helper.BarPainter;
import com.xabber.android.ui.helper.ManagedListActivity;

/**
 * Provide possibility to add, edit and delete list items.
 *
 * @param <T>
 * @author alexander.ivanov
 */
public abstract class BaseListEditor<T> extends ManagedListActivity implements
        AdapterView.OnItemClickListener, ConfirmDialogListener {

    private static final String SAVED_ACTION_WITH = "com.xabber.android.ui.BaseListActivity.SAVED_ACTION_WITH";

    private static final int CONTEXT_MENU_DELETE_ID = 0x10;
    private static final int DIALOG_DELETE_ID = 0x100;
    protected BaseListEditorAdapter<T> adapter;
    private T actionWith;
    private BarPainter barPainter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing())
            return;
        onInflate(savedInstanceState);
        if (savedInstanceState != null)
            actionWith = getSavedValue(savedInstanceState, SAVED_ACTION_WITH);
        else
            actionWith = null;
        ListView listView = getListView();
        listView.setOnItemClickListener(this);
        registerForContextMenu(listView);
        adapter = createListAdapter();
        setListAdapter(adapter);
    }

    /**
     * Inflates layout.
     *
     * @param savedInstanceState
     */
    protected void onInflate(Bundle savedInstanceState) {
        setContentView(R.layout.list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);

        setSupportActionBar(toolbar);

        barPainter = new BarPainter(this, toolbar);
    }

    protected abstract T getSavedValue(Bundle bundle, String key);

    protected abstract void putSavedValue(Bundle bundle, String key,
                                          T actionWith);

    protected abstract int getOptionsMenuId();

    protected abstract int getAddActionId();

    protected abstract Intent getAddIntent();

    protected abstract Intent getEditIntent(T actionWith);

    protected abstract int getRemoveTextResourceId();

    protected abstract String getRemoveConfirmation(T actionWith);

    protected abstract void removeItem(T actionWith);

    protected abstract BaseListEditorAdapter<T> createListAdapter();

    protected T getActionWith() {
        return actionWith;
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.onChange();
        barPainter.setDefaultColor();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (actionWith != null)
            putSavedValue(outState, SAVED_ACTION_WITH, actionWith);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(getOptionsMenuId(), menu);
        menu.findItem(getAddActionId()).setIntent(getAddIntent());
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        actionWith = (T) getListView().getItemAtPosition(info.position);
        if (actionWith == null)
            // Add button
            return;
        onCreateContextMenu(menu, actionWith);
    }

    protected void onCreateContextMenu(ContextMenu menu, T actionWith) {
        menu.add(0, CONTEXT_MENU_DELETE_ID, 0,
                getString(getRemoveTextResourceId()));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (super.onContextItemSelected(item))
            return true;
        if (item.getItemId() == CONTEXT_MENU_DELETE_ID) {
            showDialog(DIALOG_DELETE_ID);
            return true;
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = super.onCreateDialog(id);
        if (dialog != null)
            return dialog;
        if (id == DIALOG_DELETE_ID)
            return new ConfirmDialogBuilder(this, DIALOG_DELETE_ID, this)
                    .setMessage(getRemoveConfirmation(actionWith)).create();
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        T actionWith = (T) parent.getAdapter().getItem(position);
        if (actionWith != null) {
            Intent intent = getEditIntent(actionWith);
            startActivity(intent);
        }
    }

    @Override
    public void onAccept(DialogBuilder dialogBuilder) {
        switch (dialogBuilder.getDialogId()) {
            case DIALOG_DELETE_ID:
                removeItem(actionWith);
                adapter.onChange();
                break;
        }
    }

    @Override
    public void onDecline(DialogBuilder dialogBuilder) {
    }

    @Override
    public void onCancel(DialogBuilder dialogBuilder) {
    }

}
