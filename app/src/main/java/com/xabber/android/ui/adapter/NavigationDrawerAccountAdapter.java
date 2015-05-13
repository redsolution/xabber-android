package com.xabber.android.ui.adapter;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.extension.avatar.AvatarManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class NavigationDrawerAccountAdapter extends BaseListEditorAdapter<String> {
    private final int[] accountColors;

    public NavigationDrawerAccountAdapter(Activity activity) {
        super(activity);

        accountColors = activity.getResources().getIntArray(R.array.account_action_bar);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        AccountManager accountManager = AccountManager.getInstance();
        if (convertView == null) {
            view = getActivity().getLayoutInflater().inflate(R.layout.contact_list_drawer_account_item, parent, false);
        } else {
            view = convertView;
        }
        String account = getItem(position);

        int accountColor = accountColors[accountManager.getColorLevel(account)];

        ((ImageView) view.findViewById(R.id.color)).setImageDrawable(new ColorDrawable(accountColor));
        ((ImageView) view.findViewById(R.id.avatar))
                .setImageDrawable(AvatarManager.getInstance().getAccountAvatar(account));

        ((TextView) view.findViewById(R.id.name)).setText(accountManager.getVerboseName(account));
        AccountItem accountItem = accountManager.getAccount(account);
        ConnectionState state;
        if (accountItem == null) {
            state = ConnectionState.offline;
        } else {
            state = accountItem.getState();
        }
        ((TextView) view.findViewById(R.id.status)).setText(getActivity().getString(state.getStringId()));
        return view;
    }

    @Override
    protected Collection<String> getTags() {
        List<String> list = new ArrayList<>();
        list.addAll(AccountManager.getInstance().getAccounts());
        Collections.sort(list);
        return list;
    }

}