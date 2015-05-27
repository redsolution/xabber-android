package com.xabber.android.ui;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.ui.adapter.ChatListAdapter;
import com.xabber.android.ui.helper.AccountPainter;
import com.xabber.android.ui.helper.ChatScroller;

import java.util.List;

public class RecentChatFragment extends ListFragment implements Toolbar.OnMenuItemClickListener {

    private ChatScroller.ChatScrollerProvider listener = null;
    private AccountPainter accountPainter;
    private Toolbar toolbar;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RecentChatFragment() {
    }

    public static RecentChatFragment newInstance() {
        return  new RecentChatFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setListAdapter(new ChatListAdapter(getActivity()));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof ChatScroller.ChatScrollerProvider)) {
            throw new ClassCastException(activity.toString()
                    + " must implement ChatScrollerProvider");
        }

        listener = (ChatScroller.ChatScrollerProvider) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.recent_chats, container, false);

        toolbar = (Toolbar) rootView.findViewById(R.id.toolbar_default);
        toolbar.setTitle(R.string.recent_chats);

        if (!getResources().getBoolean(R.bool.landscape)) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NavUtils.navigateUpFromSameTask(getActivity());
                }
            });
        }
        toolbar.inflateMenu(R.menu.recent_chats);
        toolbar.setOnMenuItemClickListener(this);

        accountPainter = new AccountPainter(getActivity());

        updateChats();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != listener.getChatScroller()) {
            listener.getChatScroller().registerRecentChatsList(this);
        }
        toolbar.setBackgroundColor(accountPainter.getDefaultMainColor());
    }

    @Override
    public void onPause() {
        super.onPause();

        if (null != listener.getChatScroller()) {
            listener.getChatScroller().unregisterRecentChatsList(this);
        }
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (null != listener.getChatScroller()) {
            listener.getChatScroller().onChatSelected((AbstractChat) getListAdapter().getItem(position));
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_under_construction) {
            Toast.makeText(getActivity(), getActivity().getString(R.string.under_construction_message), Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    public void updateChats() {
        if (listener.getChatScroller().isInitialized()) {
            ((ChatListAdapter) getListAdapter()).updateChats(listener.getChatScroller().getActiveChats());
        }
    }

    public interface RecentChatFragmentInteractionListener {
        void onChatSelected(AbstractChat chat);

        void registerRecentChatsList(RecentChatFragment fragment);

        void unregisterRecentChatsList(RecentChatFragment fragment);

        List<AbstractChat> getActiveChats();
    }
}
