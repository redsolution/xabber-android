package com.xabber.android.ui.adapter.chat

import android.graphics.drawable.Drawable
import android.view.View
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bumptech.glide.Glide
import com.xabber.android.data.database.realmobjects.GroupMemberRealmObject
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.database.repositories.MessageRepository
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.entity.ContactJid.ContactJidCreateException
import com.xabber.android.data.extension.avatar.AvatarManager
import com.xabber.android.data.extension.groups.GroupMemberManager
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.color.ColorManager

/**
 * Represents saved message, contained only one message from simple contact or group member
 */
class SavedCompanionMessageVH(
    val itemView: View,
    val messageListener: MessageClickListener,
    private val longClickListener: MessageLongClickListener,
    val fileListener: FileListener,
    bindListener: BindListener?,
    private val avatarListener: OnMessageAvatarClickListener,
    val appearance: Int,
) : IncomingMessageVH(
    itemView,
    messageListener,
    longClickListener,
    fileListener,
    bindListener,
    avatarListener,
    appearance
) {

    override fun bind(messageRealmObject: MessageRealmObject, extraData: MessageExtraData) {
        val innerMessage: MessageRealmObject = MessageRepository.getForwardedMessages(messageRealmObject).first()
        val groupMember: GroupMemberRealmObject? = innerMessage.groupchatUserId?.let {
            GroupMemberManager.getGroupMemberById(
                innerMessage.account, innerMessage.user, it
            )
        }
        super.bind(innerMessage, extraData)
        setupAvatar(innerMessage, extraData.isNeedTail, groupMember)
        setupName(
            innerMessage.account,
            innerMessage.originalFrom?.let { ContactJid.from(it) } ?: innerMessage.user,
            extraData.isNeedName,
            groupMember
        )
        setupTime(extraData, messageRealmObject)
    }

    private fun setupAvatar(
        messageRealmObject: MessageRealmObject,
        needTail: Boolean = false,
        groupMember: GroupMemberRealmObject? = null,
    ) {
        if (!needTail) {
            avatar.visibility = View.INVISIBLE
        } else {
            avatar.visibility = View.VISIBLE
            //groupchat avatar
            if (groupMember != null) {
                val placeholder: Drawable? = try {
                    val contactJid: ContactJid =
                        ContactJid.from(messageRealmObject.user.jid.toString() + "/" + groupMember.nickname)
                    AvatarManager.getInstance().getOccupantAvatar(contactJid, groupMember.nickname)
                } catch (e: ContactJidCreateException) {
                    AvatarManager.getInstance().generateDefaultAvatar(
                        groupMember.nickname ?: "",
                        groupMember.nickname ?: ""
                    )
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
                        RosterManager.getInstance()
                            .getAbstractContact(messageRealmObject.account, ContactJid.from(senderJid).bareUserJid)
                            .getAvatar(true)
                    )
                }
            }
        }
    }

    private fun setupName(
        accountJid: AccountJid,
        companionJid: ContactJid,
        needName: Boolean = false,
        groupMember: GroupMemberRealmObject? = null,
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