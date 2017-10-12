package com.xabber.android.ui.widget.bottomnavigation;

import android.graphics.drawable.Drawable;

import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.avatar.AvatarManager;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by valery.miller on 12.10.17.
 */

public class AccountShortcutVO {

    private Drawable avatar;
    private int statusLevel;
    private AccountJid accountJid;

    public AccountShortcutVO(Drawable avatar, int statusLevel, AccountJid accountJid) {
        this.avatar = avatar;
        this.statusLevel = statusLevel;
        this.accountJid = accountJid;
    }

    public static AccountShortcutVO convert(AccountJid accountJid) {
        Drawable avatar = AvatarManager.getInstance().getAccountAvatar(accountJid);
        int statusLevel = 0;

        AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
        if (accountItem != null) {
            statusLevel = accountItem.getDisplayStatusMode().getStatusLevel();
        }
        return new AccountShortcutVO(avatar, statusLevel, accountJid);
    }

    public static ArrayList<AccountShortcutVO> convert(Collection<AccountJid> accounts) {
        ArrayList<AccountShortcutVO> items = new ArrayList<>();
        for (AccountJid account : accounts) {
            items.add(convert(account));
        }
        return items;
    }

    public Drawable getAvatar() {
        return avatar;
    }

    public int getStatusLevel() {
        return statusLevel;
    }

    public AccountJid getAccountJid() {
        return accountJid;
    }
}
