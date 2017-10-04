package com.xabber.android.ui.fragment;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.http.PatreonManager;
import com.xabber.android.data.http.XabberComClient;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.adapter.NavigationDrawerAccountAdapter;
import com.xabber.android.ui.color.AccountPainter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.widget.TextViewFadeAnimator;

import java.util.Collection;

public class ContactListDrawerFragment extends Fragment implements View.OnClickListener, OnAccountChangedListener, AdapterView.OnItemClickListener {

    ContactListDrawerListener listener;
    private NavigationDrawerAccountAdapter adapter;
    private ListView listView;
    private View divider;
    private View headerTitle;
    private ImageView drawerHeaderImage;
    private int[] headerImageResources;

    private LinearLayout llAccountInfo;
    private LinearLayout llNoAccount;
    private TextView tvAccountName;
    private TextView tvAccountEmail;

    private TextView tvPatreonTitle;
    private ProgressBar pbPatreon;
    private TextViewFadeAnimator animator;
    private String[] patreonTexts;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (ContactListDrawerListener) activity;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_drawer, container, false);

        // to avoid strange bug on some 4.x androids
        view.setBackgroundColor(ColorManager.getInstance().getNavigationDrawerBackgroundColor());

        try {
            ((TextView)view.findViewById(R.id.version))
                    .setText(getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0)
                            .versionName);
        } catch (PackageManager.NameNotFoundException e) {
            LogManager.exception(this, e);
        }

        View drawerHeader = view.findViewById(R.id.drawer_header);
        drawerHeaderImage = (ImageView) drawerHeader.findViewById(R.id.drawer_header_image);

        listView = (ListView) view.findViewById(R.id.drawer_account_list);

        View footerView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.contact_list_drawer_footer, listView, false);
        listView.addFooterView(footerView);

        View headerView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.contact_list_drawer_header, listView, false);
        headerTitle = headerView.findViewById(R.id.drawer_header_action_xmpp_accounts);
        headerTitle.setOnClickListener(this);

        view.findViewById(R.id.drawer_header_action_xabber_account).setOnClickListener(this);

        listView.addHeaderView(headerView);

        adapter = new NavigationDrawerAccountAdapter(getActivity());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        footerView.findViewById(R.id.drawer_action_settings).setOnClickListener(this);
        footerView.findViewById(R.id.drawer_action_about).setOnClickListener(this);
        footerView.findViewById(R.id.drawer_action_exit).setOnClickListener(this);

        divider = footerView.findViewById(R.id.drawer_divider);

        llAccountInfo = (LinearLayout) view.findViewById(R.id.accountInfo);
        llNoAccount = (LinearLayout) view.findViewById(R.id.noAccount);
        tvAccountName = (TextView) view.findViewById(R.id.tvAccountName);
        tvAccountEmail = (TextView) view.findViewById(R.id.tvAccountEmail);

        tvPatreonTitle = (TextView) view.findViewById(R.id.tvPatreonTitle);
        pbPatreon = (ProgressBar) view.findViewById(R.id.pbPatreon);
        view.findViewById(R.id.drawer_action_patreon).setOnClickListener(this);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        update();
    }

    @Override
    public void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onClick(View v) {
        listener.onContactListDrawerListener(v.getId());
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        update();
    }

    private void update() {
        adapter.onChange();

        Glide.with(this)
                .fromResource()
                .load(headerImageResources[AccountPainter.getDefaultAccountColorLevel()])
                .fitCenter()
                .into(drawerHeaderImage);

        if (adapter.getCount() == 0) {
            headerTitle.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
        } else {
            headerTitle.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
        }
        setupXabberAccountView();
        setupPatreonView();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        listener.onAccountSelected((AccountJid) listView.getItemAtPosition(position));
    }

    public interface ContactListDrawerListener {
        void onContactListDrawerListener(int viewId);

        void onAccountSelected(AccountJid account);
    }

    private void setupXabberAccountView() {
        XabberAccount account = XabberAccountManager.getInstance().getAccount();

        if (account != null) {
            llAccountInfo.setVisibility(View.VISIBLE);
            llNoAccount.setVisibility(View.GONE);

            String accountName = account.getFirstName() + " " + account.getLastName();
            if (accountName.trim().isEmpty())
                accountName = getActivity().getString(R.string.title_xabber_account);

            if (XabberAccount.STATUS_NOT_CONFIRMED.equals(account.getAccountStatus())) {
                tvAccountName.setText(accountName);
                tvAccountEmail.setText(R.string.title_email_confirm);
            }
            if (XabberAccount.STATUS_CONFIRMED.equals(account.getAccountStatus())) {
                tvAccountName.setText(accountName);
                tvAccountEmail.setText(R.string.title_complete_register);
            }
            if (XabberAccount.STATUS_REGISTERED.equals(account.getAccountStatus())) {

                tvAccountName.setText(accountName);
                tvAccountEmail.setText(getString(R.string.username, account.getUsername()));
            }
        } else {
            llAccountInfo.setVisibility(View.GONE);
            llNoAccount.setVisibility(View.VISIBLE);
        }
    }

    private void setupPatreonView() {
        XabberComClient.Patreon patreon = PatreonManager.getInstance().getPatreon();
        if (patreon != null) {
            XabberComClient.PatreonGoal currentGoal = null;
            for (XabberComClient.PatreonGoal goal: patreon.getGoals()) {
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
        animator = new TextViewFadeAnimator(tvPatreonTitle, patreonTexts);
        animator.startAnimation();
    }

    public void stopPatreonAnim() {
        if (animator != null)
            animator.stopAnimation();
        tvPatreonTitle.setText(patreonTexts[0]);
    }
}
