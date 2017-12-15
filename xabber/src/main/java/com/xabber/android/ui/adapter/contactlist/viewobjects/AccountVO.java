package com.xabber.android.ui.adapter.contactlist.viewobjects;

import android.graphics.drawable.Drawable;

import com.brandongogetap.stickyheaders.exposed.StickyHeader;
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

/**
 * Created by valery.miller on 11.10.17.
 */

public class AccountVO extends BaseRosterItemVO implements StickyHeader {

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

    public AccountVO(int accountColorIndicator, boolean showOfflineShadow, String name, String jid,
                     String status, int statusLevel, int statusId, Drawable avatar,
                     int offlineModeLevel, String contactCount, AccountJid accountJid,
                     boolean isExpand, String groupName) {
        super(accountColorIndicator, showOfflineShadow);
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

        return new AccountVO(accountColorIndicator, showOfflineShadow, name, jid, status, statusLevel,
                statusId, avatar, offlineModeLevel, contactCount, configuration.getAccount(),
                configuration.isExpanded(), configuration.getGroup());
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

    public Drawable getAvatar() {
        return avatar;
    }

    public int getOfflineModeLevel() {
        return offlineModeLevel;
    }

    public String getContactCount() {
        return contactCount;
    }

    public int getStatusId() {
        return statusId;
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

    public String getName() {
        return name;
    }
}
