package com.xabber.android.presentation.mvp.contactlist;

import android.view.ContextMenu;
import android.view.View;

import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ButtonVO;

import java.util.List;

import eu.davidea.flexibleadapter.items.IFlexible;

/**
 * Created by valery.miller on 02.02.18.
 */

public interface ContactListView {

    void updateItems(List<IFlexible> items);
    void onContactClick(AbstractContact contact);
    void onItemContextMenu(int adapterPosition, ContextMenu menu);
    void onContactAvatarClick(int adapterPosition);
    void onAccountAvatarClick(int adapterPosition);
    void onAccountMenuClick(int adapterPosition, View view);
    void onButtonItemClick(ButtonVO buttonVO);
    void onContactListChanged(CommonState commonState, boolean hasContacts,
                              boolean hasVisibleContacts, boolean isFilterEnabled);
    void updateAccountsList();
}
