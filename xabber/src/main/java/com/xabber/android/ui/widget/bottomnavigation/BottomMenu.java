package com.xabber.android.ui.widget.bottomnavigation;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

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
    }

    private OnClickListener listener;
    private RecyclerView accountList;
    private RelativeLayout searchView;
    private LinearLayout controlView;
    private RelativeLayout expandSearchView;
    private ImageView btnSarch;
    private ImageView btnSearchClose;
    private EditText edtSearch;
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_bottom_navigation, container, false);
        view.findViewById(R.id.btnRecent).setOnClickListener(this);
        view.findViewById(R.id.btnMenu).setOnClickListener(this);
        btnSarch = (ImageView) view.findViewById(R.id.btnSearch);
        btnSarch.setOnClickListener(this);
        btnSearchClose = (ImageView) view.findViewById(R.id.btnSearchClose);
        btnSearchClose.setOnClickListener(this);

        searchView = (RelativeLayout) view.findViewById(R.id.searchView);
        searchView.setOnClickListener(this);
        controlView = (LinearLayout) view.findViewById(R.id.controlView);
        expandSearchView = (RelativeLayout) view.findViewById(R.id.expandSearchView);

        accountList = (RecyclerView) view.findViewById(R.id.accountList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        accountList.setLayoutManager(layoutManager);

        adapter = new AccountShortcutAdapter(items, getActivity(), this);
        accountList.setAdapter(adapter);

        edtSearch = (EditText) view.findViewById(R.id.edtSearch2);
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(edtSearch, InputMethodManager.SHOW_IMPLICIT);
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
                openSearch();
                break;
            case R.id.btnSearchClose:
                closeSearch();
                break;
            case R.id.btnMenu:
                listener.onMenuClick();
                break;
            case R.id.searchView:
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

    private void setLayoutParamToRecyclerView() {
        float weightList;
        float weightSearch;

        switch (items.size()) {
            case 1:
                weightList = 1.0f;
                weightSearch = 0.3f;
                searchView.setVisibility(View.VISIBLE);
                btnSarch.setVisibility(View.GONE);
                break;
            case 2:
                weightList = 1.0f;
                weightSearch = 0.7f;
                searchView.setVisibility(View.VISIBLE);
                btnSarch.setVisibility(View.GONE);
                break;
            case 3:
                weightList = 0.33f;
                weightSearch = 1.0f;
                searchView.setVisibility(View.GONE);
                btnSarch.setVisibility(View.VISIBLE);
                break;
            case 4:
                weightList = 0.25f;
                weightSearch = 1.0f;
                searchView.setVisibility(View.GONE);
                btnSarch.setVisibility(View.VISIBLE);
                break;
            default:
                weightList = 0.2f;
                weightSearch = 1.0f;
                searchView.setVisibility(View.GONE);
                btnSarch.setVisibility(View.VISIBLE);
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
        searchView.setLayoutParams(searchParam);
    }

    private void openSearch() {
        expandSearchView.setVisibility(View.VISIBLE);
        controlView.setVisibility(View.GONE);
        edtSearch.requestFocus();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(edtSearch, 0);
    }

    private void closeSearch() {
        expandSearchView.setVisibility(View.GONE);
        controlView.setVisibility(View.VISIBLE);
        edtSearch.clearFocus();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edtSearch.getWindowToken(), 0);
    }
}
