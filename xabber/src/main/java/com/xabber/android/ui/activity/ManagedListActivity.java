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
package com.xabber.android.ui.activity;

import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * Base class for all ListActivities.
 * <p/>
 * Adds custom activity logic.
 *
 * @author alexander.ivanov
 */
public abstract class ManagedListActivity extends ManagedActivity {
    public ListView getListView() {
        return (ListView) findViewById(android.R.id.list);
    }

    public void setListAdapter(ListAdapter adapter) {
        getListView().setAdapter(adapter);
    }

}
