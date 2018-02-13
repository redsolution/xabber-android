package com.xabber.android.presentation.ui.contactlist.viewobjects;

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.ui.adapter.contactlist.AccountConfiguration;
import com.xabber.android.ui.color.ColorManager;

import java.util.List;
import java.util.UUID;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Created by valery.miller on 07.02.18.
 */

public class ButtonVO extends AbstractFlexibleItem<ButtonVO.ViewHolder> {

    public final static String ACTION_ADD_CONTACT = "Add contact";

    private String id;

    private int accountColorIndicator;
    private boolean showOfflineShadow;

    private String title;
    private String action;
    private AccountJid account;

    public ButtonVO(int accountColorIndicator, boolean showOfflineShadow,
                    String title, String action, AccountJid account) {

        this.id = UUID.randomUUID().toString();
        this.accountColorIndicator = accountColorIndicator;
        this.showOfflineShadow = showOfflineShadow;
        this.title = title;
        this.action = action;
        this.account = account;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ButtonVO) {
            ButtonVO inItem = (ButtonVO) o;
            return this.id.equals(inItem.id);
        }
        return false;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_button_in_contact_list;
    }

    @Override
    public ViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new ViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, ViewHolder holder, int position, List<Object> payloads) {

        holder.btnListAction.setText(getTitle());

        /** set up OFFLINE SHADOW */
        if (isShowOfflineShadow())
            holder.offlineShadow.setVisibility(View.VISIBLE);
        else holder.offlineShadow.setVisibility(View.GONE);
    }

    public static ButtonVO convert(@Nullable AccountConfiguration configuration, String title, String action) {
        boolean showOfflineShadow = false;
        int accountColorIndicator = ColorManager.getInstance().getAccountPainter().getDefaultMainColor();
        AccountJid account = null;

        if (configuration != null) {
            account = configuration.getAccount();
            accountColorIndicator = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);

            AccountItem accountItem = AccountManager.getInstance().getAccount(configuration.getAccount());
            if (accountItem != null) {
                StatusMode statusMode = accountItem.getDisplayStatusMode();
                if (statusMode == StatusMode.unavailable || statusMode == StatusMode.connection)
                    showOfflineShadow = true;
            }
        }
        return new ButtonVO(accountColorIndicator, showOfflineShadow, title, action, account);
    }

    public String getTitle() {
        return title;
    }

    public int getAccountColorIndicator() {
        return accountColorIndicator;
    }

    public boolean isShowOfflineShadow() {
        return showOfflineShadow;
    }

    public String getAction() {
        return action;
    }

    public AccountJid getAccount() {
        return account;
    }

    public class ViewHolder extends FlexibleViewHolder {
        final Button btnListAction;
        final ImageView offlineShadow;

        public ViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);

            offlineShadow = (ImageView) view.findViewById(R.id.offline_shadow);
            btnListAction = (Button) itemView.findViewById(R.id.btnListAction);
            btnListAction.setOnClickListener(this);
        }
    }

}
