package com.xabber.android.data.extension.groups

import android.os.Looper
import android.widget.Toast
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.BaseIqResultUiListener
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.database.DatabaseManager
import com.xabber.android.data.database.realmobjects.GroupMemberRealmObject
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
import java.util.concurrent.atomic.AtomicInteger

object GroupMemberManager {

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

    fun getGroupMemberById(account: AccountJid, groupJid: ContactJid, memberId: String): GroupMemberRealmObject? {
        return if (Looper.getMainLooper() == Looper.getMainLooper()) {
            DatabaseManager.getInstance().defaultRealmInstance.where(GroupMemberRealmObject::class.java)
                .equalTo(GroupMemberRealmObject.Fields.ACCOUNT_JID, account.toString())
                .equalTo(GroupMemberRealmObject.Fields.GROUP_JID, groupJid.toString())
                .equalTo(GroupMemberRealmObject.Fields.MEMBER_ID, memberId)
                .findFirst()
        } else DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
            realm.where(GroupMemberRealmObject::class.java)
                .equalTo(GroupMemberRealmObject.Fields.ACCOUNT_JID, account.toString())
                .equalTo(GroupMemberRealmObject.Fields.GROUP_JID, groupJid.toString())
                .equalTo(GroupMemberRealmObject.Fields.MEMBER_ID, memberId)
                .findFirst()
                ?.let { realm.copyFromRealm(it) }
        }
    }

    fun getMe(group: GroupChat): GroupMemberRealmObject? =
        if (Looper.getMainLooper() == Looper.getMainLooper()) {
            DatabaseManager.getInstance().defaultRealmInstance.where(GroupMemberRealmObject::class.java)
                .equalTo(GroupMemberRealmObject.Fields.ACCOUNT_JID, group.account.toString())
                .equalTo(GroupMemberRealmObject.Fields.GROUP_JID, group.contactJid.toString())
                .equalTo(GroupMemberRealmObject.Fields.IS_ME, true)
                .findFirst()
        } else DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
            realm.where(GroupMemberRealmObject::class.java)
                .equalTo(GroupMemberRealmObject.Fields.ACCOUNT_JID, group.account.toString())
                .equalTo(GroupMemberRealmObject.Fields.GROUP_JID, group.contactJid.toString())
                .equalTo(GroupMemberRealmObject.Fields.IS_ME, true)
                .findFirst()
                ?.let { realm.copyFromRealm(it) }
        }

    fun getCurrentGroupMembers(group: GroupChat): List<GroupMemberRealmObject?> =
        if (Looper.getMainLooper() == Looper.getMainLooper()) {
            DatabaseManager.getInstance().defaultRealmInstance.where(GroupMemberRealmObject::class.java)
                .equalTo(GroupMemberRealmObject.Fields.ACCOUNT_JID, group.account.toString())
                .equalTo(GroupMemberRealmObject.Fields.GROUP_JID, group.contactJid.toString())
                .equalTo(
                    GroupMemberRealmObject.Fields.SUBSCRIPTION_STATE,
                    GroupMemberRealmObject.SubscriptionState.both.toString()
                )
                .findAll()
        } else DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
            realm.copyFromRealm(
                realm.where(GroupMemberRealmObject::class.java)
                    .equalTo(GroupMemberRealmObject.Fields.ACCOUNT_JID, group.account.toString())
                    .equalTo(GroupMemberRealmObject.Fields.GROUP_JID, group.contactJid.toString())
                    .equalTo(
                        GroupMemberRealmObject.Fields.SUBSCRIPTION_STATE,
                        GroupMemberRealmObject.SubscriptionState.both.toString()
                    )
                    .findAll()
            )
        }

    fun removeGroupMember(account: AccountJid, groupJid: ContactJid, memberId: String) {
        Application.getInstance().runInBackground {
            DatabaseManager.getInstance().defaultRealmInstance.use {
                it.executeTransaction { realm ->
                    realm.where(GroupMemberRealmObject::class.java)
                        .equalTo(GroupMemberRealmObject.Fields.ACCOUNT_JID, account.bareJid.toString())
                        .equalTo(GroupMemberRealmObject.Fields.GROUP_JID, groupJid.bareJid.toString())
                        .equalTo(GroupMemberRealmObject.Fields.MEMBER_ID, memberId)
                        .findFirst()
                        ?.deleteFromRealm()
                }
            }
        }
    }

    fun saveOrUpdateMemberFromMessage(
        user: GroupMemberExtensionElement, account: AccountJid, groupJid: ContactJid
    ): GroupMemberRealmObject {
        var result: GroupMemberRealmObject? = null
        if (Looper.getMainLooper() == Looper.myLooper()) {
            DatabaseManager.getInstance().defaultRealmInstance.executeTransaction { realm ->
                result = (
                        realm.where(GroupMemberRealmObject::class.java)
                            .equalTo(GroupMemberRealmObject.Fields.MEMBER_ID, user.id)
                            .equalTo(GroupMemberRealmObject.Fields.ACCOUNT_JID, account.toString())
                            .equalTo(GroupMemberRealmObject.Fields.GROUP_JID, groupJid.toString())
                            .findFirst()
                            ?: GroupMemberRealmObject.createGroupMemberRealmObject(account, groupJid, user.id))
                    .apply {
                        user.avatarInfo?.let {
                            avatarHash = it.id
                            avatarUrl = it.url.toString()
                        }

                        user.subscription?.let {
                            subscriptionState = GroupMemberRealmObject.SubscriptionState.valueOf(it)
                        }

                        lastSeen = user.lastPresent
                        badge = user.badge
                        jid = user.jid
                        nickname = user.nickname
                        role = GroupMemberRealmObject.Role.valueOf(user.role)
                    }.also { gmro -> realm.insertOrUpdate(gmro) }
            }
        } else {
            DatabaseManager.getInstance().defaultRealmInstance.use { realm1 ->
                realm1.executeTransaction { realm ->
                    result = (realm.where(GroupMemberRealmObject::class.java)
                        .equalTo(GroupMemberRealmObject.Fields.MEMBER_ID, user.id)
                        .equalTo(GroupMemberRealmObject.Fields.ACCOUNT_JID, account.toString())
                        .equalTo(GroupMemberRealmObject.Fields.GROUP_JID, groupJid.toString())
                        .findFirst()
                        ?: GroupMemberRealmObject.createGroupMemberRealmObject(account, groupJid, user.id))
                        .apply {
                            user.avatarInfo?.let {
                                avatarHash = it.id
                                avatarUrl = it.url.toString()
                            }

                            user.subscription?.let {
                                subscriptionState = GroupMemberRealmObject.SubscriptionState.valueOf(it)
                            }

                            lastSeen = user.lastPresent
                            badge = user.badge
                            jid = user.jid
                            nickname = user.nickname
                            role = GroupMemberRealmObject.Role.valueOf(user.role)
                        }.also {
                            realm.insertOrUpdate(it)
                        }
                }
            }
        }
        return result!!
    }

    fun kickMember(groupMemberId: String, groupChat: GroupChat, listener: BaseIqResultUiListener) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                groupChat.fullJidIfPossible?.let { fullJid ->
                    AccountManager.getInstance().getAccount(groupChat.account)?.connection?.sendIqWithResponseCallback(
                        KickGroupMemberIQ(groupMemberId, fullJid),
                        listener,
                        listener
                    )
                }
            } catch (e: Exception) {
                LogManager.exception(this.javaClass.simpleName, e)
                listener.onOtherError(e)
            }
        }
    }

    fun kickAndBlockMember(memberId: String, groupChat: GroupChat, listener: BaseIqResultUiListener) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                groupChat.fullJidIfPossible?.let { fullJid ->
                    AccountManager.getInstance().getAccount(groupChat.account)?.connection
                        ?.sendIqWithResponseCallback(
                            BlockGroupMemberIQ(fullJid, memberId),
                            listener,
                            listener
                        )
                }
            } catch (e: Exception) {
                LogManager.exception(this.javaClass.simpleName, e)
                listener.onOtherError(e)
            }
        }
    }

    fun requestGroupchatBlocklistList(
        account: AccountJid, groupchatJid: ContactJid, listener: StanzaListener, exceptionCallback: ExceptionCallback?
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

    fun unblockGroupMember(
        account: AccountJid, groupJid: ContactJid, memberId: String, listener: BaseIqResultUiListener
    ) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                (ChatManager.getInstance().getChat(account, groupJid) as? GroupChat)?.let { groupChat ->
                    val unblockElement =
                        GroupchatBlocklistItemElement(GroupchatBlocklistItemElement.ItemType.id, memberId)
                    AccountManager.getInstance().getAccount(account)?.connection?.sendIqWithResponseCallback(
                        GroupchatBlocklistUnblockIQ(groupChat, unblockElement),
                        { packet: Stanza? ->
                            (packet as? IQ)?.let { iq ->
                                if (IQ.Type.result == iq.type) {
                                    groupChat.listOfBlockedElements.remove(unblockElement)
                                    listener.onResult()
                                } else {
                                    listener.onOtherError()
                                }
                            }
                        },
                        { exception ->
                            (exception as? XMPPErrorException)?.let {
                                listener.onIqError(it.xmppError)
                            }
                        }
                    )
                }

            } catch (e: Exception) {
                LogManager.exception(this, e)
                listener.onOtherError(e)
            }
        }
    }

    fun unblockGroupchatBlockedElement(
        account: AccountJid, groupchatJid: ContactJid, blockedElement: GroupchatBlocklistItemElement
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

    fun createChatWithIncognitoMember(groupChat: GroupChat, groupMemberId: String) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                AccountManager.getInstance().getAccount(groupChat.account)?.connection?.sendIqWithResponseCallback(
                    CreatePtpGroupIQ(groupChat, groupMemberId),
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

    fun sendSetMemberBadgeIqRequest(groupChat: GroupChat, groupMemberId: String, badge: String?) {
        val accountJid = groupChat.account
        val groupJid = groupChat.contactJid
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                AccountManager.getInstance().getAccount(accountJid)?.connection?.sendIqWithResponseCallback(
                    ChangeGroupchatMemberPreferencesIQ(groupChat, groupMemberId, badge, null),
                    { packet: Stanza? ->
                        if (packet is IQ && packet.type == IQ.Type.result) {
                            DatabaseManager.getInstance().defaultRealmInstance.use {
                                it.executeTransaction { realm ->
                                    realm.where(GroupMemberRealmObject::class.java)
                                        .equalTo(GroupMemberRealmObject.Fields.ACCOUNT_JID, accountJid.toString())
                                        .equalTo(GroupMemberRealmObject.Fields.GROUP_JID, groupJid.toString())
                                        .equalTo(GroupMemberRealmObject.Fields.MEMBER_ID, groupMemberId)
                                        .findFirst()
                                        ?.apply {
                                            this.badge = badge
                                            realm.copyToRealmOrUpdate(this)
                                        }
                                }
                            }
                            Application.getInstance().getUIListeners(OnGroupchatRequestListener::class.java)
                                .forEachOnUi { it.onGroupchatMemberUpdated(accountJid, groupJid, groupMemberId) }

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

    fun sendSetMemberNicknameIqRequest(groupChat: GroupChat, groupMemberId: String, nickname: String?) {
        val accountJid = groupChat.account
        val groupJid = groupChat.contactJid
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                AccountManager.getInstance().getAccount(accountJid)?.connection?.sendIqWithResponseCallback(
                    ChangeGroupchatMemberPreferencesIQ(groupChat, groupMemberId, null, nickname),
                    { packet: Stanza? ->
                        if (packet is IQ && packet.type == IQ.Type.result) {
                            DatabaseManager.getInstance().defaultRealmInstance.use {
                                it.executeTransaction { realm ->
                                    realm.where(GroupMemberRealmObject::class.java)
                                        .equalTo(GroupMemberRealmObject.Fields.ACCOUNT_JID, accountJid.toString())
                                        .equalTo(GroupMemberRealmObject.Fields.GROUP_JID, groupJid.toString())
                                        .equalTo(GroupMemberRealmObject.Fields.MEMBER_ID, groupMemberId)
                                        .findFirst()
                                        ?.apply {
                                            this.nickname = nickname
                                            realm.copyToRealmOrUpdate(this)
                                        }
                                }
                            }
                            Application.getInstance().getUIListeners(OnGroupchatRequestListener::class.java)
                                .forEachOnUi { it.onGroupchatMemberUpdated(accountJid, groupJid, groupMemberId) }
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
                try {
                    AccountManager.getInstance().getAccount(accountJid)?.connection?.sendIqWithResponseCallback(
                        GroupchatMembersQueryIQ(chat).apply { queryId = "" }
                    ) { packet ->
                        if (packet is GroupchatMembersResultIQ
                            && groupchatJid.bareJid.equals(packet.getFrom().asBareJid())
                            && accountJid.bareJid.equals(packet.getTo().asBareJid())
                        ) {
                            DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                                realm.executeTransaction { realm1 ->
                                    packet.listOfMembers.map { memberExtension ->
                                        (realm1.where(GroupMemberRealmObject::class.java)
                                            .equalTo(GroupMemberRealmObject.Fields.ACCOUNT_JID, accountJid.toString())
                                            .equalTo(GroupMemberRealmObject.Fields.GROUP_JID, groupchatJid.toString())
                                            .equalTo(GroupMemberRealmObject.Fields.MEMBER_ID, memberExtension.id)
                                            .findFirst()
                                            ?: GroupMemberRealmObject.createGroupMemberRealmObject(
                                                accountJid, groupchatJid, memberExtension.id
                                            ))
                                            ?.apply {
                                                role = GroupMemberRealmObject.Role.valueOf(memberExtension.role)
                                                nickname = memberExtension.nickname
                                                badge = memberExtension.badge
                                                memberExtension.jid?.let { jid = it }
                                                memberExtension.lastPresent?.let { lastSeen = it }
                                                memberExtension.avatarInfo?.let {
                                                    avatarHash = it.id
                                                    avatarUrl = it.url.toString()
                                                }
                                                memberExtension.subscription?.let {
                                                    subscriptionState =
                                                        GroupMemberRealmObject.SubscriptionState.valueOf(it)
                                                }
                                                isMe = true
                                            }
                                            ?.let { gmro -> realm1.insertOrUpdate(gmro) }
                                    }
                                }
                            }
                            Application.getInstance().getUIListeners(OnGroupchatRequestListener::class.java)
                                .forEachOnUi { it.onMeReceived(accountJid, groupchatJid) }
                        }
                    }
                } catch (e: Exception) {
                    LogManager.exception(this.javaClass.simpleName, e)
                }
            }
        }
    }

    fun requestGroupchatMembers(account: AccountJid, groupchatJid: ContactJid) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            (ChatManager.getInstance().getChat(account, groupchatJid) as? GroupChat)?.let { chat ->
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
            try {
                AccountManager.getInstance().getAccount(accountJid)?.connection?.sendIqWithResponseCallback(
                    GroupchatMembersQueryIQ(groupChat).apply { queryId = memberId }
                ) { packet ->
                    if (packet is GroupchatMembersResultIQ
                        && groupchatJid.bareJid.equals(packet.getFrom().asBareJid())
                        && accountJid.bareJid.equals(packet.getTo().asBareJid())
                    ) {
                        Application.getInstance().runInBackground {
                            DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                                realm.executeTransaction { realm1 ->
                                    packet.listOfMembers.map { memberExtension ->
                                        (realm1.where(GroupMemberRealmObject::class.java)
                                            .equalTo(GroupMemberRealmObject.Fields.ACCOUNT_JID, accountJid.toString())
                                            .equalTo(GroupMemberRealmObject.Fields.GROUP_JID, groupchatJid.toString())
                                            .equalTo(GroupMemberRealmObject.Fields.MEMBER_ID, memberExtension.id)
                                            .findFirst()
                                            ?: GroupMemberRealmObject.createGroupMemberRealmObject(
                                                accountJid, groupchatJid, memberExtension.id
                                            ))
                                            ?.apply {
                                                role = GroupMemberRealmObject.Role.valueOf(memberExtension.role)
                                                nickname = memberExtension.nickname
                                                badge = memberExtension.badge
                                                memberExtension.jid?.let { jid = it }
                                                memberExtension.lastPresent?.let { lastSeen = it }
                                                memberExtension.avatarInfo?.let {
                                                    avatarHash = it.id
                                                    avatarUrl = it.url.toString()
                                                }
                                                memberExtension.subscription?.let {
                                                    subscriptionState =
                                                        GroupMemberRealmObject.SubscriptionState.valueOf(it)
                                                }
                                            }
                                            ?.let { gmro -> realm1.insertOrUpdate(gmro) }
                                    }
                                }
                            }
                            Application.getInstance().getUIListeners(OnGroupchatRequestListener::class.java)
                                .forEachOnUi { listener ->
                                    listener.onGroupchatMembersReceived(accountJid, groupchatJid)
                                }
                        }
                    }
                }
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

    fun requestGroupchatMemberRightsForm(group: GroupChat, groupMemberId: String) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                AccountManager.getInstance().getAccount(group.account)?.connection?.sendIqWithResponseCallback(
                    GroupchatMemberRightsQueryIQ(group, groupMemberId)
                ) { packet ->
                    if (packet is GroupchatMemberRightsReplyIQ) {
                        Application.getInstance().getUIListeners(OnGroupMemberRightsListener::class.java)
                            .forEachOnUi { listener -> listener.onGroupchatMemberRightsFormReceived(group, packet) }
                    }
                }
            } catch (e: Exception) {
                LogManager.exception(this.javaClass.simpleName, e)
            }
        }
    }

    private class GroupchatMembersResultListener(
        private val account: AccountJid, private val groupchatJid: ContactJid,
    ) : StanzaListener {
        override fun processStanza(packet: Stanza) {
            if (packet is GroupchatMembersResultIQ
                && groupchatJid.bareJid.equals(packet.getFrom().asBareJid())
                && account.bareJid.equals(packet.getTo().asBareJid())
            ) {
                Application.getInstance().runInBackground {
                    DatabaseManager.getInstance().defaultRealmInstance.use { realm ->
                        realm.executeTransaction { realm1 ->
                            packet.listOfMembers.map { memberExtension ->
                                (realm1.where(GroupMemberRealmObject::class.java)
                                    .equalTo(GroupMemberRealmObject.Fields.ACCOUNT_JID, account.toString())
                                    .equalTo(GroupMemberRealmObject.Fields.GROUP_JID, groupchatJid.toString())
                                    .equalTo(GroupMemberRealmObject.Fields.MEMBER_ID, memberExtension.id)
                                    .findFirst()
                                    ?: GroupMemberRealmObject.createGroupMemberRealmObject(
                                        account, groupchatJid, memberExtension.id
                                    ))
                                    ?.apply {
                                        role = GroupMemberRealmObject.Role.valueOf(memberExtension.role)
                                        nickname = memberExtension.nickname
                                        badge = memberExtension.badge
                                        memberExtension.jid?.let { jid = it }
                                        memberExtension.lastPresent?.let { lastSeen = it }
                                        memberExtension.avatarInfo?.let {
                                            avatarHash = it.id
                                            avatarUrl = it.url.toString()
                                        }
                                        memberExtension.subscription?.let {
                                            subscriptionState = GroupMemberRealmObject.SubscriptionState.valueOf(it)
                                        }
                                    }
                                    ?.let { gmro -> realm1.insertOrUpdate(gmro) }
                            }
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

}