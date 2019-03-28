package com.xabber.android.presentation.ui.contactlist.viewobjects;

/**
 * Created by valery.miller on 05.02.18.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
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
import com.xabber.android.data.notification.custom_notification.CustomNotifyPrefsManager;
import com.xabber.android.data.notification.custom_notification.Key;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.ShowOfflineMode;
import com.xabber.android.ui.adapter.contactlist.AccountConfiguration;
import com.xabber.android.ui.color.ColorManager;

import java.util.List;
import java.util.UUID;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractHeaderItem;
import eu.davidea.viewholders.ExpandableViewHolder;

public class AccountVO extends AbstractHeaderItem<AccountVO.ViewHolder> {

    private String id;

    private int accountColorIndicator;
    private int accountColorIndicatorBack;
    private boolean showOfflineShadow;

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
    private boolean isCustomNotification;

    protected final AccountClickListener listener;

    public interface AccountClickListener {
        void onAccountAvatarClick(int adapterPosition);
        void onAccountMenuClick(int adapterPosition, View view);
    }

    public AccountVO(int accountColorIndicator, int accountColorIndicatorBack, boolean showOfflineShadow,
                     String name, String jid, String status, int statusLevel, int statusId,
                     Drawable avatar, int offlineModeLevel, String contactCount, AccountJid accountJid,
                     boolean isExpand, String groupName, boolean isCustomNotification,
                     AccountClickListener listener) {
        this.id = UUID.randomUUID().toString();
        this.accountColorIndicator = accountColorIndicator;
        this.accountColorIndicatorBack = accountColorIndicatorBack;
        this.showOfflineShadow = showOfflineShadow;
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
        this.isCustomNotification = isCustomNotification;
        this.listener = listener;
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
        return R.layout.item_account_in_contact_list;
    }

    @Override
    public AccountVO.ViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new AccountVO.ViewHolder(view, adapter, listener);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, ViewHolder viewHolder, int position, List<Object> payloads) {
        Context context = viewHolder.itemView.getContext();

        /** bind OFFLINE SHADOW */
        if (isShowOfflineShadow())
            viewHolder.offlineShadow.setVisibility(View.VISIBLE);
        else viewHolder.offlineShadow.setVisibility(View.GONE);

        /** set up ACCOUNT COLOR indicator */
        viewHolder.accountColorIndicator.setBackgroundColor(getAccountColorIndicator());
        viewHolder.accountColorIndicatorBack.setBackgroundColor(getAccountColorIndicatorBack());

        /** bind ACCOUNT BACKGROUND color */
        final int[] accountGroupColors = context.getResources().getIntArray(
                getThemeResource(context, R.attr.contact_list_account_group_background));
        final int level = AccountManager.getInstance().getColorLevel(getAccountJid());
        viewHolder.backgroundView.setBackgroundColor(accountGroupColors[level]);

        /** bind ACCOUNT NAME */
        viewHolder.tvAccountName.setText(getName());

        /** bind CONTACT COUNT */
        viewHolder.tvContactCount.setText(getContactCount());

        /** bind OFFLINE MODE */
        Drawable offlineModeImage = context.getResources().getDrawable(R.drawable.ic_show_offline_small);
        offlineModeImage.setLevel(getOfflineModeLevel());
        viewHolder.tvContactCount.setCompoundDrawablesWithIntrinsicBounds(offlineModeImage, null, null, null);

        /** bind STATUS TEXT */
        String statusText = getStatus();
        if (statusText.isEmpty()) statusText = context.getString(getStatusId());
        viewHolder.tvStatus.setText(statusText);

        /** bind AVATAR */
        if (SettingsManager.contactsShowAvatars()) {
            viewHolder.ivAvatar.setVisibility(View.VISIBLE);
            viewHolder.ivStatus.setVisibility(View.VISIBLE);
            viewHolder.ivAvatar.setImageDrawable(getAvatar());
            viewHolder.ivOnlyStatus.setVisibility(View.GONE);
        } else {
            viewHolder.ivAvatar.setVisibility(View.GONE);
            viewHolder.ivStatus.setVisibility(View.GONE);
            viewHolder.ivOnlyStatus.setVisibility(View.VISIBLE);
        }

        /** bind STATUS image */
        viewHolder.ivStatus.setImageLevel(getStatusLevel());
        viewHolder.ivOnlyStatus.setImageLevel(getStatusLevel());

        /** set up CUSTOM NOTIFICATION */
        viewHolder.tvAccountName.setCompoundDrawablesWithIntrinsicBounds(null, null,
                isCustomNotification() ? context.getResources().getDrawable(R.drawable.ic_notif_custom)
                        : null, null);
    }

    private int getThemeResource(Context context, int themeResourceId) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[] {themeResourceId});
        final int accountGroupColorsResourceId = a.getResourceId(0, 0);
        a.recycle();
        return accountGroupColorsResourceId;
    }

    public static AccountVO convert(AccountConfiguration configuration, AccountClickListener listener) {
        String jid;
        String name;
        String status;
        int statusLevel;
        int statusId;
        Drawable avatar;
        int offlineModeLevel;
        boolean showOfflineShadow = false;
        int accountColorIndicator;
        int accountColorIndicatorBack;
        String contactCount;

        AccountJid account = configuration.getAccount();

        accountColorIndicator = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);
        accountColorIndicatorBack = ColorManager.getInstance().getAccountPainter().getAccountIndicatorBackColor(account);

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

        // custom notification
        boolean isCustomNotification = CustomNotifyPrefsManager.getInstance().
                isPrefsExist(Key.createKey(account));

        return new AccountVO(accountColorIndicator, accountColorIndicatorBack, showOfflineShadow,
                name, jid, status, statusLevel,
                statusId, avatar, offlineModeLevel, contactCount, configuration.getAccount(),
                configuration.isExpanded(), configuration.getGroup(), isCustomNotification, listener);
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

    public boolean isCustomNotification() {
        return isCustomNotification;
    }

    public int getAccountColorIndicator() {
        return accountColorIndicator;
    }

    public int getAccountColorIndicatorBack() {
        return accountColorIndicatorBack;
    }


    public boolean isShowOfflineShadow() {
        return showOfflineShadow;
    }

    public class ViewHolder extends ExpandableViewHolder {

        final ImageView ivAvatar;
        final View avatarView;
        final TextView tvAccountName;
        final TextView tvStatus;
        final ImageView ivOnlyStatus;
        final TextView tvContactCount;
        final ImageView ivStatus;
        final ImageView ivMenu;
        final ImageView offlineShadow;
        final View accountColorIndicator;
        final View accountColorIndicatorBack;
        final View backgroundView;
        final View bottomView;

        final AccountClickListener listener;

        public ViewHolder(View view, FlexibleAdapter adapter, AccountClickListener listener) {
            super(view, adapter, true);

            itemView.setOnClickListener(this);
            this.listener = listener;

            ivAvatar = (ImageView) view.findViewById(R.id.ivAvatar);
            avatarView = view.findViewById(R.id.avatarView);
            ivAvatar.setOnClickListener(this);
            tvAccountName = (TextView) view.findViewById(R.id.tvAccountName);
            tvStatus = (TextView) view.findViewById(R.id.tvStatus);
            ivOnlyStatus = (ImageView) view.findViewById(R.id.ivOnlyStatus);
            tvContactCount = (TextView) view.findViewById(R.id.tvContactCount);
            ivStatus = (ImageView) view.findViewById(R.id.ivStatus);
            ivStatus.setOnClickListener(this);
            offlineShadow = (ImageView) view.findViewById(R.id.offline_shadow);
            accountColorIndicator = view.findViewById(R.id.accountColorIndicator);
            accountColorIndicatorBack = view.findViewById(R.id.accountColorIndicatorBack);
            ivMenu = (ImageView) view.findViewById(R.id.ivMenu);
            ivMenu.setOnClickListener(this);
            backgroundView = view.findViewById(R.id.backgroundView);
            bottomView = view.findViewById(R.id.bottomView);
        }

        @Override
        public void onClick(View view) {
            int adapterPosition = getAdapterPosition();

            if (view.getId() == R.id.ivAvatar) {
                listener.onAccountAvatarClick(adapterPosition);
            } else if (view.getId() == R.id.ivMenu) {
                listener.onAccountMenuClick(adapterPosition, view);
            } else {
                super.onClick(view);
            }
        }
    }
}
