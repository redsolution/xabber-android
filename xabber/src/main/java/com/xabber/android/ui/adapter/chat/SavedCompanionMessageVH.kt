package com.xabber.android.ui.adapter.chat

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.Glide
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.database.repositories.MessageRepository
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.entity.ContactJid.ContactJidCreateException
import com.xabber.android.data.extension.avatar.AvatarManager
import com.xabber.android.data.extension.groups.GroupMember
import com.xabber.android.data.extension.groups.GroupMemberManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.roster.RosterManager
import org.jxmpp.jid.parts.Resourcepart

/**
 * Represents saved message, contained only one message from simple contact or group member
 */
class SavedCompanionMessageVH(
    val itemView: View,
    val messageListener: MessageClickListener,
    val longClickListener: MessageLongClickListener,
    val fileListener: FileListener,
    val bindListener: BindListener,
    val avatarListener: OnMessageAvatarClickListener,
    val appearance: Int,
) : NoFlexIncomingMsgVH(
    itemView,
    messageListener,
    longClickListener,
    fileListener,
    bindListener,
    avatarListener,
    appearance
) {

    override fun bind(messageRealmObject: MessageRealmObject, extraData: MessagesAdapter.MessageExtraData) {
        val innerMessage: MessageRealmObject = MessageRepository.getForwardedMessages(messageRealmObject).first()
        val groupMember: GroupMember? = GroupMemberManager.getGroupMemberById(innerMessage.groupchatUserId)
        super.bind(innerMessage, extraData)
        setupAvatar(innerMessage, extraData.isNeedTail, groupMember)
    }

    private fun setupAvatar(
        messageRealmObject: MessageRealmObject,
        needTail: Boolean = false,
        groupMember: GroupMember? = null,
    ) {

        if (!needTail) {
            avatar.visibility = View.INVISIBLE
            avatarBackground.visibility = View.INVISIBLE
            return
        }

        avatar.visibility = View.VISIBLE
        avatarBackground.visibility = View.VISIBLE

        //groupchat avatar
        if (groupMember != null) {
            val placeholder: Drawable? = try {
                val contactJid: ContactJid =
                    ContactJid.from(messageRealmObject.user.jid.toString() + "/" + groupMember.nickname)
                AvatarManager.getInstance().getOccupantAvatar(contactJid, groupMember.nickname)
            } catch (e: ContactJidCreateException) {
                AvatarManager.getInstance()
                    .generateDefaultAvatar(groupMember.nickname ?: "", groupMember.nickname ?: "")
            }
            Glide.with(itemView.context)
                .load(AvatarManager.getInstance().getGroupMemberAvatar(groupMember, messageRealmObject.account))
                .centerCrop()
                .placeholder(placeholder)
                .error(placeholder)
                .into(avatar)
            return
        } else {
            avatar.setImageDrawable(
                RosterManager.getInstance().getAbstractContact(messageRealmObject.account, messageRealmObject.user)
                    .getAvatar(true)
            )
        }
    }

}