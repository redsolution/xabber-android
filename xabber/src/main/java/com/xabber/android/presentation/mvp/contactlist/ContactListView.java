package com.xabber.android.presentation.mvp.contactlist;

import java.util.List;

import eu.davidea.flexibleadapter.items.IFlexible;

/**
 * Created by valery.miller on 02.02.18.
 */

public interface ContactListView {

    void updateItems(List<IFlexible> items);
}
