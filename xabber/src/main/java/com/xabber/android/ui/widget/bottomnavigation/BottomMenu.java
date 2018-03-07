package com.xabber.android.ui.widget.bottomnavigation;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by valery.miller on 05.10.17.
 */

public class BottomMenu extends Fragment implements View.OnClickListener {

    public interface OnClickListener {
        void onRecentClick();
        void onMenuClick();
        void onAccountShortcutClick(AccountJid jid);
        void onSearchClick();
        void onSearch(String filter);
    }

    private OnClickListener listener;
    private RecyclerView accountList;
    private RelativeLayout searchLayout;
    private LinearLayout controlView;
    private RelativeLayout expandSearchLayout;
    private ImageView btnSearch;
    private SearchView searchView;
    private TextView tvUnreadCount;
    private AccountShortcutAdapter adapter;

    private ArrayList<AccountShortcutVO> items = new ArrayList<>();

    public static BottomMenu newInstance() {
        return new BottomMenu();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            listener = (OnClickListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnClickListener");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            listener = (OnClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnClickListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_bottom_navigation, container, false);
        view.findViewById(R.id.btnRecent).setOnClickListener(this);
        view.findViewById(R.id.btnMenu).setOnClickListener(this);
        btnSearch = (ImageView) view.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(this);

        searchLayout = (RelativeLayout) view.findViewById(R.id.searchLayout);
        searchLayout.setOnClickListener(this);
        controlView = (LinearLayout) view.findViewById(R.id.controlView);
        expandSearchLayout = (RelativeLayout) view.findViewById(R.id.expandSearchLayout);

        accountList = (RecyclerView) view.findViewById(R.id.accountList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        accountList.setLayoutManager(layoutManager);

        adapter = new AccountShortcutAdapter(items, getActivity(), this);
        accountList.setAdapter(adapter);

        searchView = (SearchView) view.findViewById(R.id.searchView);
        searchView.setQueryHint(getString(R.string.contact_search_hint));

        searchView.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                listener.onSearch(newText);
                return true;
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                closeSearch();
                return true;
            }
        });

        tvUnreadCount = (TextView) view.findViewById(R.id.tvUnreadCount);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        update();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnRecent:
                listener.onRecentClick();
                break;
            case R.id.btnSearch:
                listener.onSearchClick();
                openSearch();
                break;
            case R.id.btnMenu:
                listener.onMenuClick();
                break;
            case R.id.searchLayout:
                listener.onSearchClick();
                openSearch();
                break;
            case R.id.avatarView:
                int position = accountList.getChildLayoutPosition(view);
                AccountJid accountJid = items.get(position).getAccountJid();
                listener.onAccountShortcutClick(accountJid);
                break;
        }
    }

    public void update() {
        ArrayList<AccountJid> list = new ArrayList<>();
        list.addAll(AccountManager.getInstance().getEnabledAccounts());
        Collections.sort(list);

        this.items.clear();
        this.items.addAll(AccountShortcutVO.convert(list));
        adapter = new AccountShortcutAdapter(items, getActivity(), this);
        accountList.setAdapter(adapter);
        setLayoutParamToRecyclerView();
    }

    public void setUnreadMessages(int count) {
        if (tvUnreadCount != null) {
            if (count > 0) {
                if (count > 99) count = 99;
                tvUnreadCount.setText(String.valueOf(count));
                tvUnreadCount.setVisibility(View.VISIBLE);
            } else tvUnreadCount.setVisibility(View.GONE);
        }
    }

    private void setLayoutParamToRecyclerView() {
        float weightList;
        float weightSearch;

        switch (items.size()) {
            case 1:
                weightList = 1.0f;
                weightSearch = 0.3f;
                searchLayout.setVisibility(View.VISIBLE);
                btnSearch.setVisibility(View.GONE);
                break;
            case 2:
                weightList = 1.0f;
                weightSearch = 0.7f;
                searchLayout.setVisibility(View.VISIBLE);
                btnSearch.setVisibility(View.GONE);
                break;
            case 3:
                weightList = 0.33f;
                weightSearch = 1.0f;
                searchLayout.setVisibility(View.GONE);
                btnSearch.setVisibility(View.VISIBLE);
                break;
            case 4:
                weightList = 0.25f;
                weightSearch = 1.0f;
                searchLayout.setVisibility(View.GONE);
                btnSearch.setVisibility(View.VISIBLE);
                break;
            default:
                weightList = 0.2f;
                weightSearch = 1.0f;
                searchLayout.setVisibility(View.GONE);
                btnSearch.setVisibility(View.VISIBLE);
                break;
        }

        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                weightList);
        accountList.setLayoutParams(param);

        LinearLayout.LayoutParams searchParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                weightSearch);
        searchLayout.setLayoutParams(searchParam);
    }

    private void openSearch() {
        expandSearchLayout.setVisibility(View.VISIBLE);
        controlView.setVisibility(View.GONE);
        searchView.setIconified(false);
        searchView.requestFocus();
        ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).
                toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public void closeSearch() {
        expandSearchLayout.setVisibility(View.GONE);
        controlView.setVisibility(View.VISIBLE);
        searchView.setQuery("", true);
        searchView.clearFocus();
    }
}
