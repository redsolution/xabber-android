package com.xabber.android.presentation.ui.contactlist.viewobjects;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.ShowOfflineMode;
import com.xabber.android.ui.adapter.contactlist.AccountConfiguration;
import com.xabber.android.ui.color.ColorManager;

import java.util.List;
import java.util.UUID;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IHeader;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Created by valery.miller on 05.02.18.
 */

public class AccountVO extends AbstractFlexibleItem<AccountVO.ViewHolder> implements IHeader<AccountVO.ViewHolder> {

    private String id;
    private String name;
    private String jid;
    private String status;
    private int statusLevel;
    private int statusId;
    private Drawable avatar;
    private int offlineModeLevel;
    private String contactCount;
    private AccountJid accountJid;
    private boolean isExpand;
    private String groupName;

    public AccountVO(String name, String jid, String status, int statusLevel, int statusId,
                     Drawable avatar, int offlineModeLevel, String contactCount, AccountJid accountJid,
                     boolean isExpand, String groupName) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.jid = jid;
        this.status = status;
        this.statusLevel = statusLevel;
        this.statusId = statusId;
        this.avatar = avatar;
        this.offlineModeLevel = offlineModeLevel;
        this.contactCount = contactCount;
        this.accountJid = accountJid;
        this.isExpand = isExpand;
        this.groupName = groupName;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AccountVO) {
            AccountVO inItem = (AccountVO) o;
            return this.id.equals(inItem.id);
        }
        return false;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_account_in_contact_list_new;
    }

    @Override
    public AccountVO.ViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new AccountVO.ViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, ViewHolder viewHolder, int position, List<Object> payloads) {
        Context context = viewHolder.itemView.getContext();

//        final int level = AccountManager.getInstance().getColorLevel(getAccountJid());
//        viewHolder.itemView.setBackgroundColor(accountGroupColors[level]);

        //viewHolder.accountColorIndicator.setBackgroundColor(viewObject.getAccountColorIndicator());

        viewHolder.tvAccountName.setText(getName());
        viewHolder.tvContactCount.setText(getContactCount());

        String statusText = getStatus();
        if (statusText.isEmpty()) statusText = context.getString(getStatusId());

        viewHolder.tvStatus.setText(statusText);

        if (SettingsManager.contactsShowAvatars()) {
            viewHolder.ivAvatar.setVisibility(View.VISIBLE);
            viewHolder.ivStatus.setVisibility(View.VISIBLE);
            viewHolder.ivAvatar.setImageDrawable(getAvatar());
        } else {
            viewHolder.ivAvatar.setVisibility(View.GONE);
            viewHolder.ivStatus.setVisibility(View.GONE);
        }

        viewHolder.ivStatus.setImageLevel(getStatusLevel());

        Drawable offlineModeImage = context.getResources().getDrawable(R.drawable.ic_show_offline_small);
        offlineModeImage.setLevel(getOfflineModeLevel());
        viewHolder.tvContactCount.setCompoundDrawablesWithIntrinsicBounds(offlineModeImage, null, null, null);

//        if (isShowOfflineShadow())
//            viewHolder.offlineShadow.setVisibility(View.VISIBLE);
//        else viewHolder.offlineShadow.setVisibility(View.GONE);
    }

    public static AccountVO convert(AccountConfiguration configuration) {
        String jid;
        String name;
        String status;
        int statusLevel;
        int statusId;
        Drawable avatar;
        int offlineModeLevel;
        boolean showOfflineShadow = false;
        int accountColorIndicator;
        String contactCount;

        AccountJid account = configuration.getAccount();

        accountColorIndicator = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);

        jid = GroupManager.getInstance().getGroupName(account, configuration.getGroup());
        name = AccountManager.getInstance().getNickName(account);

        contactCount = configuration.getOnline() + "/" + configuration.getTotal();

        AccountItem accountItem = AccountManager.getInstance().getAccount(account);

        status = accountItem.getStatusText().trim();

        statusId = accountItem.getDisplayStatusMode().getStringID();

        avatar = AvatarManager.getInstance().getAccountAvatar(account);


        statusLevel = accountItem.getDisplayStatusMode().getStatusLevel();

        ShowOfflineMode showOfflineMode = configuration.getShowOfflineMode();
        if (showOfflineMode == ShowOfflineMode.normal) {
            if (SettingsManager.contactsShowOffline()) {
                showOfflineMode = ShowOfflineMode.always;
            } else {
                showOfflineMode = ShowOfflineMode.never;
            }
        }

        offlineModeLevel = showOfflineMode.ordinal();


        StatusMode statusMode = accountItem.getDisplayStatusMode();

        if (statusMode == StatusMode.unavailable || statusMode == StatusMode.connection) {
            showOfflineShadow = true;
        } else {
            showOfflineShadow = false;
        }

        return new AccountVO(name, jid, status, statusLevel,
                statusId, avatar, offlineModeLevel, contactCount, configuration.getAccount(),
                configuration.isExpanded(), configuration.getGroup());
    }

    public String getName() {
        return name;
    }

    public String getJid() {
        return jid;
    }

    public String getStatus() {
        return status;
    }

    public int getStatusLevel() {
        return statusLevel;
    }

    public int getStatusId() {
        return statusId;
    }

    public Drawable getAvatar() {
        return avatar;
    }

    public int getOfflineModeLevel() {
        return offlineModeLevel;
    }

    public String getContactCount() {
        return contactCount;
    }

    public AccountJid getAccountJid() {
        return accountJid;
    }

    public boolean isExpand() {
        return isExpand;
    }

    public String getGroupName() {
        return groupName;
    }

    public class ViewHolder extends FlexibleViewHolder {

        final ImageView ivAvatar;
        final View avatarView;
        final TextView tvAccountName;
        final TextView tvStatus;
        final TextView tvContactCount;
        final ImageView ivStatus;
        final ImageView ivMenu;
        final ImageView offlineShadow;
        final View accountColorIndicator;

        public ViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter, true);

            itemView.setOnClickListener(this);
            //itemView.setOnCreateContextMenuListener(this);

            ivAvatar = (ImageView) view.findViewById(R.id.ivAvatar);
            avatarView = view.findViewById(R.id.avatarView);
            ivAvatar.setOnClickListener(this);
            tvAccountName = (TextView) view.findViewById(R.id.tvAccountName);
            tvStatus = (TextView) view.findViewById(R.id.tvStatus);
            tvContactCount = (TextView) view.findViewById(R.id.tvContactCount);
            ivStatus = (ImageView) view.findViewById(R.id.ivStatus);
            ivStatus.setOnClickListener(this);
            offlineShadow = (ImageView) view.findViewById(R.id.offline_shadow);
            accountColorIndicator = view.findViewById(R.id.accountColorIndicator);
            ivMenu = (ImageView) view.findViewById(R.id.ivMenu);
            ivMenu.setOnClickListener(this);
        }
    }
}
