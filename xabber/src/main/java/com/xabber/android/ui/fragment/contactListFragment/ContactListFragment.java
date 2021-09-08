package com.xabber.android.ui.fragment.contactListFragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
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
import androidx.appcompat.widget.Toolbar;
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
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.AccountActivity;
import com.xabber.android.ui.activity.AccountAddActivity;
import com.xabber.android.ui.activity.ContactAddActivity;
import com.xabber.android.ui.activity.ContactViewerActivity;
import com.xabber.android.ui.activity.MainActivity;
import com.xabber.android.ui.adapter.contactlist.ContactListState;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.fragment.contactListFragment.viewObjects.AccountVO;
import com.xabber.android.ui.fragment.contactListFragment.viewObjects.ButtonVO;
import com.xabber.android.ui.fragment.contactListFragment.viewObjects.ContactVO;
import com.xabber.android.ui.fragment.contactListFragment.viewObjects.GroupVO;
import com.xabber.android.ui.helper.ContextMenuHelper;
import com.xabber.android.ui.widget.bottomnavigation.AccountShortcutAdapter;
import com.xabber.android.ui.widget.bottomnavigation.AccountShortcutVO;

import org.jetbrains.annotations.NotNull;

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
    private LinearLayoutManager linearLayoutManager;
    private RecyclerView accountsRecyclerView;
    private final ArrayList<AccountShortcutVO> accountShortcutVOArrayList = new ArrayList<>();
    private ArrayList<AccountJid> accountsJidList;
    /**
     * Default toolbar that is only being displayed
     * until at least one account starts connecting
     */
    private Toolbar defaultToolbarLayout;
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

    public static ContactListFragment newInstance(@Nullable AccountJid account) {
        ContactListFragment fragment = new ContactListFragment();
        Bundle args = new Bundle();
        if (account != null)
            args.putSerializable(ACCOUNT_JID, account);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NotNull Context context) {
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

        recyclerView = view.findViewById(R.id.recyclerView);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);

        defaultToolbarLayout = view.findViewById(R.id.contact_list_default_toolbar);

        infoView = view.findViewById(R.id.info);
        connectedView = infoView.findViewById(R.id.connected);
        disconnectedView = infoView.findViewById(R.id.disconnected);
        //ColorManager.setGrayScaleFilter(disconnectedView);
        textView = infoView.findViewById(R.id.text);
        buttonView = infoView.findViewById(R.id.button);
        animation = AnimationUtils.loadAnimation(getActivity(), R.anim.connection);

        items = new ArrayList<>();
        adapter = new FlexibleAdapter<>(items, null, false);

        adapter.setStickyHeaders(true);
        adapter.setDisplayHeadersAtStartUp(true);
        recyclerView.setAdapter(adapter);

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

    public void scrollToTop() {
        recyclerView.smoothScrollToPosition(0);
    }

    /**
     * Setup bottom accounts list with avatars.
     */
    public void updateAccountsList() {
        accountsJidList = new ArrayList<>(AccountManager.getInstance().getEnabledAccounts());
        Collections.sort(accountsJidList);
        if (accountsJidList.size() > 1) {
            accountShortcutVOArrayList.clear();
            accountShortcutVOArrayList.addAll(AccountShortcutVO.convert(accountsJidList));
            AccountShortcutAdapter accountShortcutAdapter = new AccountShortcutAdapter(accountShortcutVOArrayList, getActivity(), this);
            accountsRecyclerView.setAdapter(accountShortcutAdapter);
            accountsRecyclerView.setVisibility(View.VISIBLE);
            accountsRecyclerView.setOnClickListener(this);
        } else accountsRecyclerView.setVisibility(View.GONE);
    }

    private void setDefaultToolbarColors() {
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light &&
                AccountManager.getInstance().getFirstAccount() != null)
            defaultToolbarLayout.setBackgroundColor(ColorManager.getInstance().getAccountPainter().
                    getAccountRippleColor(AccountManager.getInstance().getFirstAccount()));
        else {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getContext().getTheme();
            theme.resolveAttribute(R.attr.bars_color, typedValue, true);
            defaultToolbarLayout.setBackgroundColor(typedValue.data);
        }
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter = ContactListPresenter.getInstance();
        ((MainActivity) getActivity()).setStatusBarColor();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.bindView(this);
        setDefaultToolbarColors();
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
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                if (item instanceof AccountVO)
                    ((MainActivity) getActivity()).setStatusBarColor(((AccountVO) item).getAccountJid()); // account color
                else ((MainActivity) getActivity()).setStatusBarColor(); // main color
            }

        }
    }

    @Override
    public boolean onItemClick(View view, int position) {
        adapter.notifyItemChanged(position);
        presenter.onItemClick(adapter.getItem(position));
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.avatarView) {
            int position = accountsRecyclerView.getChildLayoutPosition(v);
            AccountJid clickedAccountJid = accountsJidList.get(position);
            scrollToAccount(clickedAccountJid);
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                if (Build.VERSION.SDK_INT > 20)
                    ((MainActivity) getActivity()).setStatusBarColor(clickedAccountJid);
            }
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
            ContactJid contactJid = ((ContactVO) item).getContactJid();
            AbstractContact abstractContact = RosterManager.getInstance().getAbstractContact(accountJid, contactJid);
            ContextMenuHelper.createContactContextMenu(
                    getActivity(),
                    presenter,
                    abstractContact.getAccount(),
                    abstractContact.getContactJid(),
                    menu
            );
            return;
        }

        if (item != null && item instanceof GroupVO) {
            AccountJid accountJid = ((GroupVO) item).getAccountJid();
            ContextMenuHelper.createGroupContextMenu(getActivity(), presenter, accountJid,
                    ((GroupVO) item).getGroupName(), menu);
        }
    }

    @Override
    public void onContactAvatarClick(int adapterPosition) {
        IFlexible item = adapter.getItem(adapterPosition);
        if (item != null && item instanceof ContactVO) {
            Intent intent;
            AccountJid accountJid = ((ContactVO) item).getAccountJid();
            ContactJid contactJid = ((ContactVO) item).getContactJid();
            intent = ContactViewerActivity.createIntent(getActivity(), accountJid, contactJid);
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
            startActivity(ContactAddActivity.Companion.createIntent(getContext()));
        }
    }

    @Override
    public void onContactListChanged(CommonState commonState, boolean hasContacts,
                                     boolean hasVisibleContacts) {

        if (contactListFragmentListener != null)
            contactListFragmentListener.onContactListChange(commonState);

        if (hasVisibleContacts) {
            defaultToolbarLayout.setVisibility(View.GONE);
            infoView.setVisibility(View.GONE);
            disconnectedView.clearAnimation();
            return;
        }
        infoView.setVisibility(View.VISIBLE);
        final int text;
        final int button;
        final ContactListState state;
        final View.OnClickListener listener;
        if (hasContacts) {
            state = ContactListState.online;
            text = R.string.application_state_no_online;
            button = R.string.application_action_no_online;
            listener = view -> {
                SettingsManager.setContactsShowOffline(true);
                presenter.updateContactList();
            };
        } else {
            defaultToolbarLayout.setVisibility(View.VISIBLE);
            switch (commonState) {
                case online:
                    defaultToolbarLayout.setVisibility(View.GONE);
                    infoView.setVisibility(View.GONE);
                    disconnectedView.clearAnimation();
                    return;
                case roster:
                    state = ContactListState.connecting;
                    text = R.string.application_state_roster;
                    button = 0;
                    listener = null;
                    break;
                case connecting:
                    state = ContactListState.connecting;
                    text = R.string.application_state_connecting;
                    button = 0;
                    listener = null;
                    break;
                case waiting:
                    state = ContactListState.offline;
                    text = R.string.application_state_waiting;
                    button = R.string.application_action_waiting;
                    listener = view -> ConnectionManager.getInstance().connectAll();
                    break;
                case offline:
                    state = ContactListState.offline;
                    text = R.string.application_state_offline;
                    button = R.string.application_state_offline;
                    listener = view -> AccountManager.getInstance().setStatus(
                            StatusMode.available, null);
                    break;
                case disabled:
                    state = ContactListState.offline;
                    text = R.string.application_state_disabled;
                    button = R.string.application_action_disabled;
                    listener = view -> contactListFragmentListener.onManageAccountsClick();
                    break;
                case empty:
                    state = ContactListState.offline;
                    text = R.string.application_state_empty;
                    button = R.string.application_action_empty;
                    listener = view -> startActivity(AccountAddActivity.createIntent(getActivity()));
                    break;
                default:
                    throw new IllegalStateException();
            }
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
     */
    public void scrollToAccount(AccountJid account) {

        long count = adapter.getItemCount();
        for (int position = 0; position < (int) count; position++) {
            Object itemAtPosition = adapter.getItem(position);
            if (itemAtPosition != null && itemAtPosition instanceof AccountVO
                    && ((AccountVO) itemAtPosition).getAccountJid().equals(account)) {
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

    public interface ContactListFragmentListener {
        void onContactClick(AbstractContact contact);

        void onContactListChange(CommonState commonState);

        void onManageAccountsClick();
    }
}
