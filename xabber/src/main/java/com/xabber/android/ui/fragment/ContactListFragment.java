package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.message.NewMessageEvent;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.ui.activity.AccountAddActivity;
import com.xabber.android.ui.activity.ContactAddActivity;
import com.xabber.android.ui.activity.ManagedActivity;
import com.xabber.android.ui.adapter.AccountActionButtonsAdapter;
import com.xabber.android.ui.adapter.UpdatableAdapter;
import com.xabber.android.ui.adapter.contactlist.AccountConfiguration;
import com.xabber.android.ui.adapter.contactlist.ContactListAdapter;
import com.xabber.android.ui.adapter.contactlist.ContactListAdapter.ContactListAdapterListener;
import com.xabber.android.ui.adapter.contactlist.ContactListState;
import com.xabber.android.ui.adapter.contactlist.GroupConfiguration;
import com.xabber.android.ui.color.AccountPainter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.helper.ContextMenuHelper;
import com.xabber.android.ui.activity.AccountListActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collection;

public class ContactListFragment extends Fragment implements OnAccountChangedListener,
        OnContactChangedListener, ContactListAdapterListener, View.OnClickListener {

    private ContactListAdapter adapter;

    private RecyclerView recyclerView;

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
    private View disconnectedView;

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
    private AccountActionButtonsAdapter accountActionButtonsAdapter;
    private View scrollToChatsActionButtonContainer;
    private View actionButtonsContainer;
    private FloatingActionButton scrollToChatsActionButton;
    private AccountPainter accountPainter;

    private ContactListFragmentListener contactListFragmentListener;
    private LinearLayoutManager linearLayoutManager;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        contactListFragmentListener = (ContactListFragmentListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact_list, container, false);

        // to avoid strange bug on some 4.x androids
        view.setBackgroundColor(ColorManager.getInstance().getContactListBackgroundColor());

        recyclerView = (RecyclerView) view.findViewById(R.id.contact_list_recycler_view);
        registerForContextMenu(recyclerView);
        adapter = new ContactListAdapter(getActivity(), this);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);
        infoView = view.findViewById(R.id.info);
        connectedView = infoView.findViewById(R.id.connected);
        disconnectedView = infoView.findViewById(R.id.disconnected);
        textView = (TextView) infoView.findViewById(R.id.text);
        buttonView = (Button) infoView.findViewById(R.id.button);
        animation = AnimationUtils.loadAnimation(getActivity(), R.anim.connection);

        accountActionButtonsAdapter = new AccountActionButtonsAdapter(getActivity(),
                this, (LinearLayout) view.findViewById(R.id.account_action_buttons));
        accountActionButtonsAdapter.onChange();

        actionButtonsContainer = view.findViewById(R.id.account_action_buttons_container);

        scrollToChatsActionButtonContainer = view.findViewById(R.id.fab_up_container);
        scrollToChatsActionButtonContainer.setOnClickListener(this);
        scrollToChatsActionButtonContainer.setVisibility(View.GONE);

        scrollToChatsActionButton = (FloatingActionButton) view.findViewById(R.id.fab_up);

        accountPainter = ColorManager.getInstance().getAccountPainter();
        scrollToChatsActionButton.setColorNormal(accountPainter.getDefaultMainColor());
        scrollToChatsActionButton.setColorPressed(accountPainter.getDefaultDarkColor());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        adapter.onChange();
        scrollToChatsActionButton.setColorNormal(accountPainter.getDefaultMainColor());
        scrollToChatsActionButton.setColorPressed(accountPainter.getDefaultDarkColor());

        if (SettingsManager.contactsShowPanel()) {
            actionButtonsContainer.setVisibility(View.VISIBLE);
        } else {
            actionButtonsContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterListeners();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        contactListFragmentListener = null;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Object itemAtPosition = adapter.getItem(info.position);
        if (itemAtPosition instanceof AbstractContact) {
            ContextMenuHelper.createContactContextMenu(
                    (ManagedActivity) getActivity(), adapter, (AbstractContact) itemAtPosition, menu);
        } else if (itemAtPosition instanceof AccountConfiguration) {
            AccountConfiguration accountConfiguration = (AccountConfiguration) itemAtPosition;
            ContextMenuHelper.createAccountContextMenu(
                    (ManagedActivity) getActivity(), adapter, accountConfiguration.getAccount(), menu);
        } else if (itemAtPosition instanceof GroupConfiguration) {
            GroupConfiguration groupConfiguration = (GroupConfiguration) itemAtPosition;
            ContextMenuHelper.createGroupContextMenu((ManagedActivity) getActivity(), adapter,
                    groupConfiguration.getAccount(), groupConfiguration.getGroup(), menu);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        adapter.refreshRequest();
        scrollToChatsActionButton.setColorNormal(accountPainter.getDefaultMainColor());
        scrollToChatsActionButton.setColorPressed(accountPainter.getDefaultDarkColor());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewMessageEvent(NewMessageEvent event) {
        adapter.refreshRequest();
    }

    @Override
    public void onContactListChanged(CommonState commonState, boolean hasContacts,
                                     boolean hasVisibleContacts, boolean isFilterEnabled) {
        if (adapter.isHasActiveChats()) {
            scrollToChatsActionButtonContainer.setVisibility(View.VISIBLE);
        } else {
            scrollToChatsActionButtonContainer.setVisibility(View.GONE);
        }

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
            text = R.string.application_state_no_online;
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
                    adapter.onChange();
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
            button = R.string.application_action_offline;
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
                    startActivity(AccountListActivity.createIntent(getActivity()));
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
        textView.setText(text);
        if (button == 0) {
            buttonView.setVisibility(View.GONE);
        } else {
            buttonView.setVisibility(View.VISIBLE);
            buttonView.setText(button);
        }
        buttonView.setOnClickListener(listener);
    }

    /**
     * Force stop contact list updates before pause or application close.
     */
    public void unregisterListeners() {
        EventBus.getDefault().unregister(this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        adapter.removeRefreshRequests();
    }

    public UpdatableAdapter getAdapter() {
        return adapter;
    }

    public Filterable getFilterableAdapter() {
        return adapter;
    }

    /**
     * Scroll contact list to specified account.
     *
     * @param account
     */
    void scrollTo(AccountJid account) {
        long count = adapter.getItemCount();
        for (int position = 0; position < (int) count; position++) {
            Object itemAtPosition = adapter.getItem(position);
            if (itemAtPosition != null && itemAtPosition instanceof AccountConfiguration
                    && ((AccountConfiguration)itemAtPosition).getAccount().equals(account)) {
                linearLayoutManager.scrollToPositionWithOffset(position, 0);
                break;
            }
        }
    }

    /**
     * Filter out contact list for selected account.
     *
     * @param account
     */
    void setSelectedAccount(AccountJid account) {
        if (account.equals(AccountManager.getInstance().getSelectedAccount())) {
            SettingsManager.setContactsSelectedAccount(null);
        } else {
            SettingsManager.setContactsSelectedAccount(account);
        }
        adapter.onChange();
    }

    /**
     * Scroll to the top of contact list.
     */
    public void scrollUp() {
        recyclerView.scrollToPosition(0);
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.fab_up_container) {
            scrollUp();
            return;
        }


        AccountJid account = accountActionButtonsAdapter.getItemForView(view);
        if (account == null) { // Check for tap on account in the title
            return;
        }
        if (!SettingsManager.contactsShowAccounts()) {
            if (AccountManager.getInstance().getEnabledAccounts().size() < 2) {
                scrollUp();
            } else {
                setSelectedAccount(account);
                rebuild();
            }
        } else {
            scrollTo(account);
        }
    }

    public void onAccountsChanged() {
        accountActionButtonsAdapter.onChange();
    }

    public void rebuild() {
        accountActionButtonsAdapter.rebuild();
    }

    public interface ContactListFragmentListener {
        void onContactClick(AbstractContact contact);
        void onContactListChange(CommonState commonState);
    }

    @Override
    public void onContactClick(AbstractContact contact) {
        contactListFragmentListener.onContactClick(contact);
    }

    @Override
    public void onAccountMenuClick(AccountJid accountJid, View view) {
        PopupMenu popup = new PopupMenu(getActivity(), view);
        popup.inflate(R.menu.account);
        ContextMenuHelper.setUpAccountMenu((ManagedActivity) getActivity(), adapter, accountJid, popup.getMenu());
        popup.show();
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> addresses) {
        adapter.refreshRequest();
    }




}
