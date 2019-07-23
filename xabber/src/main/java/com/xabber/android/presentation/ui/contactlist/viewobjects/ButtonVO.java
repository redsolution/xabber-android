package com.xabber.android.presentation.ui.contactlist.viewobjects;

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import com.xabber.android.R;
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

    private String title;
    private String action;
    private AccountJid account;

    public ButtonVO(int accountColorIndicator, String title, String action, AccountJid account) {

        this.id = UUID.randomUUID().toString();
        this.accountColorIndicator = accountColorIndicator;
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
    }

    public static ButtonVO convert(@Nullable AccountConfiguration configuration, String title, String action) {
        int accountColorIndicator = ColorManager.getInstance().getAccountPainter().getDefaultMainColor();
        AccountJid account = null;

        if (configuration != null) {
            account = configuration.getAccount();
            accountColorIndicator = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);
        }
        return new ButtonVO(accountColorIndicator, title, action, account);
    }

    public String getTitle() {
        return title;
    }

    public int getAccountColorIndicator() {
        return accountColorIndicator;
    }

    public String getAction() {
        return action;
    }

    public AccountJid getAccount() {
        return account;
    }

    public class ViewHolder extends FlexibleViewHolder {
        final Button btnListAction;

        public ViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);

            btnListAction = (Button) itemView.findViewById(R.id.btnListAction);
            btnListAction.setOnClickListener(this);
        }
    }

}
