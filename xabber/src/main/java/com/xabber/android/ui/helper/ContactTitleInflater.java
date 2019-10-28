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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.xabber.android.R;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.cs.ChatStateManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;

import org.jivesoftware.smackx.chatstates.ChatState;


public class ContactTitleInflater {

    public static void updateTitle(View titleView, final Context context, AbstractContact abstractContact) {
        updateTitle(titleView, context, abstractContact, false);
    }

    public static void updateTitle(View titleView, final Context context, AbstractContact abstractContact, boolean isForVcard) {
        final TextView nameView = (TextView) titleView.findViewById(R.id.name);
        final ImageView avatarView = (ImageView) titleView.findViewById(R.id.ivAvatar);

        nameView.setText(abstractContact.getName());
        if (isForVcard){
            // if it is account, not simple user contact
            if (abstractContact.getUser().getJid().asBareJid().equals(abstractContact.getAccount().getFullJid().asBareJid())) {
                avatarView.setImageDrawable(AvatarManager.getInstance().getAccountAvatarNoDefault(abstractContact.getAccount()));
            } else {
                avatarView.setImageDrawable(abstractContact.getAvatar(false));
            }

            /*in case qrcode-avatars will be needed, probably should migrate it into avatar manager
*/
            if (avatarView.getDrawable() == null) {
                ImageView avatarQRView = (ImageView) titleView.findViewById(R.id.ivAvatarQR);
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap bitmap;
                try {
                    bitmap = barcodeEncoder.encodeBitmap("xmpp:" + abstractContact.getUser().getBareJid().toString(), BarcodeFormat.QR_CODE, 600, 600);
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
            if (abstractContact.getUser().getJid().asBareJid().equals(abstractContact.getAccount().getFullJid().asBareJid())) {
                avatarView.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(abstractContact.getAccount()));
            } else {
                avatarView.setImageDrawable(abstractContact.getAvatar());
            }
            setStatus(context, titleView, abstractContact);
        }
    }

    private static void setStatus(Context context, View titleView, AbstractContact abstractContact) {
        setStatus(context, titleView, abstractContact, false);
    }

    private static void setStatus(Context context, View titleView, AbstractContact abstractContact, boolean isForVcard) {
        final ImageView statusModeView = (ImageView) titleView.findViewById(R.id.ivStatus);
        ImageView statusModeGroupView = (ImageView) titleView.findViewById(R.id.ivStatusGroupchat);

        MessageManager messageManager = MessageManager.getInstance();
        AbstractChat chat = messageManager.getOrCreateChat(abstractContact.getAccount(), abstractContact.getUser());


        int statusLevel = abstractContact.getStatusMode().getStatusLevel();
        statusModeView.setVisibility(View.GONE);
        if (chat.isGroupchat()){
            if (isForVcard) statusModeGroupView.setVisibility(View.GONE);
            else statusModeGroupView.setVisibility(View.VISIBLE);
        }else {
            if (isContactOffline(statusLevel)) {
                statusModeView.setVisibility(View.GONE);
            } else {
                statusModeView.setVisibility(View.VISIBLE);
                statusModeView.setImageLevel(statusLevel);
            }
        }

        final TextView statusTextView = (TextView) titleView.findViewById(R.id.status_text);


        ChatState chatState = ChatStateManager.getInstance().getChatState(
                abstractContact.getAccount(), abstractContact.getUser());

        CharSequence statusText;
        if (chatState == ChatState.composing) {
            statusText = context.getString(R.string.chat_state_composing);
        } else if (chatState == ChatState.paused) {
            statusText = context.getString(R.string.chat_state_paused);
        } else {
            statusText = abstractContact.getStatusText().trim();
            if (statusText.toString().isEmpty()) {
                statusText = context.getString(abstractContact.getStatusMode().getStringID());
            }
        }
        statusTextView.setText(statusText);
    }

    private static boolean isContactOffline(int statusLevel) {
        return statusLevel == 6;
    }

}
