package com.xabber.android.ui.adapter;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.color.ColorManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class NavigationDrawerAccountAdapter extends BaseListEditorAdapter<AccountJid> {

    public NavigationDrawerAccountAdapter(Activity activity) {
        super(activity);
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
        AccountJid account = getItem(position);

        ((ImageView) view.findViewById(R.id.color)).setImageDrawable(new ColorDrawable((ColorManager.getInstance().getAccountPainter().getAccountMainColor(account))));
        ((ImageView) view.findViewById(R.id.avatar)).setImageDrawable(AvatarManager.getInstance().getAccountAvatar(account));

        TextView accountName = (TextView) view.findViewById(R.id.name);

        try {
            accountName.setText(RosterManager.getInstance().getBestContact(account, UserJid.from(accountManager.getVerboseName(account))).getName());
        } catch (UserJid.UserJidCreateException e) {
            LogManager.exception(this, e);
        }
        accountName.setTextColor(ColorManager.getInstance().getAccountPainter().getAccountTextColor(account));

        ((TextView) view.findViewById(R.id.account_jid)).setText(accountManager.getVerboseName(account));

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
    protected Collection<AccountJid> getTags() {
        List<AccountJid> list = new ArrayList<>();
        list.addAll(AccountManager.getInstance().getEnabledAccounts());
        Collections.sort(list);
        return list;
    }

}