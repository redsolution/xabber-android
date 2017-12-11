package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.brandongogetap.stickyheaders.StickyLayoutManager;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.NewMessageEvent;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.ui.activity.AccountAddActivity;
import com.xabber.android.ui.activity.ConferenceSelectActivity;
import com.xabber.android.ui.activity.ContactAddActivity;
import com.xabber.android.ui.activity.ManagedActivity;
import com.xabber.android.ui.activity.StatusEditActivity;
import com.xabber.android.ui.adapter.UpdatableAdapter;
import com.xabber.android.ui.adapter.contactlist.ContactListAdapter;
import com.xabber.android.ui.adapter.contactlist.ContactListAdapter.ContactListAdapterListener;
import com.xabber.android.ui.adapter.contactlist.ContactListState;
import com.xabber.android.ui.adapter.contactlist.RosterChatViewHolder;
import com.xabber.android.ui.adapter.contactlist.viewobjects.AccountVO;
import com.xabber.android.ui.adapter.contactlist.viewobjects.BaseRosterItemVO;
import com.xabber.android.ui.adapter.contactlist.viewobjects.ChatVO;
import com.xabber.android.ui.color.AccountPainter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.helper.ContextMenuHelper;
import com.xabber.android.ui.helper.RecyclerItemTouchHelper;
import com.xabber.android.ui.preferences.PreferenceEditor;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collection;

public class ContactListFragment extends Fragment implements OnAccountChangedListener,
        OnContactChangedListener, ContactListAdapterListener, View.OnClickListener,
        Toolbar.OnMenuItemClickListener, android.widget.PopupMenu.OnMenuItemClickListener,
        RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {

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
    private AccountPainter accountPainter;

    private ContactListFragmentListener contactListFragmentListener;
    private LinearLayoutManager linearLayoutManager;

    //private BarPainter barPainter;
    private View accountColorIndicator;
    private Menu optionsMenu;
    private View addMenuOption;
    private Toolbar toolbar;
    private LinearLayout placeholderView;
    private TextView tvPlaceholderMessage;
    private CoordinatorLayout coordinatorLayout;
    private Snackbar snackbar;

    public static final String ACCOUNT_JID = "account_jid";
    private AccountJid scrollToAccountJid;

    public static ContactListFragment newInstance(@Nullable AccountJid account) {
        ContactListFragment fragment = new ContactListFragment();
        Bundle args = new Bundle();
        if (account != null)
            args.putSerializable(ACCOUNT_JID, account);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.scrollToAccountJid = (AccountJid) getArguments().getSerializable(ACCOUNT_JID);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        contactListFragmentListener = (ContactListFragmentListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact_list, container, false);

        coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.coordinatorLayout);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar_default);
        toolbar.setOnClickListener(this);
        toolbar.inflateMenu(R.menu.toolbar_contact_list);
        optionsMenu = toolbar.getMenu();
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.ic_overflow_menu_white_24dp));
        toolbar.setTitleTextColor(getResources().getColor(R.color.grey_600));

        accountColorIndicator = view.findViewById(R.id.accountColorIndicator);
        accountColorIndicator.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getDefaultMainColor());

        toolbar.setTitle(R.string.application_title_full);

        // to avoid strange bug on some 4.x androids
        view.setBackgroundColor(ColorManager.getInstance().getContactListBackgroundColor());

        recyclerView = (RecyclerView) view.findViewById(R.id.contact_list_recycler_view);
        registerForContextMenu(recyclerView);
        adapter = new ContactListAdapter((ManagedActivity) getActivity(), this);
        linearLayoutManager = new StickyLayoutManager(getActivity(), adapter);
        ((StickyLayoutManager)linearLayoutManager).setStickyHeaderListener(adapter);
        ((StickyLayoutManager)linearLayoutManager).elevateHeaders(2);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback =
                new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);
        infoView = view.findViewById(R.id.info);
        connectedView = infoView.findViewById(R.id.connected);
        disconnectedView = infoView.findViewById(R.id.disconnected);
        textView = (TextView) infoView.findViewById(R.id.text);
        buttonView = (Button) infoView.findViewById(R.id.button);
        animation = AnimationUtils.loadAnimation(getActivity(), R.anim.connection);

        accountPainter = ColorManager.getInstance().getAccountPainter();

        addMenuOption = view.findViewById(R.id.action_add);

        // placeholder
        placeholderView = (LinearLayout) view.findViewById(R.id.placeholderView);
        tvPlaceholderMessage = (TextView) view.findViewById(R.id.tvPlaceholderMessage);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        adapter.onChange();
        //barPainter.setDefaultColor();
        accountColorIndicator.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().setStatusBarColor(ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
        }
        if (scrollToAccountJid != null) {
            scrollToAccount(scrollToAccountJid);
            scrollToAccountJid = null;
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
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        //barPainter.setDefaultColor();
        accountColorIndicator.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().setStatusBarColor(ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
        }
        adapter.refreshRequest();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewMessageEvent(NewMessageEvent event) {
        adapter.refreshRequest();
    }

    @Override
    public void onContactListChanged(CommonState commonState, boolean hasContacts,
                                     boolean hasVisibleContacts, boolean isFilterEnabled) {

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
            for (int i = 0; i < optionsMenu.size(); i++) {
                optionsMenu.getItem(i).setVisible(true);
            }
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
                    contactListFragmentListener.onManageAccountsClick();
                }
            };
            for (int i = 0; i < optionsMenu.size(); i++) {
                optionsMenu.getItem(i).setVisible(false);
            }
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
        linearLayoutManager.scrollToPositionWithOffset(position, 0);
    }


    @Override
    public void onClick(View view) {}

    public interface ContactListFragmentListener {
        void onContactClick(AbstractContact contact);
        void onContactListChange(CommonState commonState);
        void onManageAccountsClick();
    }

    @Override
    public void onContactClick(AbstractContact contact) {
        contactListFragmentListener.onContactClick(contact);
    }

    @Override
    public void onAccountMenuClick(AccountJid accountJid, View view) {
        PopupMenu popup = new PopupMenu(getActivity(), view);
        popup.inflate(R.menu.item_account_group);
        ContextMenuHelper.setUpAccountMenu((ManagedActivity) getActivity(), adapter, accountJid, popup.getMenu());
        popup.show();
    }

    @Override
    public void onScrollToPosition(int position) {
        scrollTo(position);
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> addresses) {
        adapter.refreshRequest();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_change_status:
                startActivity(StatusEditActivity.createIntent(getActivity()));
                return true;
            case R.id.action_add_contact:
                startActivity(ContactAddActivity.createIntent(getActivity()));
                return true;
            case R.id.action_close_chats:
                closeAllChats();
                return true;
            case R.id.action_join_conference:
                startActivity(ConferenceSelectActivity.createIntent(getActivity()));
                return true;
//            case R.id.action_chat_list:
//                startActivity(ChatActivity.createRecentChatsIntent(getActivity()));
//                return true;
            case R.id.action_add:
                showToolbarPopup(addMenuOption);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void closeAllChats() {
        MessageManager.closeActiveChats();
        getAdapter().onChange();
    }

    private void showToolbarPopup(View v) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.menu_add_in_contact_list);
        popupMenu.show();
    }

    @Override
    public void hidePlaceholder() {
        placeholderView.setVisibility(View.GONE);
    }

    @Override
    public void showPlaceholder(String message) {
        tvPlaceholderMessage.setText(message);
        placeholderView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onChatListStateChanged() {
        closeSnackbar();
    }

    public void showRecent() {
        if (adapter != null)
            adapter.onStateChanged(ContactListAdapter.ChatListState.recent);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof RosterChatViewHolder) {

            Object itemAtPosition = adapter.getItem(position);
            if (itemAtPosition != null && itemAtPosition instanceof ChatVO) {

                // backup of removed item for undo purpose
                final BaseRosterItemVO deletedItem = (BaseRosterItemVO) itemAtPosition;
                final int deletedIndex = viewHolder.getAdapterPosition();

                // update value
                setChatArchived((ChatVO) deletedItem, !((ChatVO) deletedItem).isArchived());

                // remove the item from recycler view
                adapter.removeItem(viewHolder.getAdapterPosition());

                // showing snackbar with Undo option
                showSnackbar(deletedItem, deletedIndex);
            }
        }
    }

    public void showSnackbar(final BaseRosterItemVO deletedItem, final int deletedIndex) {
        if (snackbar != null) snackbar.dismiss();
        final boolean archived = ((ChatVO) deletedItem).isArchived();
        snackbar = Snackbar.make(coordinatorLayout, archived ? R.string.chat_was_unarchived
                : R.string.chat_was_archived, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // update value
                setChatArchived((ChatVO) deletedItem, archived);

                // undo is selected, restore the deleted item
                adapter.restoreItem(deletedItem, deletedIndex);
            }
        });
        snackbar.setActionTextColor(Color.YELLOW);
        snackbar.show();
    }

    public void setChatArchived(ChatVO chatVO, boolean archived) {
        AbstractChat chat = MessageManager.getInstance().getChat(chatVO.getAccountJid(), chatVO.getUserJid());
        if (chat != null) chat.setArchived(archived, true);
    }

    public void closeSnackbar() {
        if (snackbar != null) snackbar.dismiss();
    }

    public ContactListAdapter.ChatListState getListState() {
        if (adapter != null) return adapter.getCurrentChatsState();
        else return ContactListAdapter.ChatListState.recent;
    }
}
