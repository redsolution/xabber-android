package com.xabber.android.ui.adapter.chat

import android.graphics.drawable.Drawable
import android.view.View
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bumptech.glide.Glide
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.database.repositories.MessageRepository
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.entity.ContactJid.ContactJidCreateException
import com.xabber.android.data.extension.avatar.AvatarManager
import com.xabber.android.data.extension.groups.GroupMember
import com.xabber.android.data.extension.groups.GroupMemberManager
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.color.ColorManager

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
        setupName(
            innerMessage.account,
            innerMessage.originalFrom?.let { ContactJid.from(it) } ?: innerMessage.user,
            extraData.isNeedName,
            groupMember
        )
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
            messageRealmObject.originalFrom?.let { senderJid ->
                avatar.setImageDrawable(
                    RosterManager.getInstance().getAbstractContact(
                        messageRealmObject.account, ContactJid.from(senderJid).bareUserJid
                    ).getAvatar(true)
                )
            }
        }
    }

    private fun setupName(
        accountJid: AccountJid,
        companionJid: ContactJid,
        needName: Boolean = false,
        groupMember: GroupMember? = null,
    ) {
        if (needName) {
            if (groupMember != null && !groupMember.isMe) {
                messageHeader.text = groupMember.nickname
                messageHeader.setTextColor(
                    ColorManager.changeColor(ColorGenerator.MATERIAL.getColor(groupMember.nickname), 0.8f)
                )
            } else {
                messageHeader.text = companionJid.bareJid.toString()
                RosterManager.getInstance().getBestContact(accountJid, companionJid)?.let {
                    messageHeader.text = it.name
                    messageHeader.setTextColor(
                        ColorManager.changeColor(ColorGenerator.MATERIAL.getColor(it), 0.8f)
                    )
                }
            }
            messageHeader.visibility = View.VISIBLE
        } else messageHeader.visibility = View.GONE
    }

}