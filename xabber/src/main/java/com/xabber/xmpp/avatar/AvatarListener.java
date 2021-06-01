package com.xabber.xmpp.avatar;

import org.jxmpp.jid.EntityBareJid;

/**
 * Listener that can notify the user about User Avatar updates.
 *
 */
public interface AvatarListener {

    void onAvatarUpdateReceived(EntityBareJid user, MetadataExtension metadata);
}