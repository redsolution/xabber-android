package com.xabber.android.data.extension.groups

import android.widget.Toast
import com.xabber.android.R
import com.xabber.android.data.*
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.connection.BaseIqResultUiListener
import com.xabber.android.data.connection.ConnectionItem
import com.xabber.android.data.connection.OnPacketListener
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.database.repositories.AccountRepository
import com.xabber.android.data.database.repositories.MessageRepository
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.archive.MessageArchiveManager
import com.xabber.android.data.extension.avatar.AvatarManager
import com.xabber.android.data.extension.retract.RetractManager
import com.xabber.android.data.extension.vcard.VCardManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.MessageManager
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.message.chat.RegularChat
import com.xabber.android.data.roster.PresenceManager
import com.xabber.android.data.roster.PresenceManager.addAutoAcceptGroupSubscription
import com.xabber.android.data.roster.PresenceManager.requestSubscription
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.*
import com.xabber.xmpp.avatar.DataExtension
import com.xabber.xmpp.avatar.MetadataExtension
import com.xabber.xmpp.avatar.MetadataInfo
import com.xabber.xmpp.avatar.UserAvatarManager
import com.xabber.xmpp.groups.GroupPinMessageIQ
import com.xabber.xmpp.groups.GroupPresenceExtensionElement
import com.xabber.xmpp.groups.GroupPresenceNotificationExtensionElement
import com.xabber.xmpp.groups.create.CreateGroupchatIQ
import com.xabber.xmpp.groups.create.CreateGroupchatIQ.ResultIq
import com.xabber.xmpp.groups.restrictions.GroupDefaultRestrictionsDataFormResultIQ
import com.xabber.xmpp.groups.restrictions.RequestGroupDefaultRestrictionsDataFormIQ
import com.xabber.xmpp.groups.restrictions.RequestToChangeGroupDefaultRestrictionsIQ
import com.xabber.xmpp.groups.settings.GroupSettingsDataFormResultIQ
import com.xabber.xmpp.groups.settings.GroupSettingsRequestFormQueryIQ
import com.xabber.xmpp.groups.settings.SetGroupSettingsRequestIQ
import com.xabber.xmpp.groups.status.GroupSetStatusRequestIQ
import com.xabber.xmpp.groups.status.GroupStatusDataFormIQ
import com.xabber.xmpp.groups.status.GroupStatusFormRequestIQ
import com.xabber.xmpp.groups.system_message.GroupSystemMessageExtensionElement
import com.xabber.xmpp.smack.XMPPTCPConnection
import com.xabber.xmpp.vcard.VCard
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager
import org.jivesoftware.smackx.disco.packet.DiscoverItems
import org.jivesoftware.smackx.pubsub.PayloadItem
import org.jivesoftware.smackx.pubsub.PublishItem
import org.jivesoftware.smackx.pubsub.packet.PubSub
import org.jivesoftware.smackx.xdata.packet.DataForm
import org.jxmpp.jid.Jid
import java.util.*

object GroupsManager : OnPacketListener, OnLoadListener {

    const val NAMESPACE = "https://xabber.com/protocol/groups"
    const val SYSTEM_MESSAGE_NAMESPACE = NAMESPACE + GroupSystemMessageExtensionElement.HASH_BLOCK

    private const val PRESENCE_RESEND_CYCLE_MILLIS = 30000L

    private val availableGroupServers: MutableMap<AccountJid, MutableList<Jid>?> = HashMap()
    private val customGroupServers: MutableMap<AccountJid, MutableList<String>?> = HashMap()

    /**
     * We have agreed to send "present" presence to group every @see PRESENCE_RESEND_CYCLE_MILLIS
     */
    private val groupsToSendPresence = mutableListOf<GroupChat>()

    private fun startPresenceResendingRunnable() {
        Application.getInstance().runOnUiThreadDelay(PRESENCE_RESEND_CYCLE_MILLIS) {
            groupsToSendPresence.map { sendPresenceToGroup(it) }
            startPresenceResendingRunnable()
        }
    }

    override fun onLoad() {
        availableGroupServers.clear()
        availableGroupServers.putAll(AccountRepository.getGroupServers())
        customGroupServers.clear()
        customGroupServers.putAll(AccountRepository.getCustomGroupServers())
        startPresenceResendingRunnable()
    }

    private fun sendPresenceToGroup(group: GroupChat, isPresent: Boolean = true) {
        try {
            Application.getInstance().runInBackgroundNetworkUserRequest {
                PresenceManager.getAccountPresence(group.account)?.let { accountPresence ->
                    VCardManager.getInstance().addVCardUpdateToPresence(
                        accountPresence, AvatarManager.getInstance().getHash(group.account.bareJid)
                    )
                    AccountManager.getAccount(group.account)?.connection?.sendStanza(
                        accountPresence.apply {
                            addExtension(GroupPresenceNotificationExtensionElement(isPresent))
                            to = group.to
                        }
                    )
                }
            }
        } catch (ignore: NetworkException) {
        }
    }

    fun enableSendingPresenceToGroup(group: GroupChat, isPresent: Boolean = true) {
        sendPresenceToGroup(group, isPresent)

        if (isPresent) {
            groupsToSendPresence.add(group)
        } else {
            groupsToSendPresence.remove(group)
        }
    }

    override fun onStanza(connection: ConnectionItem?, packet: Stanza?) {
        if (packet is Presence && packet.hasExtension(GroupPresenceExtensionElement.NAMESPACE)) {
            processPresence(packet)
        } else {
            (packet as? DiscoverItems)?.let { pack ->
                connection?.let { connection ->
                    processDiscoInfoIq(connection, pack)
                }
            }
        }
    }

    private fun processDiscoInfoIq(connectionItem: ConnectionItem, packet: Stanza) {
        try {
            val accountJid = connectionItem.account
            if (availableGroupServers[accountJid] == null) availableGroupServers.remove(accountJid)
            availableGroupServers[accountJid] = ArrayList()
            for (item in (packet as DiscoverItems).items) {
                if (NAMESPACE == item.node) {
                    val srvJid: Jid = ContactJid.from(item.entityID).bareJid
                    availableGroupServers[accountJid]?.add(srvJid)
                }
            }
            AccountRepository.saveOrUpdateGroupServers(
                accountJid,
                availableGroupServers[accountJid]
            )
            LogManager.d(this::class.java.simpleName, "Got a group server list")
        } catch (e: Exception) {
            LogManager.exception(this::class.java.simpleName, e)
        }
    }

    private fun processPresence(packet: Presence) {
        try {
            val presence =
                packet.getExtension(GroupPresenceExtensionElement.NAMESPACE) as GroupPresenceExtensionElement
            val accountJid = AccountJid.from(packet.to.toString())
            val contactJid = ContactJid.from(packet.from).bareUserJid
            if (ChatManager.getInstance().getChat(accountJid, contactJid) == null) {
                ChatManager.getInstance().createGroupChat(accountJid, contactJid)
            } else if (ChatManager.getInstance().getChat(accountJid, contactJid) is RegularChat) {
                ChatManager.getInstance().removeChat(accountJid, contactJid)
                ChatManager.getInstance().createGroupChat(accountJid, contactJid)
            }
            val chat = ChatManager.getInstance().getChat(accountJid, contactJid) as GroupChat

            if (GroupMemberManager.getMe(chat) == null) {
                GroupMemberManager.requestMe(accountJid, contactJid)
            }

            if (presence.pinnedMessageId != null
                && presence.pinnedMessageId.isNotEmpty()
                && presence.pinnedMessageId != "0"
            ) {
                val pinnedMessage =
                    MessageRepository.getMessageFromRealmByStanzaId(presence.pinnedMessageId)
                if (pinnedMessage == null || pinnedMessage.timestamp == null) {
                    MessageArchiveManager.loadMessageByStanzaId(chat, presence.pinnedMessageId)
                }
                chat.pinnedMessageId = presence.pinnedMessageId
            }
            chat.name = presence.name
            chat.numberOfOnlineMembers = presence.presentMembers

            Application.getInstance().getUIListeners(OnGroupPresenceUpdatedListener::class.java)
                .forEachOnUi { listener ->
                    listener.onGroupPresenceUpdated(
                        accountJid,
                        contactJid,
                        packet
                    )
                }

            ChatManager.getInstance().saveOrUpdateChatDataToRealm(chat)
        } catch (e: Exception) {
            LogManager.exception(this::class.java.simpleName, e)
        }
    }

    fun processVcard(accountJid: AccountJid?, groupJid: ContactJid?, vcard: VCard) {
        ChatManager.getInstance().saveOrUpdateChatDataToRealm(
            (ChatManager.getInstance().getChat(accountJid, groupJid) as? GroupChat
                ?: return).apply {
                description = vcard.description
                privacyType = vcard.privacyType
                indexType = vcard.indexType
                membershipType = vcard.membershipType
                if (vcard.nickName != null && vcard.nickName.isNotEmpty()) name = vcard.nickName
                numberOfMembers = vcard.membersCount
            }
        )
    }

    /**
     * Call when account was been kicked from group or when group was been deleted
     * @param accountJid account that got unsubscribed presence
     * @param contactJid of group that sent unsubscribed presence
     */
    fun onUnsubscribePresence(accountJid: AccountJid, contactJid: ContactJid, presence: Presence) {
        Application.getInstance().getUIListeners(OnGroupPresenceUpdatedListener::class.java)
            .forEachOnUi {
                it.onGroupPresenceUpdated(accountJid, contactJid, presence)
            }
        MessageManager.getInstance().clearHistory(accountJid, contactJid)
        if (RetractManager.isSupported(accountJid)) {
            RetractManager.sendRetractAllMessagesRequest(accountJid, contactJid)
        }
        RosterManager.getInstance().removeContact(accountJid, contactJid)
        ChatManager.getInstance().removeChat(accountJid, contactJid)
    }

    fun isSupported(connection: XMPPTCPConnection?) =
        try {
            ServiceDiscoveryManager.getInstanceFor(connection).serverSupportsFeature(NAMESPACE)
        } catch (e: Exception) {
            LogManager.exception(this::class.java.simpleName, e)
            false
        }

    fun isSupported(accountJid: AccountJid?) =
        isSupported(AccountManager.getAccount(accountJid)?.connection)

    fun sendCreateGroupchatRequest(
        accountJid: AccountJid,
        server: String,
        groupName: String,
        groupDescription: String,
        localpart: String,
        groupMembershipType: GroupMembershipType,
        groupIndexType: GroupIndexType = GroupIndexType.NONE,
        groupPrivacyType: GroupPrivacyType = GroupPrivacyType.NONE,
        listener: BaseIqResultUiListener? = null
    ) {
        try {
            listener?.onSend()
            AccountManager.getAccount(accountJid)?.connection
                ?.sendIqWithResponseCallback(
                    CreateGroupchatIQ(
                        accountJid.fullJid,
                        server,
                        groupName,
                        localpart,
                        groupDescription,
                        groupMembershipType,
                        groupPrivacyType,
                        groupIndexType
                    ),
                    { packet: Stanza ->
                        if (packet is ResultIq && packet.type == IQ.Type.result) {
                            LogManager.d(
                                this::class.java.simpleName,
                                "Groupchat successfully created"
                            )
                            try {
                                val contactJid = ContactJid.from(packet.jid)
                                val account = AccountJid.from(packet.getTo().toString())
                                addAutoAcceptGroupSubscription(account, contactJid)
                                requestSubscription(account, contactJid, false)
                                val chat =
                                    ChatManager.getInstance().createGroupChat(account, contactJid)

                                chat.apply {
                                    name = groupName
                                    description = groupDescription
                                    indexType = groupIndexType
                                    membershipType = groupMembershipType
                                    privacyType = groupPrivacyType
                                }

                                ChatManager.getInstance().saveOrUpdateChatDataToRealm(chat)
                                listener?.onResult()
                            } catch (e: Exception) {
                                LogManager.exception(this::class.java.simpleName, e)
                                listener?.onOtherError(e)
                            }
                        }
                    },
                    listener
                )
        } catch (e: Exception) {
            LogManager.exception(this::class.java.simpleName, e)
            listener?.onOtherError(e)
        }
    }

    fun getAvailableGroupchatServersForAccountJid(accountJid: AccountJid) =
        availableGroupServers[accountJid]

    fun getCustomGroupServers(accountJid: AccountJid) = customGroupServers[accountJid]

    fun saveCustomGroupServer(accountJid: AccountJid, server: String) {
        if (customGroupServers[accountJid] == null) customGroupServers[accountJid] = ArrayList()
        customGroupServers[accountJid]?.add(server)
        AccountRepository.saveCustomGroupServer(accountJid, server)
    }

    fun removeCustomGroupServer(accountJid: AccountJid, string: String) {
        customGroupServers[accountJid]?.remove(string)
        AccountRepository.removeCustomGroupServer(accountJid, string)
    }

    fun requestGroupStatusForm(groupchat: GroupChat) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                AccountManager
                    .getAccount(groupchat.account)?.connection?.sendIqWithResponseCallback(
                        GroupStatusFormRequestIQ(groupchat),
                        { packet: Stanza? ->
                            if (packet is GroupStatusDataFormIQ && packet.type == IQ.Type.result) {
                                Application.getInstance()
                                    .getUIListeners(OnGroupStatusResultListener::class.java)
                                    .forEachOnUi { listener ->
                                        listener.onStatusDataFormReceived(
                                            groupchat,
                                            packet.dataForm ?: return@forEachOnUi
                                        )
                                    }
                            }
                        },
                        {
                            Application.getInstance()
                                .getUIListeners(OnGroupStatusResultListener::class.java)
                                .forEachOnUi { listener -> listener.onError(groupchat) }
                        }
                    )
            } catch (e: Exception) {
                LogManager.exception(this::class.java.simpleName, e)
                Application.getInstance().getUIListeners(OnGroupStatusResultListener::class.java)
                    .forEachOnUi { listener -> listener.onError(groupchat) }
            }
        }
    }

    fun sendSetGroupchatStatusRequest(groupChat: GroupChat, dataForm: DataForm) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                AccountManager
                    .getAccount(groupChat.account)?.connection?.sendIqWithResponseCallback(
                        GroupSetStatusRequestIQ(groupChat, dataForm),
                        { packet: Stanza? ->
                            if (packet is IQ && packet.type == IQ.Type.result) {
                                Application.getInstance()
                                    .getUIListeners(OnGroupStatusResultListener::class.java)
                                    .forEachOnUi { listener ->
                                        listener.onStatusSuccessfullyChanged(
                                            groupChat
                                        )
                                    }
                            }
                        },
                        {
                            Application.getInstance()
                                .getUIListeners(OnGroupStatusResultListener::class.java)
                                .forEachOnUi { listener -> listener.onError(groupChat) }
                        }
                    )
            } catch (e: Exception) {
                LogManager.exception(this::class.java.simpleName, e)
                Application.getInstance().getUIListeners(OnGroupStatusResultListener::class.java)
                    .forEachOnUi { listener -> listener.onError(groupChat) }
            }
        }
    }

    fun requestGroupDefaultRestrictionsDataForm(groupchat: GroupChat) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                AccountManager
                    .getAccount(groupchat.account)?.connection?.sendIqWithResponseCallback(
                        RequestGroupDefaultRestrictionsDataFormIQ(groupchat),
                        { packet: Stanza? ->
                            if (packet is GroupDefaultRestrictionsDataFormResultIQ && packet.type == IQ.Type.result) {
                                Application.getInstance()
                                    .getUIListeners(OnGroupDefaultRestrictionsListener::class.java)
                                    .forEachOnUi { listener ->
                                        listener.onDataFormReceived(
                                            groupchat,
                                            packet.dataForm ?: return@forEachOnUi
                                        )
                                    }
                            }
                        },
                        {
                            Application.getInstance()
                                .getUIListeners(OnGroupDefaultRestrictionsListener::class.java)
                                .forEachOnUi { listener -> listener.onError(groupchat) }
                        }
                    )
            } catch (e: Exception) {
                LogManager.exception(this::class.java.simpleName, e)
                Application.getInstance()
                    .getUIListeners(OnGroupDefaultRestrictionsListener::class.java)
                    .forEachOnUi { listener -> listener.onError(groupchat) }
            }
        }
    }

    fun requestSetGroupDefaultRestrictions(groupChat: GroupChat, dataForm: DataForm) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                AccountManager
                    .getAccount(groupChat.account)?.connection?.sendIqWithResponseCallback(
                        RequestToChangeGroupDefaultRestrictionsIQ(groupChat, dataForm),
                        { packet: Stanza? ->
                            if (packet is GroupDefaultRestrictionsDataFormResultIQ && packet.type == IQ.Type.result) {
                                Application.getInstance()
                                    .getUIListeners(OnGroupDefaultRestrictionsListener::class.java)
                                    .forEachOnUi { listener -> listener.onSuccessful(groupChat) }
                            }
                        },
                        {
                            Application.getInstance()
                                .getUIListeners(OnGroupDefaultRestrictionsListener::class.java)
                                .forEachOnUi { listener -> listener.onError(groupChat) }
                        }
                    )
            } catch (e: Exception) {
                LogManager.exception(this::class.java.simpleName, e)
                Application.getInstance()
                    .getUIListeners(OnGroupDefaultRestrictionsListener::class.java)
                    .forEachOnUi { listener -> listener.onError(groupChat) }
            }
        }
    }

    fun requestGroupSettingsForm(groupchat: GroupChat) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                AccountManager
                    .getAccount(groupchat.account)?.connection?.sendIqWithResponseCallback(
                        GroupSettingsRequestFormQueryIQ(groupchat),
                        { packet: Stanza? ->
                            if (packet is GroupSettingsDataFormResultIQ && packet.type == IQ.Type.result) {
                                Application.getInstance()
                                    .getUIListeners(OnGroupSettingsResultsListener::class.java)
                                    .forEachOnUi { listener ->
                                        listener.onDataFormReceived(
                                            groupchat,
                                            packet.dataFrom ?: return@forEachOnUi
                                        )
                                    }
                            }
                        },
                        {
                            Application.getInstance()
                                .getUIListeners(OnGroupSettingsResultsListener::class.java)
                                .forEachOnUi { listener ->
                                    listener.onErrorAtDataFormRequesting(
                                        groupchat
                                    )
                                }
                        }
                    )
            } catch (e: Exception) {
                LogManager.exception(this::class.java.simpleName, e)
                Application.getInstance().getUIListeners(OnGroupSettingsResultsListener::class.java)
                    .forEachOnUi { listener -> listener.onErrorAtDataFormRequesting(groupchat) }
            }
        }
    }

    fun sendSetGroupSettingsRequest(groupchat: GroupChat, dataForm: DataForm) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                AccountManager
                    .getAccount(groupchat.account)?.connection?.sendIqWithResponseCallback(
                        SetGroupSettingsRequestIQ(groupchat, dataForm),
                        { packet: Stanza? ->
                            if (packet is IQ && packet.type == IQ.Type.result) {
                                Application.getInstance()
                                    .getUIListeners(OnGroupSettingsResultsListener::class.java)
                                    .forEachOnUi { listener ->
                                        listener.onGroupSettingsSuccessfullyChanged(
                                            groupchat
                                        )
                                    }
                            }
                        },
                        {
                            Application.getInstance()
                                .getUIListeners(OnGroupSettingsResultsListener::class.java)
                                .forEachOnUi { listener ->
                                    listener.onErrorAtSettingsSetting(
                                        groupchat
                                    )
                                }
                        }
                    )
            } catch (e: Exception) {
                LogManager.exception(this::class.java.simpleName, e)
                Application.getInstance().getUIListeners(OnGroupSettingsResultsListener::class.java)
                    .forEachOnUi { listener -> listener.onErrorAtSettingsSetting(groupchat) }
            }
        }
    }

    fun sendRemoveGroupAvatarRequest(groupChat: GroupChat) {
        Application.getInstance().runInBackground {
            try {
                val item = PayloadItem(null, MetadataExtension(null))
                val publishItem = PublishItem(UserAvatarManager.METADATA_NAMESPACE, item)
                val packet = PubSub.createPubsubPacket(
                    groupChat.contactJid.bareJid,
                    IQ.Type.set,
                    publishItem,
                    null
                )
                AccountManager.getAccount(groupChat.account)?.connection
                    ?.createStanzaCollectorAndSend(packet)?.nextResultOrThrow<Stanza>(45000)
            } catch (e: Exception) {
                LogManager.exception(this::class.java.simpleName, e)
            }
        }
    }

    fun sendPublishGroupAvatar(
        groupChat: GroupChat,
        memberId: String,
        data: ByteArray,
        height: Int,
        width: Int,
        type: UserAvatarManager.ImageType
    ) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                val connectionItem =
                    AccountManager.getAccount(groupChat.account)?.connection
                val avatarHash = AvatarManager.getAvatarHash(data)
                val dataPacket = PubSub.createPubsubPacket(
                    groupChat.contactJid.bareJid,
                    IQ.Type.set,
                    PublishItem(
                        UserAvatarManager.DATA_NAMESPACE + "#" + memberId,
                        PayloadItem(avatarHash, DataExtension(data))
                    ),
                    null
                )
                connectionItem?.createStanzaCollectorAndSend(dataPacket)
                    ?.nextResultOrThrow<Stanza>(60000)
                val metadataItem = PayloadItem(
                    avatarHash,
                    MetadataExtension(
                        listOf(
                            MetadataInfo(
                                avatarHash,
                                null,
                                data.size.toLong(),
                                type.value,
                                height,
                                width
                            )
                        ),
                        null
                    )
                )
                val metadataPacket = PubSub.createPubsubPacket(
                    groupChat.contactJid.bareJid,
                    IQ.Type.set,
                    PublishItem(
                        UserAvatarManager.METADATA_NAMESPACE + "#" + memberId,
                        metadataItem
                    ),
                    null
                )
                connectionItem?.createStanzaCollectorAndSend(metadataPacket)
                    ?.nextResultOrThrow<Stanza>(45000)
            } catch (e: Exception) {
                LogManager.exception(this::class.java.simpleName, e)
            }
        }
    }

    fun sendUnPinMessageRequest(groupChat: GroupChat) {
        //todo add privilege checking
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                AccountManager
                    .getAccount(groupChat.account)?.connection?.sendIqWithResponseCallback(
                        GroupPinMessageIQ(
                            groupChat.fullJidIfPossible ?: groupChat.contactJid.jid,
                            ""
                        ),
                        { packet: Stanza? ->
                            if (packet is IQ && packet.type == IQ.Type.result) {
                                LogManager.d(
                                    this::class.java.simpleName,
                                    "Message successfully unpinned"
                                )
                            }
                        }) { exception: Exception? ->
                        LogManager.exception(this::class.java.simpleName, exception)
                        val context = Application.getInstance().applicationContext
                        Application.getInstance().runOnUiThread {
                            Toast.makeText(
                                context,
                                context.getText(R.string.groupchat_failed_to_unpin_message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } catch (e: Exception) {
                LogManager.exception(this::class.java.simpleName, e)
            }
        }
    }

    fun sendPinMessageRequest(message: MessageRealmObject) {
        //todo add privilege checking
        val account = message.account
        val contact = message.user
        val messageId = message.stanzaId
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                val groupChat = ChatManager.getInstance().getChat(account, contact) as GroupChat?
                    ?: return@runInBackgroundNetworkUserRequest
                AccountManager
                    .getAccount(account)?.connection?.sendIqWithResponseCallback(
                        GroupPinMessageIQ(groupChat, messageId),
                        { packet: Stanza? ->
                            if (packet is IQ && packet.type == IQ.Type.result) {
                                LogManager.d(
                                    this::class.java.simpleName,
                                    "Message successfully pinned"
                                )
                            }
                        }) {
                        LogManager.e(this::class.java.simpleName, "Failed to pin message")
                        val context = Application.getInstance().applicationContext
                        Application.getInstance().runOnUiThread {
                            Toast.makeText(
                                context,
                                context.getText(R.string.groupchat_failed_to_pin_message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } catch (e: Exception) {
                LogManager.exception(this::class.java.simpleName, e)
            }
        }
    }

}