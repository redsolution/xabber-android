package com.xabber.android.presentation.ui.contactlist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.presentation.mvp.contactlist.ContactListPresenter;
import com.xabber.android.presentation.mvp.contactlist.ContactListView;
import com.xabber.android.presentation.ui.contactlist.viewobjects.AccountVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ButtonVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ContactVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.GroupVO;
import com.xabber.android.ui.activity.AccountActivity;
import com.xabber.android.ui.activity.AccountAddActivity;
import com.xabber.android.ui.activity.ContactActivity;
import com.xabber.android.ui.activity.ContactAddActivity;
import com.xabber.android.ui.activity.ContactEditActivity;
import com.xabber.android.ui.activity.ContactListActivity;
import com.xabber.android.ui.adapter.contactlist.ContactListState;
import com.xabber.android.ui.helper.ContextMenuHelper;
import com.xabber.android.ui.widget.bottomnavigation.AccountShortcutAdapter;
import com.xabber.android.ui.widget.bottomnavigation.AccountShortcutVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

/**
 * Created by valery.miller on 02.02.18.
 */

public class ContactListFragment extends Fragment implements ContactListView,
        FlexibleAdapter.OnStickyHeaderChangeListener, FlexibleAdapter.OnItemClickListener,
        View.OnClickListener {

    public static final String ACCOUNT_JID = "account_jid";

    private ContactListPresenter presenter;
    private ContactListFragmentListener contactListFragmentListener;

    private RecyclerView recyclerView;
    private FlexibleAdapter<IFlexible> adapter;
    private List<IFlexible> items;
    private CoordinatorLayout coordinatorLayout;
    private LinearLayoutManager linearLayoutManager;
    private AccountShortcutAdapter accountShortcutAdapter;
    private RecyclerView accountsRecyclerView;
    private ArrayList<AccountShortcutVO> accountShortcutVOArrayList = new ArrayList<>();
    private ArrayList<AccountJid> accountsJidList;
    /**
     * View with information shown on empty contact list.
     */
    private View infoView;

    /**
     * Image view with connected icon.
     */
    private View connectedView;

    /**
     * Image view with disconnected icon.
     */
    private ImageView disconnectedView;

    /**
     * View with help text.
     */
    private TextView textView;

    /**
     * Button to apply help text.
     */
    private Button buttonView;

    /**
     * Animation for disconnected view.
     */
    private Animation animation;

    public interface ContactListFragmentListener {
        void onContactClick(AbstractContact contact);
        void onContactListChange(CommonState commonState);
        void onManageAccountsClick();
    }

    public static ContactListFragment newInstance(@Nullable AccountJid account) {
        ContactListFragment fragment = new ContactListFragment();
        Bundle args = new Bundle();
        if (account != null)
            args.putSerializable(ACCOUNT_JID, account);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        contactListFragmentListener = (ContactListFragmentListener) activity;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        contactListFragmentListener = (ContactListFragmentListener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        contactListFragmentListener = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact_list_new, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.coordinatorLayout);

        infoView = view.findViewById(R.id.info);
        connectedView = infoView.findViewById(R.id.connected);
        disconnectedView = infoView.findViewById(R.id.disconnected);
        //ColorManager.setGrayScaleFilter(disconnectedView);
        textView = (TextView) infoView.findViewById(R.id.text);
        buttonView = (Button) infoView.findViewById(R.id.button);
        animation = AnimationUtils.loadAnimation(getActivity(), R.anim.connection);

        items = new ArrayList<>();
        adapter = new FlexibleAdapter<>(items, null, false);

        adapter.setStickyHeaders(true);
        adapter.setDisplayHeadersAtStartUp(true);
        recyclerView.setAdapter(adapter);

        adapter.setSwipeEnabled(true);
        adapter.expandItemsAtStartUp();
        adapter.addListener(this);

        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        /*
        Setting accounts buttons-avatars-list
         */
        accountsRecyclerView = view.findViewById(R.id.accounts_list_in_contact_list_recycler);
        accountsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        updateAccountsList();
        return view;
    }

    public void scrollToTop(){
        recyclerView.smoothScrollToPosition(0);
    }

    /**
    Setup bottom accounts list with avatars.
     */
    public void updateAccountsList(){
        accountsJidList = new ArrayList<>(AccountManager.getInstance().getEnabledAccounts());
        Collections.sort(accountsJidList);
        if (accountsJidList.size() > 1){
            accountShortcutVOArrayList.clear();
            accountShortcutVOArrayList.addAll(AccountShortcutVO.convert(accountsJidList));
            accountShortcutAdapter = new AccountShortcutAdapter(accountShortcutVOArrayList, getActivity(), this);
            accountsRecyclerView.setAdapter(accountShortcutAdapter);
            accountsRecyclerView.setVisibility(View.VISIBLE);
            accountsRecyclerView.setOnClickListener(this);
        }
        else accountsRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter = ContactListPresenter.getInstance();
        ((ContactListActivity)getActivity()).setStatusBarColor();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.bindView(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.unbindView();
    }

    @Override
    public void updateItems(List<IFlexible> items) {
        this.items.clear();
        this.items.addAll(items);
        adapter.updateDataSet(this.items);
    }

    /**
     * Callback. When new header becomes sticky - change color of status bar.
     */
    @Override
    public void onStickyHeaderChange(int newPosition, int oldPosition) {
        if (newPosition > -1 && adapter.getItemCount() > newPosition) {
            IFlexible item = adapter.getItem(newPosition);
            if (item instanceof AccountVO)
                ((ContactListActivity)getActivity()).setStatusBarColor(((AccountVO)item).getAccountJid()); // account color
            else ((ContactListActivity)getActivity()).setStatusBarColor(); // main color
        }
    }

    @Override
    public boolean onItemClick(int position) {
        adapter.notifyItemChanged(position);
        presenter.onItemClick(adapter.getItem(position));
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.avatarView){
            int position = accountsRecyclerView.getChildLayoutPosition(v);
            AccountJid clickedAccountJid = accountsJidList.get(position);
            scrollToAccount(clickedAccountJid);
            if (Build.VERSION.SDK_INT > 20)
                ((ContactListActivity)getActivity()).setStatusBarColor(clickedAccountJid);
        }
    }

    @Override
    public void onContactClick(AbstractContact contact) {
        contactListFragmentListener.onContactClick(contact);
    }

    @Override
    public void onItemContextMenu(int adapterPosition, ContextMenu menu) {
        IFlexible item = adapter.getItem(adapterPosition);
        if (item != null && item instanceof ContactVO) {
            AccountJid accountJid = ((ContactVO) item).getAccountJid();
            UserJid userJid = ((ContactVO) item).getUserJid();
            AbstractContact abstractContact = RosterManager.getInstance().getAbstractContact(accountJid, userJid);
            ContextMenuHelper.createContactContextMenu(getActivity(), presenter, abstractContact, menu);
            return;
        }

        if (item != null && item instanceof GroupVO) {
            AccountJid accountJid = ((GroupVO) item).getAccountJid();
            ContextMenuHelper.createGroupContextMenu(getActivity(), presenter, accountJid,
                    ((GroupVO) item).getGroupName(), menu);
            return;
        }
    }

    @Override
    public void onContactAvatarClick(int adapterPosition) {
        IFlexible item = adapter.getItem(adapterPosition);
        if (item != null && item instanceof ContactVO) {
            Intent intent;
            AccountJid accountJid = ((ContactVO) item).getAccountJid();
            UserJid userJid = ((ContactVO) item).getUserJid();
            if (MUCManager.getInstance().hasRoom(accountJid, userJid)) {
                intent = ContactActivity.createIntent(getActivity(), accountJid, userJid);
            } else {
                intent = ContactEditActivity.createIntent(getActivity(), accountJid, userJid);
            }
            getActivity().startActivity(intent);
        }
    }

    @Override
    public void onAccountAvatarClick(int adapterPosition) {
        IFlexible item = adapter.getItem(adapterPosition);
        if (item != null && item instanceof AccountVO) {
            getActivity().startActivity(AccountActivity.createIntent(getActivity(),
                    ((AccountVO) item).getAccountJid()));
        }
    }

    @Override
    public void onAccountMenuClick(int adapterPosition, View view) {
        IFlexible item = adapter.getItem(adapterPosition);
        if (item != null && item instanceof AccountVO) {
            PopupMenu popup = new PopupMenu(getActivity(), view);
            popup.inflate(R.menu.item_account_group);
            ContextMenuHelper.setUpAccountMenu(getActivity(), presenter, ((AccountVO) item).getAccountJid(), popup.getMenu());
            popup.show();
        }
    }

    @Override
    public void onButtonItemClick(ButtonVO buttonVO) {
        if (buttonVO.getAction().equals(ButtonVO.ACTION_ADD_CONTACT)) {
            getActivity().startActivity(ContactAddActivity.createIntent(getActivity()));
        }
    }

    @Override
    public void onContactListChanged(CommonState commonState, boolean hasContacts,
                                     boolean hasVisibleContacts, boolean isFilterEnabled) {

        if (contactListFragmentListener != null)
            contactListFragmentListener.onContactListChange(commonState);

        if (hasVisibleContacts) {
            infoView.setVisibility(View.GONE);
            disconnectedView.clearAnimation();
            return;
        }
        infoView.setVisibility(View.VISIBLE);
        final int text;
        final int button;
        final ContactListState state;
        final View.OnClickListener listener;
        if (isFilterEnabled) {
            if (commonState == CommonState.online) {
                state = ContactListState.online;
            } else if (commonState == CommonState.roster || commonState == CommonState.connecting) {
                state = ContactListState.connecting;
            } else {
                state = ContactListState.offline;
            }
            text = R.string.application_state_nobody_by_filter;
            button = 0;
            listener = null;
        } else if (hasContacts) {
            state = ContactListState.online;
            text = R.string.application_state_no_online;
            button = R.string.application_action_no_online;
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SettingsManager.setContactsShowOffline(true);
                    presenter.updateContactList();
                }
            };
        } else if (commonState == CommonState.online) {
            state = ContactListState.online;
            text = R.string.application_state_no_contacts;
            button = R.string.application_action_no_contacts;
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(ContactAddActivity.createIntent(getActivity()));
                }
            };
        } else if (commonState == CommonState.roster) {
            state = ContactListState.connecting;
            text = R.string.application_state_roster;
            button = 0;
            listener = null;
        } else if (commonState == CommonState.connecting) {
            state = ContactListState.connecting;
            text = R.string.application_state_connecting;
            button = 0;
            listener = null;
        } else if (commonState == CommonState.waiting) {
            state = ContactListState.offline;
            text = R.string.application_state_waiting;
            button = R.string.application_action_waiting;
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ConnectionManager.getInstance().connectAll();
                }
            };
        } else if (commonState == CommonState.offline) {
            state = ContactListState.offline;
            text = R.string.application_state_offline;
            button = R.string.application_state_offline;
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AccountManager.getInstance().setStatus(
                            StatusMode.available, null);
                }
            };
        } else if (commonState == CommonState.disabled) {
            state = ContactListState.offline;
            text = R.string.application_state_disabled;
            button = R.string.application_action_disabled;
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    contactListFragmentListener.onManageAccountsClick();
                }
            };
        } else if (commonState == CommonState.empty) {
            state = ContactListState.offline;
            text = R.string.application_state_empty;
            button = R.string.application_action_empty;
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(AccountAddActivity.createIntent(getActivity()));
                }
            };
        } else {
            throw new IllegalStateException();
        }

        // set up image and animation in placeholder
        if (state == ContactListState.offline) {
            connectedView.setVisibility(View.INVISIBLE);
            disconnectedView.setVisibility(View.VISIBLE);
            disconnectedView.clearAnimation();
        } else if (state == ContactListState.connecting) {
            connectedView.setVisibility(View.VISIBLE);
            disconnectedView.setVisibility(View.VISIBLE);
            if (disconnectedView.getAnimation() == null) {
                disconnectedView.startAnimation(animation);
            }
        } else {
            connectedView.setVisibility(View.VISIBLE);
            disconnectedView.setVisibility(View.INVISIBLE);
            disconnectedView.clearAnimation();
        }

        // set up text in placeholder
        textView.setText(text);

        // set up button in placeholder
        if (button == 0) {
            buttonView.setVisibility(View.GONE);
        } else {
            buttonView.setVisibility(View.VISIBLE);
            buttonView.setText(button);
        }
        buttonView.setOnClickListener(listener);
    }

    /**
     * Scroll contact list to specified account.
     *
     * @param account
     */
    public void scrollToAccount(AccountJid account) {

        long count = adapter.getItemCount();
        for (int position = 0; position < (int) count; position++) {
            Object itemAtPosition = adapter.getItem(position);
            if (itemAtPosition != null && itemAtPosition instanceof AccountVO
                    && ((AccountVO)itemAtPosition).getAccountJid().equals(account)) {
                scrollTo(position);
                break;
            }
        }
    }

    /**
     * Scroll to the top of contact list.
     */
    public void scrollTo(int position) {
        if (linearLayoutManager != null)
            linearLayoutManager.scrollToPositionWithOffset(position, 0);
    }
}
