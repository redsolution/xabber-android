package com.xabber.android.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.message.ChatContact;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.groupchat.GroupChat;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.CircleManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.GroupchatInviteContactActivity;
import com.xabber.android.ui.adapter.contactlist.AccountConfiguration;
import com.xabber.android.ui.adapter.contactlist.ContactListGroupUtils;
import com.xabber.android.ui.adapter.contactlist.GroupConfiguration;
import com.xabber.android.ui.fragment.contactListFragment.viewObjects.ContactVO;
import com.xabber.android.ui.fragment.contactListFragment.viewObjects.ExtContactVO;
import com.xabber.android.ui.fragment.contactListFragment.viewObjects.GroupVO;
import com.xabber.android.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class GroupchatInviteContactFragment extends Fragment implements FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener, ContactVO.ContactClickListener, GroupVO.GroupClickListener {

    public static final String LOG_TAG = GroupchatInviteContactFragment.class.getSimpleName();

    private static final String ARG_ACCOUNT = "com.xabber.android.ui.fragment.GroupchatInviteContactFragment.ARG_ACCOUNT";
    private static final String ARG_GROUPCHAT_CONTACT = "com.xabber.android.ui.fragment.GroupchatInviteContactFragment.ARG_GROUPCHAT_CONTACT";
    private static final String ARG_SELECTED_LIST = "com.xabber.android.ui.fragment.GroupchatInviteContactFragment.ARG_SELECTED_LIST";

    private AccountJid account;

    private RecyclerView contactList;
    private FlexibleAdapter<IFlexible> adapter;
    private List<IFlexible> items;
    private ArrayList<String> listOfSelectedContactJids;
    private EditText filterEt;
    private ImageView clearIv;
    private AppCompatTextView emptyListPlaceholder;

    private boolean modeSelectMultipleAccounts = false;

    private OnNumberOfSelectedInvitesChanged listener;

    public static GroupchatInviteContactFragment newInstance(AccountJid account, ContactJid groupchatContact) {
        GroupchatInviteContactFragment fragment = new GroupchatInviteContactFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_ACCOUNT, account);
        args.putParcelable(ARG_GROUPCHAT_CONTACT, groupchatContact);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof OnNumberOfSelectedInvitesChanged) {
            listener = (OnNumberOfSelectedInvitesChanged) getActivity();
        } else {
            throw new RuntimeException(getActivity() + " needs to implement "
                    + OnNumberOfSelectedInvitesChanged.class.getSimpleName());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            account = args.getParcelable(ARG_ACCOUNT);
        } else {
            requireActivity().finish();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_groupchat_invite_contact_list, container, false);

        filterEt = view.findViewById(R.id.search_et);
        filterEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                update();
                if (s != null && s.length() > 0)
                    clearIv.setVisibility(View.VISIBLE);
                else clearIv.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        clearIv = view.findViewById(R.id.groupchat_invites_clear_iv);
        clearIv.setOnClickListener(v -> {
            filterEt.setText("");
            update();
        });

        emptyListPlaceholder = view.findViewById(R.id.empty_placeholder);

        contactList = view.findViewById(R.id.contact_list);
        contactList.setLayoutManager(new LinearLayoutManager(getContext()));

        items = new ArrayList<>();
        adapter = new FlexibleAdapter<>(items, this, false);
        adapter.setDisplayHeadersAtStartUp(true);
        adapter.expandItemsAtStartUp();

        contactList.setAdapter(adapter);

        if (contactList.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) contactList.getItemAnimator()).setSupportsChangeAnimations(false);
        }

        if (savedInstanceState != null) {
            listOfSelectedContactJids = savedInstanceState.getStringArrayList(ARG_SELECTED_LIST);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList(ARG_SELECTED_LIST, new ArrayList<>(adapter.getSelectedPositions()));
    }

    public void update() {

        List<IFlexible> items = new ArrayList<>();

        final Collection<RosterContact> accountRosterContacts = RosterManager.getInstance().getAccountRosterContacts(account);

        final boolean showOffline = SettingsManager.contactsShowOffline();
        final boolean showGroups = SettingsManager.contactsShowGroups();
        final boolean showEmptyGroups = SettingsManager.contactsShowEmptyGroups();
        final Comparator<AbstractContact> comparator = SettingsManager.contactsOrder();

        final Map<String, GroupConfiguration> groups;
        final List<AbstractContact> contacts;
        final Map<AccountJid, AccountConfiguration> accountConfiguration = new TreeMap<>();
        accountConfiguration.put(account, null);
        final Map<AccountJid, Map<ContactJid, AbstractChat>> abstractChats = new TreeMap<>();

        for (AbstractChat abstractChat : ChatManager.getInstance().getChats()) {
            if ((abstractChat.isActive()) && account.equals(abstractChat.getAccount())) {
                final AccountJid account = abstractChat.getAccount();
                Map<ContactJid, AbstractChat> users = abstractChats.get(account);
                if (users == null) {
                    users = new TreeMap<>();
                    abstractChats.put(account, users);
                }
                users.put(abstractChat.getUser(), abstractChat);
            }
        }

        if (showGroups) {
            groups = new TreeMap<>();
            contacts = null;
        } else {
            groups = null;
            contacts = new ArrayList<>();
        }

        for (RosterContact rosterContact : accountRosterContacts) {
            if (!rosterContact.isEnabled()) {
                continue;
            }
            final boolean online = rosterContact.getStatusMode().isOnline();
            ContactListGroupUtils.addContact(rosterContact, online, accountConfiguration, groups,
                    contacts, false, showGroups, showOffline);
        }
        for (Map<ContactJid, AbstractChat> users : abstractChats.values()) {
            for (AbstractChat abstractChat : users.values()) {
                final AbstractContact abstractContact = new ChatContact(abstractChat);
                final String group = CircleManager.NO_GROUP;
                final boolean online = false;
                ContactListGroupUtils.addContact(abstractContact, group, online, accountConfiguration, groups, contacts,
                        false, showGroups);
            }
        }

        if (showGroups) {
            createContactListWithGroups(items, showEmptyGroups, groups, comparator);
        } else {
            createContactList(items, contacts, comparator);
        }

        this.items.clear();
        this.items.addAll(items);
        adapter.updateDataSet(this.items);

        if (this.items.size() == 0) emptyListPlaceholder.setVisibility(View.VISIBLE);
        else emptyListPlaceholder.setVisibility(View.GONE);

        checkForRestore();
        adapter.notifyDataSetChanged();
    }

    private void checkForRestore() {
        if (listOfSelectedContactJids != null) {
            adapter.clearSelection();
            for (String contactJid : listOfSelectedContactJids) {
                adapter.addSelection(getPositionByContactJid(contactJid));
            }
            listener.onInviteCountChange(adapter.getSelectedItemCount());
        }
    }

    private int getPositionByContactJid(String contactJid){
        for (IFlexible item : adapter.getCurrentItems()){
            if (item instanceof ContactVO && ((ContactVO)item).getContactJid().toString().equals(contactJid))
                return  adapter.getGlobalPositionOf(item);
        }
        return 0;
    }

    public List<ContactJid> getSelectedContacts() {
        ArrayList<ContactJid> selectedContacts = new ArrayList<>();
        Set<Integer> selectedPositions = adapter.getSelectedPositionsAsSet();
        for (Integer position : selectedPositions) {
            Object item = adapter.getItem(position);
            if (item instanceof ContactVO) {
                selectedContacts.add(((ContactVO) item).getContactJid());
            }
        }
        return selectedContacts;
    }

    private void createContactListWithGroups(List<IFlexible> items, boolean showEmptyGroups,
                                             Map<String, GroupConfiguration> groups,
                                             Comparator<AbstractContact> comparator) {
        for (GroupConfiguration rosterConfiguration : groups.values()) {
            if (showEmptyGroups || !rosterConfiguration.isEmpty()) {
                GroupVO group = GroupVO.convert(rosterConfiguration, false, this);
                rosterConfiguration.sortAbstractContacts(comparator);

                for (AbstractContact contact : rosterConfiguration.getAbstractContacts()) {
                    boolean isGroupchat = ChatManager.getInstance().getChat(contact.getAccount(),
                            contact.getUser()) instanceof GroupChat;
                    boolean isServer = contact.getUser().getJid().isDomainBareJid();

                    if (filterEt.getText() != null && !filterEt.getText().toString().isEmpty()){
                        String filter = filterEt.getText().toString();
                        String transliteratedFilterString = StringUtils.translitirateToLatin(filter);

                        String contactName = RosterManager.getInstance()
                                .getBestContact(contact.getAccount(), contact.getUser())
                                .getName()
                                .toLowerCase();

                        if (contact.getUser().toString().contains(filter)
                                || contact.getUser().toString().contains(transliteratedFilterString)
                                || contactName.contains(filter)
                                || contactName.contains(transliteratedFilterString)){
                            if (!isGroupchat && !isServer)
                                group.addSubItem(SettingsManager.contactsShowMessages()
                                        ? ExtContactVO.convert(contact, this)
                                        : ContactVO.convert(contact, this));
                        }
                    } else if (!isGroupchat && !isServer)
                        group.addSubItem(SettingsManager.contactsShowMessages()
                                ? ExtContactVO.convert(contact, this)
                                : ContactVO.convert(contact, this));
                }
                if (group.getSubItems() != null && group.getSubItems().size() > 0)
                    items.add(group);
            }
        }
    }

    private void createContactList(List<IFlexible> items, List<AbstractContact> contacts,
                                   Comparator<AbstractContact> comparator) {
        Collections.sort(contacts, comparator);
        items.addAll(SettingsManager.contactsShowMessages()
                ? ExtContactVO.convert(contacts, this)
                : ContactVO.convert(contacts, this));
    }

    public void cancelSelection() {
        modeSelectMultipleAccounts = false;
        listOfSelectedContactJids.clear();
        adapter.clearSelection();
        update();
    }

    @Override
    public boolean onItemClick(View view, int position) {
        if (modeSelectMultipleAccounts){

            if (adapter.getItem(position) instanceof ContactVO) {
                ContactVO clickedItem = (ContactVO) adapter.getItem(position);
                adapter.toggleSelection(position);
                ContactJid contactJid = clickedItem.getContactJid();
                listener.onInviteCountChange(adapter.getSelectedItemCount());

                if (listOfSelectedContactJids == null)
                    listOfSelectedContactJids = new ArrayList<>();

                if (!listOfSelectedContactJids.contains(contactJid.toString())){
                    if (adapter.isSelected(position)) listOfSelectedContactJids.add(contactJid.toString());
                    else listOfSelectedContactJids.remove(contactJid.toString());
                }
            }

            adapter.notifyItemChanged(position);

            if (adapter.getSelectedItemCount() == 0) modeSelectMultipleAccounts = false;

        } else {

            if (adapter.getItem(position) instanceof ContactVO) {
                ((GroupchatInviteContactActivity)getActivity()).openInvitationDialogForContact(
                        ((ContactVO)adapter.getItem(position)).getContactJid());
            }

        }

        return true;
    }

    @Override
    public void onItemLongClick(int position) {
        if (!modeSelectMultipleAccounts) modeSelectMultipleAccounts = true;

        if (adapter.getItem(position) instanceof ContactVO) {

            ContactVO clickedItem = (ContactVO) adapter.getItem(position);
            adapter.toggleSelection(position);
            ContactJid contactJid = clickedItem.getContactJid();
            listener.onInviteCountChange(adapter.getSelectedItemCount());

            if (listOfSelectedContactJids == null)
                listOfSelectedContactJids = new ArrayList<>();

            if (!listOfSelectedContactJids.contains(contactJid.toString())){
                if (adapter.isSelected(position)) listOfSelectedContactJids.add(contactJid.toString());
                else listOfSelectedContactJids.remove(contactJid.toString());
            }

        }
        if (adapter.getSelectedItemCount() == 0) modeSelectMultipleAccounts = false;
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onContactAvatarClick(int adapterPosition) {
        adapter.toggleSelection(adapterPosition);
        listener.onInviteCountChange(adapter.getSelectedItemCount());
    }

    @Override
    public void onContactCreateContextMenu(int adapterPosition, ContextMenu menu) {
    }

    @Override
    public void onContactButtonClick(int adapterPosition) {
    }

    @Override
    public void onGroupCreateContextMenu(int adapterPosition, ContextMenu menu) {
    }

    public interface OnNumberOfSelectedInvitesChanged {
        void onInviteCountChange(int newCount);
    }
}
