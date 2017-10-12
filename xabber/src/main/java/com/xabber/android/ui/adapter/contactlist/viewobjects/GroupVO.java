package com.xabber.android.ui.adapter.contactlist.viewobjects;

import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.ui.adapter.contactlist.GroupConfiguration;
import com.xabber.android.ui.color.ColorManager;

/**
 * Created by valery.miller on 11.10.17.
 */

public class GroupVO extends BaseRosterItemVO {

    public static final String RECENT_CHATS_TITLE = "Recent chats";

    private String title;
    private int expandIndicatorLevel;
    private int offlineIndicatorLevel;
    private String groupName;
    private AccountJid accountJid;

    public GroupVO(int accountColorIndicator, boolean showOfflineShadow, String title,
                   int expandIndicatorLevel, int offlineIndicatorLevel, String groupName,
                   AccountJid accountJid) {

        super(accountColorIndicator, showOfflineShadow);
        this.title = title;
        this.expandIndicatorLevel = expandIndicatorLevel;
        this.offlineIndicatorLevel = offlineIndicatorLevel;
        this.groupName = groupName;
        this.accountJid = accountJid;
    }

    public static GroupVO convert(GroupConfiguration configuration) {

        String name = GroupManager.getInstance().getGroupName(configuration.getAccount(), configuration.getGroup());
        boolean showOfflineShadow = false;
        int accountColorIndicator;
        int expandIndicatorLevel;
        int offlineIndicatorLevel;

        AccountJid account = configuration.getAccount();
        if (account == null || account == GroupManager.NO_ACCOUNT)
            accountColorIndicator = ColorManager.getInstance().getAccountPainter().getDefaultMainColor();
        else accountColorIndicator = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);

        expandIndicatorLevel = configuration.isExpanded() ? 1 : 0;
        offlineIndicatorLevel = configuration.getShowOfflineMode().ordinal();

        if (!name.equals(RECENT_CHATS_TITLE))
            name = String.format("%s (%d/%d)", name, configuration.getOnline(), configuration.getTotal());

        AccountItem accountItem = AccountManager.getInstance().getAccount(configuration.getAccount());

        if (accountItem != null) {
            StatusMode statusMode = accountItem.getDisplayStatusMode();
            if (statusMode == StatusMode.unavailable || statusMode == StatusMode.connection)
                showOfflineShadow = true;
        }

        return new GroupVO(accountColorIndicator, showOfflineShadow, name, expandIndicatorLevel,
                offlineIndicatorLevel, configuration.getGroup(), configuration.getAccount());
    }

    public String getTitle() {
        return title;
    }


    public int getExpandIndicatorLevel() {
        return expandIndicatorLevel;
    }


    public int getOfflineIndicatorLevel() {
        return offlineIndicatorLevel;
    }

    public String getGroupName() {
        return groupName;
    }

    public AccountJid getAccountJid() {
        return accountJid;
    }
}
