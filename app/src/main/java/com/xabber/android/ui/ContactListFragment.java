package com.xabber.android.ui;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.message.OnChatChangedListener;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.ui.adapter.AccountActionButtonsAdapter;
import com.xabber.android.ui.adapter.AccountConfiguration;
import com.xabber.android.ui.adapter.ContactListAdapter;
import com.xabber.android.ui.adapter.ContactListAdapter.OnContactListChangedListener;
import com.xabber.android.ui.adapter.ContactListState;
import com.xabber.android.ui.adapter.GroupConfiguration;
import com.xabber.android.ui.adapter.GroupedContactAdapter;
import com.xabber.android.ui.adapter.UpdatableAdapter;
import com.xabber.android.ui.helper.AccountPainter;
import com.xabber.android.ui.helper.ContextMenuHelper;
import com.xabber.android.ui.preferences.AccountList;

import java.util.Collection;

public class ContactListFragment extends Fragment implements OnAccountChangedListener,
        OnContactChangedListener, OnChatChangedListener, OnItemClickListener,
        OnContactListChangedListener, View.OnClickListener, GroupedContactAdapter.OnClickListener {

    private ContactListAdapter adapter;

    private ListView listView;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_list_fragment, container, false);
        listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);
        listView.setItemsCanFocus(true);
        registerForContextMenu(listView);
        adapter = new ContactListAdapter(getActivity(), this, this);
        listView.setAdapter(adapter);
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

        accountPainter = new AccountPainter(getActivity());
        scrollToChatsActionButton.setColorNormal(accountPainter.getDefaultMainColor());
        scrollToChatsActionButton.setColorPressed(accountPainter.getDefaultDarkColor());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnChatChangedListener.class, this);
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
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        BaseEntity baseEntity = (BaseEntity) listView.getItemAtPosition(info.position);
        if (baseEntity instanceof AbstractContact) {
            ContextMenuHelper.createContactContextMenu(
                    getActivity(), adapter, (AbstractContact) baseEntity, menu);
        } else if (baseEntity instanceof AccountConfiguration) {
            ContextMenuHelper.createAccountContextMenu(
                    getActivity(), adapter, baseEntity.getAccount(), menu);
        } else if (baseEntity instanceof GroupConfiguration) {
            ContextMenuHelper.createGroupContextMenu(getActivity(), adapter,
                    baseEntity.getAccount(), baseEntity.getUser(), menu);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object object = parent.getAdapter().getItem(position);
        if (object instanceof AbstractContact) {
            ((OnContactClickListener) getActivity()).onContactClick((AbstractContact) object);
        } else if (object instanceof GroupConfiguration) {
            GroupConfiguration groupConfiguration = (GroupConfiguration) object;
            adapter.setExpanded(groupConfiguration.getAccount(), groupConfiguration.getUser(),
                    !groupConfiguration.isExpanded());
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void onContactsChanged(Collection<BaseEntity> addresses) {
        adapter.refreshRequest();
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        adapter.refreshRequest();
        scrollToChatsActionButton.setColorNormal(accountPainter.getDefaultMainColor());
        scrollToChatsActionButton.setColorPressed(accountPainter.getDefaultDarkColor());
    }

    @Override
    public void onChatChanged(String account, String user, boolean incoming) {
        if (incoming) {
            adapter.refreshRequest();
        }
    }

    @Override
    public void onContactListChanged(CommonState commonState, boolean hasContacts,
                                     boolean hasVisibleContacts, boolean isFilterEnabled) {
        if (adapter.isHasActiveChats()) {
            scrollToChatsActionButtonContainer.setVisibility(View.VISIBLE);
        } else {
            scrollToChatsActionButtonContainer.setVisibility(View.GONE);
        }


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
                    startActivity(ContactAdd.createIntent(getActivity()));
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
                    ConnectionManager.getInstance().updateConnections(true);
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
                    startActivity(AccountList.createIntent(getActivity()));
                }
            };
        } else if (commonState == CommonState.empty) {
            state = ContactListState.offline;
            text = R.string.application_state_empty;
            button = R.string.application_action_empty;
            listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(AccountAdd.createIntent(getActivity()));
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
    void unregisterListeners() {
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnChatChangedListener.class, this);
        adapter.removeRefreshRequests();
    }

    UpdatableAdapter getAdapter() {
        return adapter;
    }

    Filterable getFilterableAdapter() {
        return adapter;
    }

    /**
     * Scroll contact list to specified account.
     *
     * @param account
     */
    void scrollTo(String account) {
        long count = listView.getCount();
        for (int position = 0; position < (int) count; position++) {
            BaseEntity baseEntity = (BaseEntity) listView.getItemAtPosition(position);
            if (baseEntity != null && baseEntity instanceof AccountConfiguration
                    && baseEntity.getAccount().equals(account)) {
                stopMovement();
                listView.setSelection(position);
                break;
            }
        }
    }

    /**
     * Filter out contact list for selected account.
     *
     * @param account
     */
    void setSelectedAccount(String account) {
        if (account.equals(AccountManager.getInstance().getSelectedAccount())) {
            SettingsManager.setContactsSelectedAccount("");
        } else {
            SettingsManager.setContactsSelectedAccount(account);
        }
        stopMovement();
        adapter.onChange();
    }

    /**
     * Scroll to the top of contact list.
     */
    void scrollUp() {
        if (listView.getCount() > 0) {
            listView.setSelection(0);
        }
        stopMovement();
    }

    /**
     * Stop fling scrolling.
     */
    private void stopMovement() {
        MotionEvent event = MotionEvent.obtain(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0);
        listView.onTouchEvent(event);
        event.recycle();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.fab_up_container) {
            scrollUp();
            return;
        }


        String account = accountActionButtonsAdapter.getItemForView(view);
        if (account == null) { // Check for tap on account in the title
            return;
        }
        if (!SettingsManager.contactsShowAccounts()) {
            if (AccountManager.getInstance().getAccounts().size() < 2) {
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

    @Override
    public void onAccountMenuClick(View view, final String account) {
        PopupMenu popup = new PopupMenu(getActivity(), view);
        popup.inflate(R.menu.account);
        ContextMenuHelper.setUpAccountMenu(getActivity(), adapter, account, popup.getMenu());
        popup.show();
    }

    public interface OnContactClickListener {
        void onContactClick(AbstractContact contact);
    }

}
