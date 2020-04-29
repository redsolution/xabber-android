package com.xabber.android.ui.fragment;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.AppBarLayout;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.ui.activity.ContactAddActivity;
import com.xabber.android.ui.activity.SearchActivity;
import com.xabber.android.ui.activity.StatusEditActivity;
import com.xabber.android.ui.color.ColorManager;

import java.util.Collection;


public class CallsFragment extends Fragment implements View.OnClickListener, PopupMenu.OnMenuItemClickListener, OnAccountChangedListener, OnContactChangedListener {

    private CoordinatorLayout coordinatorLayout;

    /* Toolbar variables */
    private RelativeLayout toolbarRelativeLayout;
    private AppBarLayout toolbarAppBarLayout;
    private Toolbar toolbarToolbarLayout;
    private View toolbarAccountColorIndicator;
    private View toolbarAccountColorIndicatorBack;
    private ImageView toolbarAddIv;
    private TextView toolbarTitleTv;
    private ImageView toolbarAvatarIv;
    private ImageView toolbarStatusIv;
    private ImageView toolbarSearchIv;

    public static CallsFragment newInstance() {
        return new CallsFragment();
    }

    @Override
    public void onAttach(Context context) {
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        super.onAttach(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calls, container, false);
        coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.chatlist_coordinator_layout);
        toolbarRelativeLayout = view.findViewById(R.id.toolbar_chatlist);
        toolbarToolbarLayout = view.findViewById(R.id.chat_list_toolbar);
        toolbarAccountColorIndicator = view.findViewById(R.id.accountColorIndicator);
        toolbarAccountColorIndicatorBack = view.findViewById(R.id.accountColorIndicatorBack);
        toolbarAddIv = (ImageView) view.findViewById(R.id.ivAdd);
        toolbarAddIv.setVisibility(View.GONE);
        toolbarTitleTv = (TextView) view.findViewById(R.id.tvTitle);
        toolbarAvatarIv = (ImageView) view.findViewById(R.id.ivAvatar);
        toolbarStatusIv = (ImageView) view.findViewById(R.id.ivStatus);
        toolbarSearchIv = (ImageView) view.findViewById(R.id.toolbar_search_button);
        toolbarAppBarLayout = view.findViewById(R.id.chatlist_toolbar_root);
        //toolbarAddIv.setOnClickListener(this);
        toolbarAvatarIv.setOnClickListener(this);
        //toolbarTitleTv.setOnClickListener(this);
        toolbarSearchIv.setOnClickListener(this);
        toolbarTitleTv.setText("Calls");

        ((TextView) view.findViewById(R.id.calls_text_explanation))
                .setMovementMethod(LinkMovementMethod.getInstance());

        return view;
    }

    @Override
    public void onDetach() {
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class,this);
        super.onDetach();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    /** OnClickListener for Toolbar */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivAdd:
                //showToolbarPopup(toolbarAddIv);
                break;
            case R.id.ivAvatar:
                startActivity(StatusEditActivity.createIntent(getActivity()));
                break;
            case R.id.tvTitle:

                break;
            case R.id.toolbar_search_button:
                startActivity(SearchActivity.createSearchIntent(getActivity()));
                break;
        }
    }

    /** Show menu Add contact / Add conference */
    //private void showToolbarPopup(View v) {
    //    PopupMenu popupMenu = new PopupMenu(getContext(), v);
    //    popupMenu.setOnMenuItemClickListener(this);
    //    popupMenu.inflate(R.menu.menu_add_in_contact_list);
    //    popupMenu.show();
    //}

    /** Handle toolbarRelativeLayout menus clicks */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_contact:
                startActivity(ContactAddActivity.createIntent(getActivity()));
                return true;
            default:
                return false;
        }
    }

    /** Update toolbarRelativeLayout via current state */
    public void updateToolbar(){
        setupToolbarLayout();
        /* Update avatar and status ImageViews via current settings and main user */
        if (SettingsManager.contactsShowAvatars() && AccountManager.getInstance().getEnabledAccounts().size() != 0){
            toolbarAvatarIv.setVisibility(View.VISIBLE);
            toolbarStatusIv.setVisibility(View.VISIBLE);
            AccountJid mainAccountJid = AccountManager.getInstance().getFirstAccount();
            AccountItem mainAccountItem = AccountManager.getInstance().getAccount(mainAccountJid);
            Drawable mainAccountAvatar = AvatarManager.getInstance().getAccountAvatar(mainAccountJid);
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
                && AccountManager.getInstance().getEnabledAccounts().size() > 1){
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

    /**
     * Setup Toolbar scroll behavior according to ... TODO add proper explanation and logic
     */
    public void setupToolbarLayout() {
        //Simply migrated from ChatListFragment atm, should add proper logic when we start working at this activity

        setToolbarScrollEnabled(false);
        /*
        if (recyclerView != null){
            int count = items.size();
            if (count <= maxItemsOnScreen){
                setToolbarScrollEnabled(false);
            } else {    setToolbarScrollEnabled(true);  }
        }
         */
    }

    /**
     * Enable or disable Toolbar scroll behavior
     * @param enabled
     */
    private void setToolbarScrollEnabled(boolean enabled){
        AppBarLayout.LayoutParams toolbarLayoutParams = (AppBarLayout.LayoutParams) toolbarToolbarLayout.getLayoutParams();
        CoordinatorLayout.LayoutParams appBarLayoutParams = (CoordinatorLayout.LayoutParams) toolbarAppBarLayout.getLayoutParams();
        if (enabled && toolbarLayoutParams.getScrollFlags() == 0){
            appBarLayoutParams.setBehavior(new AppBarLayout.Behavior());
            toolbarLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        } else if (!enabled && toolbarLayoutParams.getScrollFlags() != 0) {
            toolbarLayoutParams.setScrollFlags(0);
            appBarLayoutParams.setBehavior(null);
        }
        toolbarToolbarLayout.setLayoutParams(toolbarLayoutParams);
        toolbarAppBarLayout.setLayoutParams(appBarLayoutParams);
    }

    public void update() {
        updateToolbar();
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        update();
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        update();
    }
}
