package com.xabber.android.ui.adapter.contactlist.viewobjects;

import android.graphics.drawable.Drawable;

import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.ui.color.ColorManager;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by valery.miller on 11.10.17.
 */

public class ContactVO extends BaseRosterItemVO {

    private String name;
    private String status;
    private int statusId;
    private int statusLevel;
    private Drawable avatar;
    private int mucIndicatorLevel;
    private UserJid userJid;
    private AccountJid accountJid;

    public ContactVO(int accountColorIndicator, boolean showOfflineShadow, String name, String status,
                     int statusId, int statusLevel, Drawable avatar, int mucIndicatorLevel,
                     UserJid userJid, AccountJid accountJid) {
        super(accountColorIndicator, showOfflineShadow);
        this.name = name;
        this.status = status;
        this.statusId = statusId;
        this.statusLevel = statusLevel;
        this.avatar = avatar;
        this.mucIndicatorLevel = mucIndicatorLevel;
        this.userJid = userJid;
        this.accountJid = accountJid;
    }

    public static ContactVO convert(AbstractContact contact) {

        boolean showOfflineShadow = false;
        int accountColorIndicator;
        Drawable avatar;
        int statusLevel;
        int mucIndicatorLevel;

        AccountItem accountItem = AccountManager.getInstance().getAccount(contact.getAccount());
        if (accountItem != null && accountItem.getState() == ConnectionState.connected) {
            showOfflineShadow = false;
        } else {
            showOfflineShadow = true;
        }

        accountColorIndicator = ColorManager.getInstance().getAccountPainter()
                .getAccountMainColor(contact.getAccount());
        avatar = contact.getAvatarForContactList();


        String name = contact.getName();

        if (MUCManager.getInstance().hasRoom(contact.getAccount(), contact.getUser())) {
            mucIndicatorLevel = 1;
        } else if (MUCManager.getInstance().isMucPrivateChat(contact.getAccount(), contact.getUser())) {
            mucIndicatorLevel = 2;
        } else {
            mucIndicatorLevel = 0;
        }

        statusLevel = contact.getStatusMode().getStatusLevel();
        String statusText = contact.getStatusText().trim();
        int statusId = contact.getStatusMode().getStringID();

        return new ContactVO(accountColorIndicator, showOfflineShadow, name, statusText, statusId,
                statusLevel, avatar, mucIndicatorLevel, contact.getUser(), contact.getAccount());
    }

    public static ArrayList<ContactVO> convert(Collection<AbstractContact> contacts) {
        ArrayList<ContactVO> items = new ArrayList<>();
        for (AbstractContact contact : contacts) {
            items.add(convert(contact));
        }
        return items;
    }

    public String getName() {
        return name;
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

    public int getMucIndicatorLevel() {
        return mucIndicatorLevel;
    }

    public int getStatusId() {
        return statusId;
    }

    public UserJid getUserJid() {
        return userJid;
    }

    public AccountJid getAccountJid() {
        return accountJid;
    }
}
