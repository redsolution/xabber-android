package com.xabber.android.ui.fragment;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.http.PatreonManager;
import com.xabber.android.data.http.XabberComClient;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.activity.AccountActivity;
import com.xabber.android.ui.activity.AccountAddActivity;
import com.xabber.android.ui.activity.AccountListActivity;
import com.xabber.android.ui.activity.MainActivity;
import com.xabber.android.ui.activity.StatusEditActivity;
import com.xabber.android.ui.adapter.AccountListPreferenceAdapter;
import com.xabber.android.ui.color.AccountPainter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.dialog.AccountDeleteDialog;
import com.xabber.android.ui.widget.TextViewFadeAnimator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class MainActivitySettingsFragment extends Fragment implements View.OnClickListener,
        OnAccountChangedListener, AdapterView.OnItemClickListener,
        AccountListPreferenceAdapter.Listener {

    ContactListDrawerListener listener;
    private CompositeSubscription compositeSubscription = new CompositeSubscription();
    private ImageView drawerHeaderImage;
    private int[] headerImageResources;

    private TextView tvAccountName;
    private TextView tvAccountEmail;
    private ImageView ivSync;

    private TextView tvPatreonTitle;
    private ProgressBar pbPatreon;
    private TextViewFadeAnimator animator;
    private String[] patreonTexts;

    private AccountListPreferenceAdapter accountListAdapter;
    private ImageView ivReorder;

    public static MainActivitySettingsFragment newInstance() {
        return new MainActivitySettingsFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ContactListDrawerListener)
            listener = (ContactListDrawerListener) activity;
        else throw new RuntimeException(activity.toString()
                + " must implement ContactListDrawerFragment.ContactListDrawerListener");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TypedArray defaultAvatars = Application.getInstance().getResources()
                .obtainTypedArray(R.array.navigation_drawer_header_images);
        headerImageResources = new int[defaultAvatars.length()];
        for (int index = 0; index < defaultAvatars.length(); index++) {
            headerImageResources[index] = defaultAvatars.getResourceId(index, -1);
        }
        defaultAvatars.recycle();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_drawer, container, false);

        // to avoid strange bug on some 4.x androids
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            view.setBackgroundColor(ColorManager.getInstance()
                    .getNavigationDrawerBackgroundColor());
        }

        try {
            ((TextView) view.findViewById(R.id.version)).setText(getActivity().getPackageManager()
                    .getPackageInfo(getActivity().getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            LogManager.exception(this, e);
        }

        View drawerHeader = view.findViewById(R.id.drawer_header);
        drawerHeaderImage = drawerHeader.findViewById(R.id.drawer_header_image);
        drawerHeaderImage.setImageResource(
                headerImageResources[AccountPainter.getDefaultAccountColorLevel()]);

        view.findViewById(R.id.drawer_header_action_xabber_account).setOnClickListener(this);

        ivSync = view.findViewById(R.id.ivSync);
        ImageView ivTheme = view.findViewById(R.id.ivThemeChange);
        ivTheme.setOnClickListener(this);
        tvAccountName = view.findViewById(R.id.tvAccountName);
        tvAccountEmail = view.findViewById(R.id.tvAccountEmail);

        tvPatreonTitle = view.findViewById(R.id.tvPatreonTitle);
        pbPatreon = view.findViewById(R.id.pbPatreon);
        view.findViewById(R.id.drawer_action_patreon).setOnClickListener(this);

        view.findViewById(R.id.drawer_action_settings).setOnClickListener(this);
        view.findViewById(R.id.drawer_action_about).setOnClickListener(this);
        view.findViewById(R.id.drawer_action_exit).setOnClickListener(this);

        RecyclerView recyclerView = view
                .findViewById(R.id.account_list_recycler_view);

        accountListAdapter = new AccountListPreferenceAdapter(getActivity(), this);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(accountListAdapter);
        recyclerView.setNestedScrollingEnabled(false);

        ivReorder = view.findViewById(R.id.ivReorder);
        ivReorder.setOnClickListener(this);
        Button btnAddAccount = view.findViewById(R.id.btnAddAccount);
        btnAddAccount.setOnClickListener(this);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        update();
        subscribeForXabberAccount();
    }

    @Override
    public void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        stopPatreonAnim();
        compositeSubscription.clear();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivReorder:
                startActivity(AccountListActivity.createIntent(getActivity()));
                break;
            case R.id.btnAddAccount:
                startActivity(AccountAddActivity.createIntent(getActivity()));
                break;
            case R.id.ivThemeChange:
                changeTheme();
                break;
            default:
                listener.onContactListDrawerListener(v.getId());
                break;
        }
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        update();
    }

    private void update() {
        drawerHeaderImage.setImageResource(
                headerImageResources[AccountPainter.getDefaultAccountColorLevel()]);

        setupPatreonView();
        setupAccountList();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    private void setupXabberAccountView(XabberAccount account) {
        if (account != null) {
            String accountName = account.getFirstName() + " " + account.getLastName();
            if (accountName.trim().isEmpty())
                accountName = getActivity().getString(R.string.title_xabber_account);
            tvAccountName.setText(accountName);
            tvAccountEmail.setText(account.getFullUsername());
            ivSync.setImageResource(R.drawable.ic_sync_done);

        } else {
            tvAccountName.setText(R.string.title_xabber_account);
            tvAccountEmail.setText(R.string.not_login);
            ivSync.setImageResource(R.drawable.ic_sync_disable);
        }
    }

    private void setupPatreonView() {
        XabberComClient.Patreon patreon = PatreonManager.getInstance().getPatreon();
        if (patreon != null) {
            XabberComClient.PatreonGoal currentGoal = null;
            for (XabberComClient.PatreonGoal goal : patreon.getGoals()) {
                if (goal.getGoal() > patreon.getPledged()) {
                    currentGoal = goal;
                    break;
                }
            }

            if (currentGoal != null) {
                patreonTexts = new String[3];
                patreonTexts[0] = patreon.getString();
                patreonTexts[1] = getString(R.string.patreon_pledged, patreon.getPledged(), currentGoal.getGoal());
                patreonTexts[2] = getString(R.string.patreon_current_goal, currentGoal.getTitle());

                tvPatreonTitle.setSelected(true);
                pbPatreon.setMax(currentGoal.getGoal());
                pbPatreon.setProgress(patreon.getPledged());

                animator = new TextViewFadeAnimator(tvPatreonTitle, patreonTexts);
                startPatreonAnim();
            }
        }
    }

    public void startPatreonAnim() {
        if (patreonTexts != null && patreonTexts.length > 0) {
            animator = new TextViewFadeAnimator(tvPatreonTitle, patreonTexts);
            animator.startAnimation();
        }
    }

    public void stopPatreonAnim() {
        if (animator != null)
            animator.stopAnimation();
        if (patreonTexts != null && patreonTexts.length > 0)
            tvPatreonTitle.setText(patreonTexts[0]);
    }

    private void setupAccountList() {
        List<AccountItem> accountItems = new ArrayList<>();
        for (AccountItem accountItem : AccountManager.getInstance().getAllAccountItems()) {
            accountItems.add(accountItem);
        }
        accountListAdapter.setAccountItems(accountItems);

        if (accountItems.size() > 1) ivReorder.setVisibility(View.VISIBLE);
        else ivReorder.setVisibility(View.GONE);
    }

    @Override
    public void onAccountClick(AccountJid account) {
        startActivity(AccountActivity.createIntent(getActivity(), account));
    }

    @Override
    public void onEditAccountStatus(AccountItem accountItem) {
        startActivity(StatusEditActivity.createIntent(getActivity(), accountItem.getAccount()));
    }

    @Override
    public void onEditAccount(AccountItem accountItem) {
        startActivity(AccountActivity.createIntent(getActivity(), accountItem.getAccount()));
    }

    @Override
    public void onDeleteAccount(AccountItem accountItem) {
        AccountDeleteDialog.newInstance(accountItem.getAccount()).show(getFragmentManager(),
                AccountDeleteDialog.class.getName());
    }

    private void changeTheme() {
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
            SettingsManager.setLightTheme();
        } else {
            SettingsManager.setDarkTheme();
        }
        ActivityManager.getInstance().clearStack(true);
        Activity activity = getActivity();
        if (activity != null) {
            Intent intent = MainActivity.createFragmentIntent(activity,
                    MainActivity.ActiveFragmentType.SETTINGS);
            startActivity(intent);
            activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
    }

    private void subscribeForXabberAccount() {
        compositeSubscription.add(XabberAccountManager.getInstance().subscribeForAccount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(account -> setupXabberAccountView(account)).subscribe());
    }

    public interface ContactListDrawerListener {
        void onContactListDrawerListener(int viewId);

        void onAccountSelected(AccountJid account);
    }
}
