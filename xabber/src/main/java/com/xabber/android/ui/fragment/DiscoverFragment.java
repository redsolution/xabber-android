package com.xabber.android.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.ui.OnAccountChangedListener;
import com.xabber.android.ui.OnContactChangedListener;
import com.xabber.android.ui.activity.SearchActivity;
import com.xabber.android.ui.color.ColorManager;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class DiscoverFragment extends Fragment implements View.OnClickListener,
        OnAccountChangedListener, OnContactChangedListener {

    /* Toolbar variables */
    private RelativeLayout toolbarRelativeLayout;
    private View toolbarAccountColorIndicator;
    private View toolbarAccountColorIndicatorBack;
    private ImageView toolbarAvatarIv;
    private ImageView toolbarStatusIv;

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

        toolbarRelativeLayout = view.findViewById(R.id.discover_toolbar_relative_layout);
        toolbarAccountColorIndicator = view.findViewById(R.id.accountColorIndicator);
        toolbarAccountColorIndicatorBack = view.findViewById(R.id.accountColorIndicatorBack);

        toolbarAvatarIv = view.findViewById(R.id.ivAvatar);
        toolbarStatusIv = view.findViewById(R.id.ivStatus);

        view.findViewById(R.id.discover_toolbar_tune_image_view).setOnClickListener(this);
        view.findViewById(R.id.discover_toolbar_edittext).setOnClickListener(this);
        toolbarAvatarIv.setOnClickListener(this);

        updateToolbar();

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateToolbar();
    }

    /**
     * OnClickListener for Toolbar
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.discover_toolbar_tune_image_view:
                Toast.makeText(getContext(), "Under construction", Toast.LENGTH_SHORT).show();
                break;
            case R.id.discover_toolbar_edittext:
                startSearchActivity();
                break;
        }
    }

    private void startSearchActivity() {
        Intent intent = SearchActivity.Companion.createSearchIntent(requireContext());
        startActivity(intent);
    }

    /**
     * Update toolbarRelativeLayout via current state
     */
    public void updateToolbar() {
        if (!isAdded()) return;
        /* Update avatar and status ImageViews via current settings and main user */
        if (SettingsManager.contactsShowAvatars() && AccountManager.INSTANCE.getEnabledAccounts().size() != 0) {
            toolbarAvatarIv.setVisibility(View.VISIBLE);
            toolbarStatusIv.setVisibility(View.VISIBLE);
            AccountJid mainAccountJid = AccountManager.INSTANCE.getFirstAccount();
            AccountItem mainAccountItem = AccountManager.INSTANCE.getAccount(mainAccountJid);
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
                AccountManager.INSTANCE.getFirstAccount() != null)
            toolbarRelativeLayout.setBackgroundColor(
                    ColorManager.getInstance().getAccountPainter().getAccountRippleColor(
                            AccountManager.INSTANCE.getFirstAccount()
                    )
            );
        else {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getContext().getTheme();
            theme.resolveAttribute(R.attr.bars_color, typedValue, true);
            toolbarRelativeLayout.setBackgroundColor(typedValue.data);
        }

        /* Update left color indicator via current main user */
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light
                && AccountManager.INSTANCE.getEnabledAccounts().size() > 1) {
            toolbarAccountColorIndicator.setBackgroundColor(
                    ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
            toolbarAccountColorIndicatorBack.setBackgroundColor(
                    ColorManager.getInstance().getAccountPainter().getDefaultIndicatorBackColor());
        } else {
            toolbarAccountColorIndicator.setBackgroundColor(getResources().getColor(R.color.transparent));
            toolbarAccountColorIndicatorBack.setBackgroundColor(getResources().getColor(R.color.transparent));
        }
    }

    @Override
    public void onAccountsChanged(@org.jetbrains.annotations.Nullable Collection<? extends AccountJid> accounts) {
        Application.getInstance().runOnUiThread(this::updateToolbar);
    }

    @Override
    public void onContactsChanged(@NotNull Collection<? extends RosterContact> entities) {
        Application.getInstance().runOnUiThread(this::updateToolbar);
    }

}
