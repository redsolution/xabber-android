package com.xabber.xmpp.avatar;

import com.xabber.android.data.extension.avatar.AvatarStorage;

import org.jxmpp.jid.EntityBareJid;


public class AvatarMetadataStore {

    boolean hasAvatarAvailable(EntityBareJid jid, String hash){
        return AvatarStorage.getInstance().read(hash) != null;
    }

    void setAvatarAvailable(EntityBareJid jid, String hash){

    }
}