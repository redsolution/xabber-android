/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.groupchat.GroupChat;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.StatusBadgeSetupHelper;
import com.xabber.android.ui.activity.AccountActivity;
import com.xabber.android.ui.color.ColorManager;


public class ContactTitleInflater {

    public static void updateTitle(View titleView, final Context context, AbstractContact abstractContact) {
        updateTitle(titleView, context, abstractContact, false);
    }

    public static void updateTitleWithNameColorizing(){

    }

    public static void updateTitle(View titleView, final Context context, AbstractContact abstractContact, boolean isForVcard) {
        updateTitle(titleView, context, abstractContact, isForVcard, true);
    }

    public static void updateTitle(View titleView, final Context context, AbstractContact abstractContact, boolean isForVcard, boolean qrAvatarNeeded) {
        final TextView nameView = (TextView) titleView.findViewById(R.id.name);
        final TextView addressTextView = (TextView) titleView.findViewById(R.id.address_text);
        final ImageView avatarView = (ImageView) titleView.findViewById(R.id.ivAvatar);

        nameView.setText(abstractContact.getName());
        if (isForVcard){
            nameView.setVisibility(
                    abstractContact.getContactJid().getBareJid().toString().equals(abstractContact.getName()) ?
                            View.GONE : View.VISIBLE
            );
            if (addressTextView != null) {
                addressTextView.setPadding(
                        addressTextView.getPaddingLeft(),
                        nameView.getVisibility() == View.VISIBLE ? addressTextView.getPaddingBottom() / 3 : addressTextView.getPaddingBottom(),
                        addressTextView.getPaddingRight(),
                        addressTextView.getPaddingBottom()
                );
                addressTextView.setSelected(true);
            }
            // if it is account, not simple user contact
            if (abstractContact.getContactJid().getJid().asBareJid().equals(abstractContact.getAccount().getFullJid().asBareJid())) {
                avatarView.setImageDrawable(AvatarManager.getInstance().getAccountAvatarNoDefault(abstractContact.getAccount()));
            } else {
                avatarView.setImageDrawable(abstractContact.getAvatar(false));
            }

            /*in case qrcode-avatars will be needed, probably should migrate it into avatar manager
*/
            if (avatarView.getDrawable() == null && qrAvatarNeeded) {
                ImageView avatarQRView = (ImageView) titleView.findViewById(R.id.ivAvatarQR);
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap bitmap;
                try {
                    bitmap = barcodeEncoder.encodeBitmap("xmpp:" + abstractContact.getContactJid().getBareJid().toString(), BarcodeFormat.QR_CODE, 600, 600);
                    Bitmap cropped = Bitmap.createBitmap(bitmap, 50,50,500,500);
                    if (cropped != null) {
                        avatarQRView.setImageBitmap(cropped);
                        //avatarQRView.setVisibility(View.VISIBLE);
                    }
                } catch (WriterException e) {
                    e.printStackTrace();
                }
            }
            setStatus(context, titleView, abstractContact, true);
        }
        else {
            // if it is account, not simple user contact
            if (abstractContact.getContactJid().getJid().asBareJid().equals(abstractContact.getAccount().getFullJid().asBareJid())) {
                avatarView.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(abstractContact.getAccount()));
            } else {
                avatarView.setImageDrawable(abstractContact.getAvatar());
            }
            setStatus(context, titleView, abstractContact);
        }
        /* Colorize name accordingly to color theme */
        if (titleView.getParent().getClass().toString() == AccountActivity.class.toString()){
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                nameView.setTextColor(Color.BLACK);
            else nameView.setTextColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(abstractContact.getAccount()));
        }

    }

    private static void setStatus(Context context, View titleView, AbstractContact abstractContact) {
        setStatus(context, titleView, abstractContact, false);
    }

    private static void setStatus(Context context, View titleView, AbstractContact abstractContact, boolean isForVcard) {
        final ImageView statusModeView = (ImageView) titleView.findViewById(R.id.ivStatus);
        ImageView statusModeGroupView = (ImageView) titleView.findViewById(R.id.ivStatusGroupchat);

        boolean isServer = false;
        boolean isGroupchat = false;
        AbstractChat chat = ChatManager.getInstance()
                .getChat(abstractContact.getAccount(), abstractContact.getContactJid());
        if (chat != null) {
            isServer = abstractContact.getContactJid().getJid().isDomainBareJid();
            isGroupchat = chat instanceof GroupChat;
        }
        int statusLevel = abstractContact.getStatusMode().getStatusLevel();
        statusModeView.setVisibility(View.GONE);
        if (isServer) {
            statusModeGroupView.setImageResource(R.drawable.ic_status_combined);
            statusModeGroupView.setImageLevel(StatusBadgeSetupHelper.INSTANCE.getStatusLevelForAccount(abstractContact));
            if (isForVcard) {
                statusModeGroupView.setVisibility(View.GONE);
                statusModeView.setVisibility(View.GONE);
            } else statusModeGroupView.setVisibility(View.VISIBLE);
        } else if (isGroupchat) {
            if (isForVcard) {
                statusModeGroupView.setVisibility(View.GONE);
                statusModeView.setVisibility(View.GONE);
            } else statusModeGroupView.setVisibility(View.VISIBLE);
        } else {
            if (isContactOffline(statusLevel) || isForVcard) {
                statusModeView.setVisibility(View.GONE);
            } else {
                statusModeView.setVisibility(View.VISIBLE);
                statusModeView.setImageLevel(statusLevel);
            }
        }

        final TextView statusTextView = (TextView) titleView.findViewById(R.id.status_text);

        CharSequence statusText;
        if (isServer) statusText = "Server";
        else {
            statusText = ChatStateManager.getInstance().getFullChatStateString(
                    abstractContact.getAccount(), abstractContact.getContactJid());
            if (statusText == null) {
                statusText = abstractContact.getStatusText().trim();
                if (statusText.toString().isEmpty()) {
                    statusText = context.getString(abstractContact.getStatusMode().getStringID());
                }
            }
        }
        statusTextView.setText(statusText);
        /* Colorize status accordingly to color theme */
        if (titleView.getParent().getClass().toString() == AccountActivity.class.toString()){
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                statusTextView.setTextColor(Color.BLACK);
            else statusTextView.setTextColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(abstractContact.getAccount()));
        }
    }

    private static boolean isContactOffline(int statusLevel) {
        return statusLevel == 6;
    }

}
