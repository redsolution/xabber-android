package com.xabber.android.presentation.mvp.contactlist;

import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.presentation.ui.contactlist.viewobjects.AccountVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ContactVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.GroupVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ToolbarVO;
import com.xabber.android.ui.adapter.contactlist.AccountConfiguration;
import com.xabber.android.ui.adapter.contactlist.ContactListAdapter;
import com.xabber.android.ui.adapter.contactlist.GroupConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import eu.davidea.flexibleadapter.items.IFlexible;

/**
 * Created by valery.miller on 02.02.18.
 */

public class ContactListPresenter {

    private static ContactListPresenter instance;
    private ContactListView view;

    private String filterString = null;
    private ContactListAdapter.ChatListState currentChatsState = ContactListAdapter.ChatListState.recent;

    public static ContactListPresenter getInstance() {
        if (instance == null) instance = new ContactListPresenter();
        return instance;
    }

    public void onLoadContactList(ContactListView view) {
        this.view = view;
        List<IFlexible> items = new ArrayList<>();

        items.add(new ToolbarVO());

        for (AccountJid accountJid : AccountManager.getInstance().getEnabledAccounts()) {
            items.add(AccountVO.convert(new AccountConfiguration(accountJid, GroupManager.IS_ACCOUNT,
                    GroupManager.getInstance())));
            items.add(GroupVO.convert(new GroupConfiguration(accountJid, "No groups", GroupManager.getInstance())));
        }

        final Collection<RosterContact> allRosterContacts = RosterManager.getInstance().getAllContacts();
        items.addAll(ContactVO.convert(allRosterContacts));

        this.view.updateItems(items);
    }

}
