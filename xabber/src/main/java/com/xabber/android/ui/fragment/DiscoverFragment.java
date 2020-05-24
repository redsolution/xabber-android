package com.xabber.android.ui.fragment;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.database.realmobjects.RecentSearchRealmObject;
import com.xabber.android.data.database.repositories.RecentSearchRealmObjectRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.RegularChat;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.MainActivity;
import com.xabber.android.ui.activity.StatusEditActivity;
import com.xabber.android.ui.adapter.DiscoverContactsListItemAdapter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.fragment.chatListFragment.ChatListAdapter;
import com.xabber.android.ui.fragment.chatListFragment.ChatListFragment;
import com.xabber.android.ui.fragment.chatListFragment.ChatListItemListener;
import com.xabber.android.utils.StringUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class DiscoverFragment extends Fragment implements View.OnClickListener,
        OnAccountChangedListener, OnContactChangedListener, ChatListItemListener,
        DiscoverContactsListItemAdapter.DiscoverContactsListItemListener {

    private static final String LOG_TAG = DiscoverFragment.class.getSimpleName();

    /* Toolbar variables */
    private AppBarLayout toolbarAppBarLayout;
    private Toolbar toolbarToolbarLayout;
    private RelativeLayout toolbarRelativeLayout;
    private View toolbarAccountColorIndicator;
    private View toolbarAccountColorIndicatorBack;
    private ImageView toolbarAvatarIv;
    private ImageView toolbarStatusIv;
    private EditText toolbarSearchEt;
    private ImageView toolbarClearIv;
    private ImageView toolbarArrowBackIv;
    private ImageView toolbarTuneIcon;

    /* ContactsList variables */
    private View contactListRoot;
    private TextView contactListTitleTextView;
    private RecyclerView contactListRecyclerView;
    private LinearLayoutManager chatListLinearLayoutManager;
    private DiscoverContactsListItemAdapter contactsListAdapter;
    private View contactListAccountColorIndicator;

    /* RecentList variables */
    private ConstraintLayout recentListConstraintLayout;
    private RelativeLayout recentListTitleRootView;
    private TextView recentListTitleTextView;
    private AppCompatButton recentListClearBtn;
    private RecyclerView recentListRecyclerView;
    private LinearLayoutManager recentListLinearLayoutManager;
    private ChatListAdapter recentChatListAdapter;

    /* Keyboard management variables */
    private InputMethodManager inputMethodManager;
    private boolean keyboardShowed = false;

    public static DiscoverFragment newInstance() {
        return new DiscoverFragment();
    }

    @Override
    public void onAttach(Context context) {
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        super.onDetach();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        initContactsList(view);
        initRecent(view);
        initToolbar(view);

        if (getActivity() != null && getActivity().getSystemService(Context.INPUT_METHOD_SERVICE) != null)
            inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        buildChatsListWithFilter(null);
        update();
    }

    /**
     * OnClickListener for Toolbar
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivAvatar:
                startActivity(StatusEditActivity.createIntent(getActivity()));
                break;
            case R.id.discover_toolbar_clear_button:
                toolbarSearchEt.setText("");
                break;
            case R.id.discover_toolbar_arrow_back_image_view:
                setKeyboardShowed(false);
                toolbarSearchEt.setText("");
                break;
            case R.id.discover_toolbar_tune_image_view:
                Toast.makeText(getContext(), "Under construction", Toast.LENGTH_SHORT).show();
                break;
            case R.id.discover_recent_list_title_clear_button:
                RecentSearchRealmObjectRepository.clearAllRecentSearched();
                buildChatsListWithFilter(null);
                break;
        }
    }

    @Override
    public void onContactListItemClick(@NotNull AbstractChat contact) {
        try {
            ((MainActivity) getActivity()).onChatClick(RosterManager.getInstance()
                    .getAbstractContact(contact.getAccount(), contact.getUser()));
            RecentSearchRealmObjectRepository.itemWasSearched(contact.getAccount(), contact.getUser());
        } catch (Exception e) {
            LogManager.exception(ChatListFragment.class.toString(), e);
        }
    }

    @Override
    public void onChatItemClick(@NotNull AbstractChat contact) {
        try {
            ((MainActivity) getActivity()).onChatClick(RosterManager.getInstance()
                    .getAbstractContact(contact.getAccount(), contact.getUser()));
            RecentSearchRealmObjectRepository.itemWasSearched(contact.getAccount(), contact.getUser());
        } catch (Exception e) {
            LogManager.exception(ChatListFragment.class.toString(), e);
        }
    }

    @Override
    public void onChatAvatarClick(@NotNull AbstractChat contact) {
        try {
            ((MainActivity) getActivity()).onChatClick(RosterManager.getInstance()
                    .getAbstractContact(contact.getAccount(), contact.getUser()));
        } catch (Exception e) {
            LogManager.exception(ChatListFragment.class.toString(), e);
        }
    }

    @Override
    public void onChatItemContextMenu(@NotNull ContextMenu menu, @NotNull AbstractChat contact) {
        //todo disable context menu
    }

    @Override
    public void onChatItemSwiped(@NotNull AbstractChat abstractContact) {
        //todo disable swiping
    }

    @Override
    public void onListBecomeEmpty() {
        //todo implement this
    }

    /**
     * Update toolbarRelativeLayout via current state
     */
    public void updateToolbar() {
        /* Update avatar and status ImageViews via current settings and main user */
        if (SettingsManager.contactsShowAvatars()
                && AccountManager.getInstance().getEnabledAccounts().size() != 0) {
            toolbarAvatarIv.setVisibility(View.VISIBLE);
            toolbarStatusIv.setVisibility(View.VISIBLE);
            AccountJid mainAccountJid = AccountManager.getInstance().getFirstAccount();
            AccountItem mainAccountItem = AccountManager.getInstance().getAccount(mainAccountJid);
            Drawable mainAccountAvatar = AvatarManager.getInstance()
                    .getAccountAvatar(mainAccountJid);
            int mainAccountStatusMode = mainAccountItem.getDisplayStatusMode().getStatusLevel();
            toolbarAvatarIv.setImageDrawable(mainAccountAvatar);
            toolbarStatusIv.setImageLevel(mainAccountStatusMode);
        } else {
            toolbarAvatarIv.setVisibility(View.GONE);
            toolbarStatusIv.setVisibility(View.GONE);
        }

        /* Update background color via current main user and theme; */
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light &&
                AccountManager.getInstance().getFirstAccount() != null)
            toolbarRelativeLayout.setBackgroundColor(ColorManager.getInstance().getAccountPainter().
                    getAccountRippleColor(AccountManager.getInstance().getFirstAccount()));
        else {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getContext().getTheme();
            theme.resolveAttribute(R.attr.bars_color, typedValue, true);
            toolbarRelativeLayout.setBackgroundColor(typedValue.data);
        }

        /* Update left color indicator via current main user */
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light
                && AccountManager.getInstance().getEnabledAccounts().size() > 1) {
            toolbarAccountColorIndicator.setBackgroundColor(
                    ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
            toolbarAccountColorIndicatorBack.setBackgroundColor(
                    ColorManager.getInstance().getAccountPainter().getDefaultIndicatorBackColor());
        } else {
            toolbarAccountColorIndicator.setBackgroundColor(
                    getResources().getColor(R.color.transparent));
            toolbarAccountColorIndicatorBack.setBackgroundColor(
                    getResources().getColor(R.color.transparent));
        }
    }

    public void update() {
        //updateToolbar();
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        update();
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        update();
    }

    public void setToolbarModeSearching(boolean enabled){
        if (enabled){
            toolbarClearIv.setVisibility(View.VISIBLE);
            toolbarArrowBackIv.setVisibility(View.VISIBLE);
            toolbarAvatarIv.setVisibility(View.GONE);
            toolbarStatusIv.setVisibility(View.GONE);
            toolbarTuneIcon.setVisibility(View.GONE);
            recentListTitleRootView.setVisibility(View.GONE);
            contactListRoot.setVisibility(View.GONE);

            toolbarSearchEt.setBackgroundDrawable(null);
            RelativeLayout.LayoutParams etLayoutParams = (RelativeLayout.LayoutParams) toolbarSearchEt.getLayoutParams();
            etLayoutParams.setMargins(0, 0,144, 0);
            toolbarSearchEt.setLayoutParams(etLayoutParams);

            if (!toolbarSearchEt.hasFocus()) toolbarSearchEt.requestFocus();
        } else {
            toolbarClearIv.setVisibility(View.GONE);
            toolbarArrowBackIv.setVisibility(View.GONE);
            toolbarAvatarIv.setVisibility(View.VISIBLE);
            toolbarStatusIv.setVisibility(View.VISIBLE);
            toolbarTuneIcon.setVisibility(View.VISIBLE);
            recentListTitleRootView.setVisibility(View.VISIBLE);
            contactListRoot.setVisibility(View.VISIBLE);

            toolbarSearchEt.setBackgroundDrawable(getContext().getDrawable(R.drawable.edit_text_rounded_corners_background));
            RelativeLayout.LayoutParams etLayoutParams = (RelativeLayout.LayoutParams) toolbarSearchEt.getLayoutParams();
            etLayoutParams.setMargins(48, 0,144, 0);
            toolbarSearchEt.setLayoutParams(etLayoutParams);

            if (!toolbarSearchEt.hasFocus()) toolbarSearchEt.requestFocus();
        }
    }

    public void setKeyboardShowed(boolean show){
        if (show){
            toolbarSearchEt.requestFocus();
            inputMethodManager.showSoftInput(toolbarSearchEt, InputMethodManager.SHOW_IMPLICIT);
        } else {
            inputMethodManager.hideSoftInputFromWindow(toolbarSearchEt.getWindowToken(), 0);
        }
    }

    public boolean isKeyboardShowed(){
        return toolbarSearchEt.isFocused();
    }

    private void initToolbar(View view){
        toolbarAppBarLayout = view.findViewById(R.id.discover_toolbar_root);
        toolbarToolbarLayout = view.findViewById(R.id.discover_toolbar);
        toolbarRelativeLayout = view.findViewById(R.id.discover_toolbar_relative_layout);
        toolbarAccountColorIndicator = view.findViewById(R.id.accountColorIndicator);
        toolbarAccountColorIndicatorBack = view.findViewById(R.id.accountColorIndicatorBack);
        toolbarAvatarIv = view.findViewById(R.id.ivAvatar);
        toolbarStatusIv = view.findViewById(R.id.ivStatus);
        toolbarSearchEt = view.findViewById(R.id.discover_toolbar_edittext);
        toolbarClearIv = view.findViewById(R.id.discover_toolbar_clear_button);
        toolbarArrowBackIv = view.findViewById(R.id.discover_toolbar_arrow_back_image_view);
        toolbarTuneIcon = view.findViewById(R.id.discover_toolbar_tune_image_view);

        toolbarClearIv.setOnClickListener(this);
        toolbarAvatarIv.setOnClickListener(this);
        toolbarArrowBackIv.setOnClickListener(this);
        toolbarTuneIcon.setOnClickListener(this);

        toolbarSearchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() != 0){
                    buildChatsListWithFilter(s.toString().toLowerCase());
                    setToolbarModeSearching(true);
                } else {
                    buildChatsListWithFilter(null);
                    setToolbarModeSearching(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        toolbarSearchEt.requestFocus();


        updateToolbar();
    }

    private void initContactsList(View view){
        contactListRoot = view.findViewById(R.id.discover_contacts_list_root);
        contactListTitleTextView = view.findViewById(R.id.discover_contacts_list_title);
        contactListRecyclerView = view.findViewById(R.id.discover_contacts_list_recycler);
        contactListAccountColorIndicator = view.findViewById(R.id.discover_contact_list_account_color_indicator);

        chatListLinearLayoutManager = new LinearLayoutManager(getActivity(),
                LinearLayoutManager.HORIZONTAL, false);
        contactListRecyclerView.setLayoutManager(chatListLinearLayoutManager);

        /* Update left color indicator via current main user */
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light
                && AccountManager.getInstance().getEnabledAccounts().size() > 1) {
            contactListAccountColorIndicator.setBackgroundColor(
                    ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
        } else {
            contactListAccountColorIndicator.setBackgroundColor(
                    getResources().getColor(R.color.transparent));
        }

        ArrayList<AbstractChat> onlineChats = new ArrayList<>();
        for (AbstractChat abstractChat : ChatManager.getInstance().getChatsOfEnabledAccounts()){
            StatusMode statusMode = RosterManager.getInstance()
                    .getAbstractContact(abstractChat.getAccount(), abstractChat.getUser()).getStatusMode();
            if (abstractChat.getLastMessage() != null
                    && !abstractChat.isArchived()
                    && !abstractChat.isGroupchat()
                    && !abstractChat.getUser().getJid().isDomainBareJid()
                    && (statusMode.equals(StatusMode.chat) || statusMode.equals(StatusMode.available)))
                onlineChats.add(abstractChat);
        }

        Collections.sort(onlineChats, (o1, o2) -> Long.compare(o2.getLastTime().getTime(),
                o1.getLastTime().getTime()));

        ArrayList<AbstractChat> otherChats = new ArrayList<>();
        for (AbstractChat abstractChat : ChatManager.getInstance().getChatsOfEnabledAccounts()){
            if (abstractChat.getLastMessage() != null
                    && !abstractChat.isArchived()
                    && !abstractChat.isGroupchat()
                    && !abstractChat.getUser().getJid().isDomainBareJid())
                otherChats.add(abstractChat);
        }

        Collections.sort(otherChats, (o1, o2) -> Long.compare(o2.getLastTime().getTime(),
                o1.getLastTime().getTime()));

        ArrayList<AbstractChat> allChats = concatLists(onlineChats, otherChats);

        if (allChats.size() > 15)
            allChats = new ArrayList<>(allChats.subList(0,14));

        contactsListAdapter = new DiscoverContactsListItemAdapter(allChats, this);
        contactListRecyclerView.setAdapter(contactsListAdapter);

    }

    private void initRecent(View view){
        recentListConstraintLayout = view.findViewById(R.id.discover_recent_root_constraint_layout);
        recentListTitleTextView = view.findViewById(R.id.discover_recent_list_title);
        recentListRecyclerView = view.findViewById(R.id.discover_recent_list_recycler);
        recentListClearBtn = view.findViewById(R.id.discover_recent_list_title_clear_button);
        recentListTitleRootView = view.findViewById(R.id.discover_recent_list_title_root_relative_layout);

        recentListClearBtn.setOnClickListener(this);

        recentListLinearLayoutManager = new LinearLayoutManager(getActivity(),
                LinearLayoutManager.VERTICAL, false);
        recentListRecyclerView.setLayoutManager(recentListLinearLayoutManager);

        ArrayList<AbstractChat> chats = new ArrayList<>();
//        if (!RecentSearchRealmObjectRepository.getAllRecentSearchRealmObjects().isEmpty()){
//            for (RecentSearchRealmObject recentSearchRealmObject : RecentSearchRealmObjectRepository.getAllRecentSearchRealmObjects()){
//                try {
//                    AccountJid accountJid = AccountJid.from(recentSearchRealmObject.getAccountJid());
//                    ContactJid contactJid = ContactJid.from(recentSearchRealmObject.getContactJid());
//                    if (ChatManager.getInstance().hasChat(recentSearchRealmObject.getAccountJid(), recentSearchRealmObject.getContactJid())){
//                        AbstractChat abstractChat = ChatManager.getInstance().getOrCreateChat(accountJid, contactJid);
//                        if (abstractChat.getLastMessage() != null && !abstractChat.isArchived())
//                            chats.add(abstractChat);
//                    } else chats.add(new RegularChat(accountJid, contactJid));
//                }catch (Exception e){ LogManager.exception(LOG_TAG, e); }
//            }
//            recentListTitleRootView.setVisibility(View.VISIBLE);
//        } else {
//            recentListTitleRootView.setVisibility(View.GONE);
//        }

        recentChatListAdapter = new ChatListAdapter(chats, this, false);
        recentListRecyclerView.setAdapter(recentChatListAdapter);
        buildChatsListWithFilter(null);
    }

    private void buildChatsListWithFilter(@Nullable String filterString){
        if (filterString != null && !filterString.isEmpty()){
            ArrayList<AbstractChat> chatsList = new ArrayList<>(
                    getFilteredChatsOfEnabledAccountsByString(ChatManager.getInstance()
                            .getChatsOfEnabledAccounts(), filterString));

            Collections.sort(chatsList, (o1, o2) -> Long.compare(o2.getLastTime().getTime(),
                    o1.getLastTime().getTime()));

            ArrayList<AbstractChat> contactList = new ArrayList<>(
                    getFilteredContactsOfEnabledAccountsByString(RosterManager.getInstance()
                            .getAllContactsForEnabledAccounts(), filterString));

            recentListTitleRootView.setVisibility(View.GONE);

            recentChatListAdapter.clear();
            recentChatListAdapter.addItems(concatLists(chatsList, contactList));
            recentChatListAdapter.notifyDataSetChanged();
        } else {
            ArrayList<AbstractChat> chats = new ArrayList<>();
            if (!RecentSearchRealmObjectRepository.getAllRecentSearchRealmObjects().isEmpty()){
                for (RecentSearchRealmObject recentSearchRealmObject : RecentSearchRealmObjectRepository.getAllRecentSearchRealmObjects()){
                    try {
                        AccountJid accountJid = AccountJid.from(recentSearchRealmObject.getAccountJid());
                        ContactJid contactJid = ContactJid.from(recentSearchRealmObject.getContactJid());
                        if (ChatManager.getInstance().hasChat(recentSearchRealmObject.getAccountJid(), recentSearchRealmObject.getContactJid())){
                            AbstractChat abstractChat = ChatManager.getInstance().getOrCreateChat(accountJid, contactJid);
                            if (abstractChat.getLastMessage() != null && !abstractChat.isArchived())
                                chats.add(abstractChat);
                        } else chats.add(new RegularChat(accountJid, contactJid));
                    }catch (Exception e){ LogManager.exception(LOG_TAG, e); }
                }
                recentListTitleRootView.setVisibility(View.VISIBLE);
            } else { recentListTitleRootView.setVisibility(View.GONE); }
            recentChatListAdapter.clear();
            recentChatListAdapter.addItems(chats);
            recentChatListAdapter.notifyDataSetChanged();
        }

    }
    private Collection<AbstractChat> getFilteredChatsOfEnabledAccountsByString(
            Collection<AbstractChat> abstractChats, String filterString) {
        String transliteratedFilterString = StringUtils.translitirateToLatin(filterString);
        Collection<AbstractChat> resultCollection = new ArrayList<>();
        for (AbstractChat abstractChat : abstractChats) {
            AbstractContact abstractContact = RosterManager.getInstance()
                    .getAbstractContact(abstractChat.getAccount(), abstractChat.getUser());
            if (abstractChat.getLastMessage() == null)
                continue;
            if (abstractChat.getUser().toString().contains(filterString)
                    || abstractChat.getUser().toString().contains(transliteratedFilterString)
                    || abstractContact.getName().contains(filterString)
                    || abstractContact.getName().contains(transliteratedFilterString))
                resultCollection.add(abstractChat);
        }
        return resultCollection;
    }

    private Collection<AbstractChat> getFilteredContactsOfEnabledAccountsByString(
            Collection<AbstractContact> abstractContacts, String filterString) {
        String transliteratedFilterString = StringUtils.translitirateToLatin(filterString);
        Collection<AbstractChat> resultCollection = new ArrayList<>();
        for (AbstractContact abstractContact : abstractContacts) {

            if (abstractContact.getUser().toString().contains(filterString)
                    || abstractContact.getUser().toString().contains(transliteratedFilterString)
                    || abstractContact.getName().contains(filterString)
                    || abstractContact.getName().contains(transliteratedFilterString))
                resultCollection.add(new RegularChat(abstractContact.getAccount(),
                        abstractContact.getUser()));
        }
        return resultCollection;
    }

    private ArrayList<AbstractChat> concatLists(ArrayList<AbstractChat> chatList,
                                                ArrayList<AbstractChat> contactsList) {
        ArrayList<AbstractChat> result = new ArrayList<>(chatList);
        for (AbstractChat abstractChat : contactsList) {
            boolean isDuplicating = false;
            for (AbstractChat abstractChat1 : chatList)
                if (abstractChat.getUser() == abstractChat1.getUser()) {
                    isDuplicating = true;
                    break;
                }
            if (!isDuplicating) result.add(abstractChat);
        }

        return result;
    }

}
