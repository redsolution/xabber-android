package com.xabber.android.presentation.ui.contactlist;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.presentation.mvp.contactlist.ContactListPresenter;
import com.xabber.android.presentation.mvp.contactlist.ContactListView;
import com.xabber.android.presentation.ui.contactlist.viewobjects.AccountVO;
import com.xabber.android.ui.activity.ContactListActivity;

import java.util.ArrayList;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

/**
 * Created by valery.miller on 02.02.18.
 */

public class ContactListFragment extends Fragment implements ContactListView,
        FlexibleAdapter.OnStickyHeaderChangeListener, FlexibleAdapter.OnItemClickListener {

    public static final String ACCOUNT_JID = "account_jid";

    private ContactListPresenter presenter;

    private FlexibleAdapter<IFlexible> adapter;
    private List<IFlexible> items;

    public static ContactListFragment newInstance(@Nullable AccountJid account) {
        ContactListFragment fragment = new ContactListFragment();
        Bundle args = new Bundle();
        if (account != null)
            args.putSerializable(ACCOUNT_JID, account);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact_list_new, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        items = new ArrayList<>();
        adapter = new FlexibleAdapter<>(items, null, true);

        adapter.setStickyHeaders(true);
        adapter.setDisplayHeadersAtStartUp(true);
        recyclerView.setAdapter(adapter);

        adapter.setSwipeEnabled(true);
        adapter.expandItemsAtStartUp();
        adapter.addListener(this);
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter = ContactListPresenter.getInstance(getActivity());
        ((ContactListActivity)getActivity()).setStatusBarColor();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.onLoadContactList(this);
    }

    @Override
    public void updateItems(List<IFlexible> items) {
        this.items.clear();
        this.items.addAll(items);
        adapter.updateDataSet(items);
    }

    /**
     * Callback. When new header becomes sticky - change color of status bar.
     */
    @Override
    public void onStickyHeaderChange(int newPosition, int oldPosition) {
        if (newPosition > -1 && adapter.getItemCount() > newPosition) {
            IFlexible item = adapter.getItem(newPosition);
            if (item instanceof AccountVO)
                ((ContactListActivity)getActivity()).setStatusBarColor(((AccountVO)item).getAccountJid()); // account color
            else ((ContactListActivity)getActivity()).setStatusBarColor(); // main color
        }
    }

    @Override
    public boolean onItemClick(int position) {
        adapter.notifyItemChanged(position);
        return true;
    }
}
