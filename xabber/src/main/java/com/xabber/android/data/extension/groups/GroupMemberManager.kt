package com.xabber.android.data.extension.groups

import android.widget.Toast
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.BaseIqResultUiListener
import com.xabber.android.data.OnLoadListener
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.database.repositories.GroupMemberRepository
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.avatar.AvatarManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.message.chat.RegularChat
import com.xabber.android.data.roster.PresenceManager.acceptSubscription
import com.xabber.android.data.roster.PresenceManager.addAutoAcceptSubscription
import com.xabber.android.data.roster.PresenceManager.requestSubscription
import com.xabber.android.ui.OnGroupMemberRightsListener
import com.xabber.android.ui.OnGroupSelectorListToolbarActionResultListener
import com.xabber.android.ui.OnGroupchatRequestListener
import com.xabber.android.ui.activity.ChatActivity
import com.xabber.android.ui.forEachOnUi
import com.xabber.xmpp.avatar.DataExtension
import com.xabber.xmpp.avatar.MetadataExtension
import com.xabber.xmpp.avatar.MetadataInfo
import com.xabber.xmpp.avatar.UserAvatarManager
import com.xabber.xmpp.groups.GroupMemberExtensionElement
import com.xabber.xmpp.groups.block.BlockGroupMemberIQ
import com.xabber.xmpp.groups.block.KickGroupMemberIQ
import com.xabber.xmpp.groups.block.blocklist.GroupchatBlocklistItemElement
import com.xabber.xmpp.groups.block.blocklist.GroupchatBlocklistQueryIQ
import com.xabber.xmpp.groups.block.blocklist.GroupchatBlocklistResultIQ
import com.xabber.xmpp.groups.block.blocklist.GroupchatBlocklistUnblockIQ
import com.xabber.xmpp.groups.create.CreateGroupchatIQ.ResultIq
import com.xabber.xmpp.groups.create.CreatePtpGroupIQ
import com.xabber.xmpp.groups.members.ChangeGroupchatMemberPreferencesIQ
import com.xabber.xmpp.groups.members.GroupchatMembersQueryIQ
import com.xabber.xmpp.groups.members.GroupchatMembersResultIQ
import com.xabber.xmpp.groups.rights.GroupRequestMemberRightsChangeIQ
import com.xabber.xmpp.groups.rights.GroupchatMemberRightsQueryIQ
import com.xabber.xmpp.groups.rights.GroupchatMemberRightsReplyIQ
import org.jivesoftware.smack.ExceptionCallback
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.XMPPException.XMPPErrorException
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.packet.XMPPError
import org.jivesoftware.smackx.pubsub.PayloadItem
import org.jivesoftware.smackx.pubsub.PublishItem
import org.jivesoftware.smackx.pubsub.packet.PubSub
import org.jivesoftware.smackx.xdata.packet.DataForm
import org.jxmpp.jid.BareJid
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList

object GroupMemberManager : OnLoadListener {

    private val members: MutableMap<String, GroupMember?> = HashMap()

    override fun onLoad() {
        GroupMemberRepository.getAllGroupMembersFromRealm().map { members[it.id] = it }
    }

    fun removeMemberAvatar(group: GroupChat, memberId: String) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                AccountManager.getInstance().getAccount(group.account)?.connection?.createStanzaCollectorAndSend(
                    PubSub.createPubsubPacket(
                        group.contactJid.bareJid,
                        IQ.Type.set,
                        PublishItem(
                            UserAvatarManager.METADATA_NAMESPACE + "#" + memberId,
                            PayloadItem(null, MetadataExtension(null))
                        ),
                        null
                    )
                )?.nextResultOrThrow<Stanza>(45000)
            } catch (e: Exception) {
                LogManager.exception(this.javaClass.simpleName, e)
            }
        }
    }

    fun publishMemberAvatar(
        group: GroupChat,
        memberId: String,
        data: ByteArray,
        height: Int,
        width: Int,
        type: UserAvatarManager.ImageType,
    ) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                val avatarHash = AvatarManager.getAvatarHash(data)

                AccountManager.getInstance().getAccount(group.account)?.connection?.createStanzaCollectorAndSend(
                    PubSub.createPubsubPacket(
                        group.contactJid.bareJid, IQ.Type.set,
                        PublishItem(
                            UserAvatarManager.DATA_NAMESPACE + "#" + memberId,
                            PayloadItem(avatarHash, DataExtension(data))
                        ),
                        null
                    )
                )?.nextResultOrThrow<Stanza>(60000)

                val metadataInfo = MetadataInfo(avatarHash, null, data.size.toLong(), type.value, height, width)
                AccountManager.getInstance().getAccount(group.account)?.connection?.createStanzaCollectorAndSend(
                    PubSub.createPubsubPacket(
                        group.contactJid.bareJid,
                        IQ.Type.set,
                        PublishItem(
                            UserAvatarManager.METADATA_NAMESPACE + "#" + memberId,
                            PayloadItem(avatarHash, MetadataExtension(listOf(metadataInfo), null))
                        ),
                        null
                    )
                )?.nextResultOrThrow<Stanza>(45000)
            } catch (e: Exception) {
                LogManager.exception(this.javaClass.simpleName, e)
            }
        }
    }

    fun getGroupMemberById(id: String?): GroupMember? = id?.let { members[it] }

    fun getMe(groupJid: ContactJid) = getGroupMembers(groupJid).firstOrNull { member -> member?.isMe ?: false }

    fun getGroupMembers(groupJid: ContactJid) = members.values.filter { it?.groupJid == groupJid.toString() }

    fun removeGroupMember(id: String) {
        members.remove(id)
        GroupMemberRepository.removeGroupMemberById(id)
    }

    fun saveOrUpdateMemberFromMessage(user: GroupMemberExtensionElement, groupJid: BareJid?): GroupMember? {
        getGroupMemberFromGroupMemberExtensionElement(user, groupJid).also { newMember ->
            if (members.contains(newMember.id)) {
                members[newMember.id]?.apply {
                    newMember.jid?.let { jid = it }
                    newMember.nickname?.let { nickname = it }
                    newMember.role?.let { role = it }
                    newMember.badge?.let { badge = it }
                    newMember.avatarHash?.let { avatarHash = it }
                    newMember.avatarUrl?.let { avatarUrl = it }
                    newMember.lastPresent?.let { lastPresent = it }
                }
            } else members[newMember.id] = newMember
            GroupMemberRepository.saveOrUpdateGroupMember(newMember)
            return members[newMember.id]
        }
    }

    fun kickMember(groupMember: GroupMember, groupChat: GroupChat, listener: BaseIqResultUiListener) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                groupChat.fullJidIfPossible?.let { fullJid ->
                    if (groupMember.jid != null && groupMember.jid?.isNotEmpty() == true) {
                        AccountManager.getInstance().getAccount(groupChat.account)?.connection
                            ?.sendIqWithResponseCallback(
                                KickGroupMemberIQ(JidCreate.bareFrom(groupMember.jid), fullJid),
                                listener,
                                listener
                            )
                    } else {
                        AccountManager.getInstance().getAccount(groupChat.account)?.connection
                            ?.sendIqWithResponseCallback(
                                KickGroupMemberIQ(groupMember.id, fullJid),
                                listener,
                                listener
                            )
                    }
                }
            } catch (e: Exception) {
                LogManager.exception(this.javaClass.simpleName, e)
                listener.onOtherError(e)
            }
        }
    }

    fun kickAndBlockMember(groupMember: GroupMember, groupChat: GroupChat, listener: BaseIqResultUiListener) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                groupChat.fullJidIfPossible?.let { fullJid ->
                    if (groupMember.jid != null && groupMember.jid?.isNotEmpty() == true) {
                        val memberJid: Jid = JidCreate.bareFrom(groupMember.jid)
                        AccountManager.getInstance().getAccount(groupChat.account)?.connection
                            ?.sendIqWithResponseCallback(BlockGroupMemberIQ(fullJid, memberJid), listener, listener)
                    } else {
                        AccountManager.getInstance().getAccount(groupChat.account)?.connection
                            ?.sendIqWithResponseCallback(
                                BlockGroupMemberIQ(fullJid, groupMember.id),
                                listener,
                                listener
                            )
                    }
                }
            } catch (e: Exception) {
                LogManager.exception(this.javaClass.simpleName, e)
                listener.onOtherError(e)
            }
        }
    }

    fun requestGroupchatBlocklistList(
        account: AccountJid, groupchatJid: ContactJid, listener: StanzaListener,
        exceptionCallback: ExceptionCallback?
    ) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            (ChatManager.getInstance().getChat(account, groupchatJid) as? GroupChat)?.let { chat ->
                try {
                    AccountManager.getInstance().getAccount(account)?.connection?.sendIqWithResponseCallback(
                        GroupchatBlocklistQueryIQ(chat as GroupChat?),
                        { packet: Stanza ->
                            if (packet is GroupchatBlocklistResultIQ) {
                                if (groupchatJid.bareJid.equals(packet.getFrom().asBareJid())
                                    && account.bareJid.equals(packet.getTo().asBareJid())
                                ) {
                                    chat.listOfBlockedElements = packet.blockedItems ?: ArrayList()
                                }
                            }
                            listener.processStanza(packet)
                        },
                        exceptionCallback
                    )
                } catch (e: Exception) {
                    LogManager.exception(this.javaClass.simpleName, e)
                }
            }
        }
    }

    fun unblockGroupchatBlockedElement(
        account: AccountJid,
        groupchatJid: ContactJid,
        blockedElement: GroupchatBlocklistItemElement
    ) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                (ChatManager.getInstance().getChat(account, groupchatJid) as? GroupChat)?.let { groupChat ->
                    AccountManager.getInstance().getAccount(account)?.connection?.sendIqWithResponseCallback(
                        GroupchatBlocklistUnblockIQ(groupChat, blockedElement),
                        { packet: Stanza? ->
                            (packet as? IQ)?.let { iq ->
                                if (IQ.Type.result == iq.type) {
                                    groupChat.listOfBlockedElements.remove(blockedElement)
                                    Application.getInstance()
                                        .getUIListeners(OnGroupSelectorListToolbarActionResultListener::class.java)
                                        .forEachOnUi { listener ->
                                            listener.onActionSuccess(
                                                account,
                                                groupchatJid,
                                                listOf(blockedElement.blockedItem)
                                            )
                                        }
                                } else {
                                    Application.getInstance()
                                        .getUIListeners(OnGroupSelectorListToolbarActionResultListener::class.java)
                                        .forEachOnUi { listener ->
                                            listener.onActionFailure(
                                                account,
                                                groupchatJid,
                                                listOf(blockedElement.blockedItem)
                                            )
                                        }
                                }
                            }
                        },
                        {
                            Application.getInstance()
                                .getUIListeners(OnGroupSelectorListToolbarActionResultListener::class.java)
                                .forEachOnUi {
                                    it.onActionFailure(account, groupchatJid, listOf(blockedElement.blockedItem))
                                }
                        }
                    )
                }

            } catch (e: Exception) {
                LogManager.exception(this.javaClass.simpleName, e)
            }
        }
    }

    fun unblockGroupchatBlockedElements(
        account: AccountJid,
        groupchatJid: ContactJid,
        blockedElements: List<GroupchatBlocklistItemElement>,
    ) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            val failedUnblockRequests = ArrayList<String>()
            val successfulUnblockRequests = ArrayList<String>()
            val unfinishedRequestCount = AtomicInteger(blockedElements.size)
            val groupChat = (ChatManager.getInstance().getChat(account, groupchatJid) as? RegularChat)?.let {
                ChatManager.getInstance().removeChat(it)
                ChatManager.getInstance().createGroupChat(account, groupchatJid)
            } ?: ChatManager.getInstance().createGroupChat(account, groupchatJid)
            for (blockedElement in blockedElements) {
                try {
                    AccountManager.getInstance().getAccount(account)?.connection?.sendIqWithResponseCallback(
                        GroupchatBlocklistUnblockIQ(groupChat, blockedElement),
                        { packet: Stanza? ->
                            (packet as? IQ).let {
                                groupChat.listOfBlockedElements?.remove(blockedElement)
                                successfulUnblockRequests.add(blockedElement.blockedItem)
                                unfinishedRequestCount.getAndDecrement()
                                if (unfinishedRequestCount.get() == 0) {
                                    Application.getInstance()
                                        .getUIListeners(OnGroupSelectorListToolbarActionResultListener::class.java)
                                        .forEachOnUi {
                                            when {
                                                failedUnblockRequests.size == 0 -> {
                                                    it.onActionSuccess(account, groupchatJid, successfulUnblockRequests)
                                                }
                                                successfulUnblockRequests.size > 0 -> {
                                                    it.onPartialSuccess(
                                                        account,
                                                        groupchatJid,
                                                        successfulUnblockRequests,
                                                        failedUnblockRequests
                                                    )
                                                }
                                                else -> {
                                                    it.onActionFailure(account, groupchatJid, failedUnblockRequests)
                                                }
                                            }
                                        }
                                }
                            }
                        },
                        {
                            failedUnblockRequests.add(blockedElement.blockedItem)
                            unfinishedRequestCount.getAndDecrement()
                            if (unfinishedRequestCount.get() == 0) {
                                Application.getInstance()
                                    .getUIListeners(OnGroupSelectorListToolbarActionResultListener::class.java)
                                    .forEachOnUi { listener ->
                                        if (successfulUnblockRequests.size > 0) {
                                            listener.onPartialSuccess(
                                                account,
                                                groupchatJid,
                                                successfulUnblockRequests,
                                                failedUnblockRequests
                                            )
                                        } else {
                                            listener.onActionFailure(account, groupchatJid, failedUnblockRequests)
                                        }
                                    }
                            }
                        }
                    )
                } catch (e: Exception) {
                    LogManager.exception(this.javaClass.simpleName, e)
                    failedUnblockRequests.add(blockedElement.blockedItem)
                    unfinishedRequestCount.getAndDecrement()
                }
            }
        }
    }

    fun createChatWithIncognitoMember(groupChat: GroupChat, groupMember: GroupMember) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                AccountManager.getInstance().getAccount(groupChat.account)?.connection?.sendIqWithResponseCallback(
                    CreatePtpGroupIQ(groupChat, groupMember.id),
                    { packet: Stanza ->
                        if (packet is ResultIq && packet.type == IQ.Type.result) {
                            try {
                                val contactJid = ContactJid.from(packet.jid)
                                val account = AccountJid.from(packet.getTo().toString())
                                addAutoAcceptSubscription(account, contactJid)
                                acceptSubscription(account, contactJid, true)
                                requestSubscription(account, contactJid)
                                val context = Application.getInstance().applicationContext
                                context.startActivity(
                                    ChatActivity.createSendIntent(
                                        context,
                                        groupChat.account,
                                        contactJid,
                                        null
                                    )
                                )
                            } catch (e: Exception) {
                                LogManager.exception(this.javaClass.simpleName, e)
                            }
                        }
                    },
                    { exception: Exception ->
                        LogManager.exception(this.javaClass.simpleName, exception)
                    }
                )
            } catch (e: Exception) {
                LogManager.exception(this.javaClass.simpleName, e)
            }
        }
    }

    fun sendSetMemberBadgeIqRequest(groupChat: GroupChat, groupMember: GroupMember, badge: String?) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                AccountManager.getInstance().getAccount(groupChat.account)?.connection?.sendIqWithResponseCallback(
                    ChangeGroupchatMemberPreferencesIQ(groupChat, groupMember.id, badge, null),
                    { packet: Stanza? ->
                        if (packet is IQ && packet.type == IQ.Type.result) {
                            groupMember.badge = badge
                            GroupMemberRepository.saveOrUpdateGroupMember(groupMember)
                            Application.getInstance().getUIListeners(OnGroupchatRequestListener::class.java)
                                .forEachOnUi {
                                    it.onGroupchatMemberUpdated(groupChat.account, groupChat.contactJid, groupMember.id)
                                }

                        }
                    },
                    { exception: Exception? ->
                        LogManager.exception(this.javaClass.simpleName, exception)
                        if (exception is XMPPErrorException
                            && exception.xmppError.condition == XMPPError.Condition.not_allowed
                        ) Application.getInstance().runOnUiThread {
                            Toast.makeText(
                                Application.getInstance().applicationContext,
                                Application.getInstance().applicationContext.getString(R.string.groupchat_you_have_no_permissions_to_do_it),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            } catch (e: Exception) {
                LogManager.exception(this.javaClass.simpleName, e)
            }
        }
    }

    fun sendSetMemberNicknameIqRequest(groupChat: GroupChat, groupMember: GroupMember, nickname: String?) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                AccountManager.getInstance().getAccount(groupChat.account)?.connection?.sendIqWithResponseCallback(
                    ChangeGroupchatMemberPreferencesIQ(groupChat, groupMember.id, null, nickname),
                    { packet: Stanza? ->
                        if (packet is IQ && packet.type == IQ.Type.result) {
                            groupMember.nickname = nickname
                            GroupMemberRepository.saveOrUpdateGroupMember(groupMember)
                            Application.getInstance().getUIListeners(OnGroupchatRequestListener::class.java)
                                .forEachOnUi {
                                    it.onGroupchatMemberUpdated(
                                        groupChat.account,
                                        groupChat.contactJid,
                                        groupMember.id
                                    )
                                }
                        }
                    },
                    { exception: Exception? ->
                        LogManager.exception(this.javaClass.simpleName, exception)
                        if (exception is XMPPErrorException
                            && exception.xmppError.condition == XMPPError.Condition.not_allowed
                        ) Application.getInstance().runOnUiThread {
                            Toast.makeText(
                                Application.getInstance().applicationContext,
                                Application.getInstance().applicationContext.getString(R.string.groupchat_you_have_no_permissions_to_do_it),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            } catch (e: Exception) {
                LogManager.exception(this.javaClass.simpleName, e)
            }
        }
    }

    fun requestMe(accountJid: AccountJid, groupchatJid: ContactJid) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            (ChatManager.getInstance().getChat(accountJid, groupchatJid) as? GroupChat)?.let { chat ->
                if (getGroupMembers(groupchatJid).isNotEmpty()) {
                    Application.getInstance().runOnUiThread {
                        Application.getInstance().getUIListeners(OnGroupchatRequestListener::class.java)
                            .forEachOnUi { it.onMeReceived(accountJid, groupchatJid) }
                    }
                }
                try {
                    AccountManager.getInstance().getAccount(accountJid)?.connection?.sendIqWithResponseCallback(
                        GroupchatMembersQueryIQ(chat).apply { queryId = "" },
                        GroupchatMeResultListener(accountJid, groupchatJid)
                    )
                } catch (e: Exception) {
                    LogManager.exception(this.javaClass.simpleName, e)
                }
            }
        }
    }

    fun requestGroupchatMembers(account: AccountJid, groupchatJid: ContactJid) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            (ChatManager.getInstance().getChat(account, groupchatJid) as? GroupChat)?.let { chat ->
                if (getGroupMembers(groupchatJid).isNotEmpty()) {
                    Application.getInstance().getUIListeners(OnGroupchatRequestListener::class.java)
                        .forEachOnUi { it.onGroupchatMembersReceived(account, groupchatJid) }
                }
                try {
                    AccountManager.getInstance().getAccount(account)?.connection?.sendIqWithResponseCallback(
                        GroupchatMembersQueryIQ(chat).apply {
                            val version = chat.membersListVersion
                            queryVersion = if (version != null && version.isNotEmpty()) version else "1"
                        },
                        GroupchatMembersResultListener(account, groupchatJid)
                    )
                } catch (e: Exception) {
                    LogManager.exception(this.javaClass.simpleName, e)
                }
            }

        }
    }

    fun requestGroupchatMemberInfo(groupChat: GroupChat, memberId: String?) {
        val accountJid = groupChat.account
        val groupchatJid = groupChat.contactJid
        Application.getInstance().runInBackgroundNetworkUserRequest {
            if (getGroupMembers(groupchatJid).isNotEmpty()) {
                Application.getInstance().getUIListeners(OnGroupchatRequestListener::class.java)
                    .forEachOnUi { it.onMeReceived(accountJid, groupchatJid) }
            }
            try {
                AccountManager.getInstance().getAccount(accountJid)?.connection?.sendIqWithResponseCallback(
                    GroupchatMembersQueryIQ(groupChat).apply { queryId = memberId },
                    GroupchatMembersResultListener(accountJid, groupchatJid)
                )
            } catch (e: Exception) {
                LogManager.exception(this.javaClass.simpleName, e)
            }
        }
    }

    fun requestGroupchatMemberRightsChange(groupChat: GroupChat, dataForm: DataForm) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                AccountManager.getInstance().getAccount(groupChat.account)?.connection?.sendIqWithResponseCallback(
                    GroupRequestMemberRightsChangeIQ(groupChat, dataForm),
                    {
                        Application.getInstance().getUIListeners(OnGroupMemberRightsListener::class.java)
                            .forEachOnUi { it.onSuccessfullyChanges(groupChat) }
                    },
                    {
                        Application.getInstance().getUIListeners(OnGroupMemberRightsListener::class.java)
                            .forEachOnUi { it.onError(groupChat) }
                    }
                )
            } catch (e: Exception) {
                Application.getInstance().getUIListeners(OnGroupMemberRightsListener::class.java)
                    .forEachOnUi { it.onError(groupChat) }
            }
        }
    }

    fun requestGroupchatMemberRightsForm(accountJid: AccountJid, groupchatJid: ContactJid, groupMember: GroupMember) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            val chat = (ChatManager.getInstance().getChat(accountJid, groupchatJid) as? RegularChat)?.let {
                ChatManager.getInstance().removeChat(it)
                ChatManager.getInstance().createGroupChat(accountJid, groupchatJid)
            } ?: ChatManager.getInstance().createGroupChat(accountJid, groupchatJid)
            val accountItem = AccountManager.getInstance().getAccount(accountJid)
            if (accountItem != null) {
                try {
                    accountItem.connection.sendIqWithResponseCallback(
                        GroupchatMemberRightsQueryIQ(chat, groupMember.id),
                        GroupchatMemberRightsFormResultListener(accountJid, groupchatJid)
                    )
                } catch (e: Exception) {
                    LogManager.exception(this.javaClass.simpleName, e)
                }
            }
        }
    }

    private class GroupchatMemberRightsFormResultListener(
        private val account: AccountJid?,
        private val groupchatJid: ContactJid?
    ) : StanzaListener {
        override fun processStanza(packet: Stanza) {
            if (packet is GroupchatMemberRightsReplyIQ) {
                Application.getInstance().getUIListeners(OnGroupMemberRightsListener::class.java)
                    .forEachOnUi { listener ->
                        (ChatManager.getInstance().getChat(account, groupchatJid) as? GroupChat?)?.let { groupChat ->
                            listener.onGroupchatMemberRightsFormReceived(groupChat, packet)
                        }
                    }
            }
        }
    }

    private class GroupchatMeResultListener(
        private val accountJid: AccountJid,
        private val groupchatJid: ContactJid,
    ) : StanzaListener {
        override fun processStanza(packet: Stanza) {
            if (packet is GroupchatMembersResultIQ) {
                if (groupchatJid.bareJid.equals(packet.getFrom().asBareJid())
                    && accountJid.bareJid.equals(packet.getTo().asBareJid())
                ) {
                    packet.listOfMembers.map { memberExtension ->
                        val id = memberExtension.id
                        if (members[id] == null) members[id] = GroupMember(id)
                        (members[id] ?: GroupMember(id))
                            .also { members[id] = it }
                            .apply {
                                groupJid = groupchatJid.toString()
                                role = memberExtension.role
                                nickname = memberExtension.nickname
                                badge = memberExtension.badge
                                (memberExtension.jid)?.let { jid = it }
                                (memberExtension.lastPresent)?.let { lastPresent = it }
                                (memberExtension.avatarInfo)?.let {
                                    avatarHash = it.id
                                    avatarUrl = it.url.toString()
                                }
                                isMe = true
                            }.also {
                                if (memberExtension.subscription != null && memberExtension.subscription != "both") {
                                    removeGroupMember(it.id)
                                } else GroupMemberRepository.saveOrUpdateGroupMember(it)
                            }
                    }
                    Application.getInstance().getUIListeners(OnGroupchatRequestListener::class.java)
                        .forEachOnUi { it.onMeReceived(accountJid, groupchatJid) }
                }
            }
        }
    }

    private class GroupchatMembersResultListener(
        private val account: AccountJid,
        private val groupchatJid: ContactJid,
    ) : StanzaListener {
        override fun processStanza(packet: Stanza) {
            if (packet is GroupchatMembersResultIQ) {
                if (groupchatJid.bareJid.equals(packet.getFrom().asBareJid())
                    && account.bareJid.equals(packet.getTo().asBareJid())
                ) {
                    packet.listOfMembers.map { memberExtension ->
                        val id = memberExtension.id
                        members[id] ?: GroupMember(id)
                            .also {
                                members[id] = it
                            }.apply {
                                groupJid = groupchatJid.toString()
                                role = memberExtension.role
                                nickname = memberExtension.nickname
                                badge = memberExtension.badge
                                (memberExtension.jid)?.let { jid = it }
                                (memberExtension.lastPresent)?.let { lastPresent = it }
                                (memberExtension.avatarInfo)?.let {
                                    avatarHash = it.id
                                    avatarUrl = it.url.toString()
                                }
                            }.also {
                                if (memberExtension.subscription != null && memberExtension.subscription != "both") {
                                    LogManager.exception(this.javaClass.simpleName, Exception())
                                } else GroupMemberRepository.saveOrUpdateGroupMember(it)
                            }
                    }
                    (ChatManager.getInstance().getChat(account, groupchatJid) as? GroupChat)?.let {
                        it.membersListVersion = packet.queryVersion
                        ChatManager.getInstance().saveOrUpdateChatDataToRealm(it)
                    }
                    Application.getInstance().getUIListeners(OnGroupchatRequestListener::class.java)
                        .forEachOnUi { listener -> listener.onGroupchatMembersReceived(account, groupchatJid) }
                }
            }
        }
    }

    private fun getGroupMemberFromGroupMemberExtensionElement(
        memberExtensionElement: GroupMemberExtensionElement,
        memberGroupJid: BareJid?
    ) = GroupMember(memberExtensionElement.id).apply {
        (memberGroupJid)?.let { groupJid = it.toString() }
        (memberExtensionElement.avatarInfo)?.let {
            avatarHash = it.id
            avatarUrl = it.url.toString()
        }
        lastPresent = memberExtensionElement.lastPresent
        badge = memberExtensionElement.badge
        jid = memberExtensionElement.jid
        nickname = memberExtensionElement.nickname
        role = memberExtensionElement.role
    }

}