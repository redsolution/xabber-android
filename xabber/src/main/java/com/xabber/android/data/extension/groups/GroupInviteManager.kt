package com.xabber.android.data.extension.groups

import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.BaseIqResultUiListener
import com.xabber.android.data.OnLoadListener
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.connection.StanzaSender
import com.xabber.android.data.database.realmobjects.GroupInviteRealmObject
import com.xabber.android.data.database.repositories.GroupInviteRepository
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.blocking.BlockingManager
import com.xabber.android.data.extension.blocking.BlockingManager.BlockContactListener
import com.xabber.android.data.extension.vcard.VCardManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.data.message.chat.RegularChat
import com.xabber.android.data.roster.PresenceManager
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.*
import com.xabber.xmpp.avatar.UserAvatarManager
import com.xabber.xmpp.groups.invite.incoming.DeclineGroupInviteIQ
import com.xabber.xmpp.groups.invite.incoming.IncomingInviteExtensionElement
import com.xabber.xmpp.groups.invite.outgoing.*
import org.jivesoftware.smack.ExceptionCallback
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.packet.XMPPError
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

object GroupInviteManager : OnLoadListener {

    private val LOG_TAG = GroupInviteManager.javaClass.simpleName

    private val invitesList = mutableListOf<GroupInviteRealmObject>()

    override fun onLoad() {
        GroupInviteRepository.getAllInvitationsFromRealm().forEach { invitesList.add(it) }
    }

    fun processIncomingInvite(
        inviteExtensionElement: IncomingInviteExtensionElement, account: AccountJid,
        sender: ContactJid?, timestamp: Long
    ) {
        try {
            val groupContactJid = ContactJid.from(inviteExtensionElement.groupJid)
            if (BlockingManager.getInstance().contactIsBlocked(account, groupContactJid)
                || RosterManager.getInstance().accountIsSubscribedTo(account, groupContactJid)
            ) {
                LogManager.i(LOG_TAG, "Got incoming invite, but group is already in blocklist")
                return
            }
            val inviteReason = inviteExtensionElement.getReason()
            if (invitesList.none { it.accountJid == account && it.groupJid == groupContactJid && it.senderJid == sender }) {
                val giro = GroupInviteRealmObject(account, groupContactJid, sender).apply {
                    isIncoming = true
                    reason = inviteReason
                    date = if (timestamp != 0.toLong()) timestamp else System.currentTimeMillis()
                }
                invitesList.add(giro)
                GroupInviteRepository.saveOrUpdateInviteToRealm(giro)
                if (ChatManager.getInstance().getChat(account, groupContactJid) is RegularChat) {
                    ChatManager.getInstance().removeChat(account, groupContactJid)
                }
                ChatManager.getInstance().createGroupChat(account, groupContactJid)
                VCardManager.getInstance().requestByUser(account, groupContactJid.jid)
                UserAvatarManager.getInstanceFor(AccountManager.getInstance().getAccount(account)?.connection)
                    .requestContactPubsubAvatar(groupContactJid)
            }
        } catch (e: Exception) {
            LogManager.exception(LOG_TAG, e)
        }

        notifySamUiListeners(OnNewMessageListener::class.java)
        notifySamUiListeners(OnMessageUpdatedListener::class.java)
    }

    fun onContactAddedToRoster(accountJid: AccountJid, contactJid: ContactJid) {
        if (hasActiveIncomingInvites(accountJid, contactJid)) {
            getInvites(accountJid, contactJid).forEach {
                it.isAccepted = true
                GroupInviteRepository.saveOrUpdateInviteToRealm(it)
            }
        }
    }

    fun onConversationDeleted(accountJid: AccountJid, groupJid: ContactJid) {
        if (hasActiveIncomingInvites(accountJid, groupJid)) {
            GroupInviteRepository.removeInviteFromRealm(accountJid, groupJid)
            invitesList.removeAll { it.accountJid == accountJid && it.groupJid == groupJid }
            ChatManager.getInstance().removeChat(accountJid, groupJid)
        }
    }

    fun acceptInvitation(accountJid: AccountJid, groupJid: ContactJid) {
        try {
            val name = (ChatManager.getInstance().getChat(accountJid, groupJid) as GroupChat?)!!.name
            PresenceManager.addAutoAcceptGroupSubscription(accountJid, groupJid)
            PresenceManager.acceptSubscription(accountJid, groupJid)
            PresenceManager.requestSubscription(accountJid, groupJid)
            RosterManager.getInstance().createContact(accountJid, groupJid, name, ArrayList())
            invitesList
                .filter { it.accountJid == accountJid && it.groupJid == groupJid }
                .forEach {
                    it.isAccepted = true
                    GroupInviteRepository.saveOrUpdateInviteToRealm(it)
                }
        } catch (e: java.lang.Exception) {
            LogManager.exception(LOG_TAG, e)
        }

        notifySamUiListeners(OnNewMessageListener::class.java)
        notifySamUiListeners(OnMessageUpdatedListener::class.java)
    }

    fun declineInvitation(accountJid: AccountJid, groupJid: ContactJid) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                val groupChat = ChatManager.getInstance().getChat(accountJid, groupJid) as GroupChat?
                val connection = AccountManager.getInstance().getAccount(accountJid)!!.connection
                connection.sendIqWithResponseCallback(DeclineGroupInviteIQ(groupChat!!),
                    { packet: Stanza? ->
                        if (packet is IQ && packet.type == IQ.Type.result) {
                            invitesList
                                .filter { it.accountJid == accountJid && it.groupJid == groupJid }
                                .forEach {
                                    it.isDeclined = true
                                    GroupInviteRepository.saveOrUpdateInviteToRealm(it)
                                }
                            ChatManager.getInstance().removeChat(groupChat)
                            BlockingManager.getInstance().blockContact(accountJid, groupJid,
                                object : BlockContactListener {
                                    override fun onSuccessBlock() {}
                                    override fun onErrorBlock() {}
                                })
                        }
                    },
                    { exception: java.lang.Exception ->
                        LogManager.e(
                            LOG_TAG, "Error to decline the invite from group $groupJid " +
                                    "to account $accountJid!${exception.message}"
                        )
                    }
                )
            } catch (e: java.lang.Exception) {
                LogManager.e(
                    LOG_TAG, "Error to decline the invite from group $groupJid " +
                            "to account $accountJid!${e.message}"
                )
            }
        }

        notifySamUiListeners(OnNewMessageListener::class.java)
        notifySamUiListeners(OnMessageUpdatedListener::class.java)
    }

    fun hasActiveIncomingInvites(accountJid: AccountJid, groupchatJid: ContactJid) =
        invitesList.any { it.accountJid == accountJid && it.groupJid == groupchatJid && !it.isDeclined && !it.isAccepted }
                && !BlockingManager.getInstance().contactIsBlocked(accountJid, groupchatJid)

    fun getLastInvite(accountJid: AccountJid, groupJid: ContactJid) =
        invitesList.lastOrNull { it.accountJid == accountJid && it.groupJid == groupJid }

    fun getInvites(accountJid: AccountJid, groupJid: ContactJid) =
        invitesList.filter { it.accountJid == accountJid && it.groupJid == groupJid }

    /**
     * Create and send IQ to group and Message with invitation to contact according to Direct Invitation.
     */
    fun sendGroupInvitations(
        account: AccountJid, groupJid: ContactJid, contactsToInvite: List<ContactJid>,
        reason: String?, listener: BaseIqResultUiListener
    ) {
        try {
            Application.getInstance().runInBackgroundNetworkUserRequest {
                val chat = ChatManager.getInstance().getChat(account, groupJid)
                val accountItem = AccountManager.getInstance().getAccount(account)
                if (chat is GroupChat && accountItem != null) {
                    listener.onSend()
                    contactsToInvite.forEach { contact ->
                        accountItem.connection.sendIqWithResponseCallback(
                            GroupInviteRequestIQ(chat as GroupChat?, contact, false),
                            { sendMessageWithInvite(account, groupJid, contact, reason, listener) })
                        { exception: java.lang.Exception? ->
                            run {
                                LogManager.exception(LOG_TAG, exception)
                                if (exception is XMPPException.XMPPErrorException) {
                                    listener.onIqError(exception.xmppError)
                                } else listener.onOtherError(exception)
                            }
                        }
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            LogManager.exception(LOG_TAG, e)
            listener.onOtherError(e)
        }
    }

    /**
     * Sends a message with invite to group as direct invitation
     * Must be called only from @see #sendGroupInvitations
     */
    private fun sendMessageWithInvite(
        account: AccountJid, groupJid: ContactJid, contactToInviteJid: ContactJid,
        reason: String?, listener: BaseIqResultUiListener
    ) {
        val errors = mutableListOf<XMPPError>()
        var exception: java.lang.Exception? = null

        try {
            StanzaSender.sendStanza(
                account,
                Message().apply {
                    addBody(
                        null, Application.getInstance().applicationContext
                            .getString(R.string.groupchat_legacy_invitation_body, groupJid.toString())
                    )
                    to = contactToInviteJid.jid
                    type = Message.Type.chat
                    addExtension(InviteMessageExtensionElement(groupJid, reason))
                })
            { packet1: Stanza ->
                run {
                    if (packet1.error != null) errors.add(packet1.error)
                }
            }
        } catch (e: java.lang.Exception) {
            LogManager.exception(LOG_TAG, e)
            exception = e
        }

        when {
            errors.size == 1 -> listener.onIqError(errors[0])
            errors.size > 1 -> listener.onIqErrors(errors)
            exception != null -> listener.onOtherError(exception)
            else -> listener.onResult()
        }

    }

    fun requestGroupInvitationsList(
        account: AccountJid, groupchatJid: ContactJid, listener: StanzaListener,
        exceptionCallback: ExceptionCallback?
    ) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            val chat = ChatManager.getInstance().getChat(account, groupchatJid)
            val accountItem = AccountManager.getInstance().getAccount(account)
            if (chat is GroupChat && accountItem != null) {
                val queryIQ = GroupchatInviteListQueryIQ(chat as GroupChat?)
                try {
                    accountItem.connection.sendIqWithResponseCallback(
                        queryIQ,
                        { packet: Stanza ->
                            if (packet is GroupchatInviteListResultIQ
                                && groupchatJid.bareJid.equals(packet.getFrom().asBareJid())
                                && account.bareJid.equals(packet.getTo().asBareJid())
                            ) {
                                chat.listOfInvites = packet.listOfInvitedJids ?: ArrayList()
                            }
                            listener.processStanza(packet)
                        },
                        exceptionCallback
                    )
                } catch (e: Exception) {
                    LogManager.exception(LOG_TAG, e)
                }
            }
        }
    }

    fun revokeGroupchatInvitation(account: AccountJid, groupchatJid: ContactJid, inviteJid: String) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            try {
                val groupChat = ChatManager.getInstance().getChat(account, groupchatJid) as? GroupChat?

                val stanzaResultListener: (packet: Stanza) -> Unit = { packet ->
                    if (packet is IQ && packet.type == IQ.Type.result) {
                        groupChat?.listOfInvites?.remove(inviteJid)
                        Application.getInstance()
                            .getUIListeners(OnGroupSelectorListToolbarActionResultListener::class.java)
                            .forEachOnUi { it.onActionSuccess(account, groupchatJid, listOf(inviteJid)) }
                    } else {
                        Application.getInstance()
                            .getUIListeners(OnGroupSelectorListToolbarActionResultListener::class.java)
                            .forEachOnUi { it.onActionFailure(account, groupchatJid, listOf(inviteJid)) }
                    }
                }

                val stanzaErrorListener: (exception: java.lang.Exception) -> Unit = {
                    Application.getInstance()
                        .getUIListeners(OnGroupSelectorListToolbarActionResultListener::class.java)
                        .forEachOnUi { it.onActionFailure(account, groupchatJid, listOf(inviteJid)) }
                }

                AccountManager.getInstance().getAccount(account)?.connection?.sendIqWithResponseCallback(
                    GroupchatInviteListRevokeIQ(groupChat, inviteJid), stanzaResultListener, stanzaErrorListener
                )

            } catch (e: java.lang.Exception) {
                LogManager.exception(LOG_TAG, e)
            }
        }
    }

    fun revokeGroupchatInvitations(account: AccountJid, groupchatJid: ContactJid, inviteJids: Set<String>) {
        Application.getInstance().runInBackgroundNetworkUserRequest {
            val failedRevokeRequests = ArrayList<String>()
            val successfulRevokeRequests = ArrayList<String>()
            val unfinishedRequestCount = AtomicInteger(inviteJids.size)
            val chat = ChatManager.getInstance().getChat(account, groupchatJid)
            val groupChat = if (chat is GroupChat) chat else null

            inviteJids.forEach { inviteJid ->

                val stanzaResultListener: (packet: Stanza?) -> Unit = { packet ->
                    if (packet is IQ) {
                        groupChat?.listOfInvites?.remove(inviteJid)
                        successfulRevokeRequests.add(inviteJid)
                        unfinishedRequestCount.getAndDecrement()
                        if (unfinishedRequestCount.get() == 0) {
                            Application.getInstance()
                                .getUIListeners(OnGroupSelectorListToolbarActionResultListener::class.java)
                                .forEachOnUi { listener ->
                                    when {
                                        failedRevokeRequests.size == 0 ->
                                            listener.onActionSuccess(account, groupchatJid, successfulRevokeRequests)

                                        successfulRevokeRequests.size > 0 ->
                                            listener.onPartialSuccess(
                                                account, groupchatJid,
                                                successfulRevokeRequests, failedRevokeRequests
                                            )

                                        else -> listener.onActionFailure(account, groupchatJid, failedRevokeRequests)
                                    }
                                }
                        }
                    }
                }

                val stanzaErrorListener: (exception: java.lang.Exception) -> Unit = {
                    failedRevokeRequests.add(inviteJid)
                    unfinishedRequestCount.getAndDecrement()
                    if (unfinishedRequestCount.get() == 0) {
                        Application.getInstance()
                            .getUIListeners(OnGroupSelectorListToolbarActionResultListener::class.java)
                            .forEachOnUi { listener ->
                                if (successfulRevokeRequests.isNotEmpty()) {
                                    listener.onPartialSuccess(
                                        account, groupchatJid, successfulRevokeRequests,
                                        failedRevokeRequests
                                    )
                                } else listener.onActionFailure(account, groupchatJid, failedRevokeRequests)
                            }
                    }
                }

                try {
                    AccountManager.getInstance().getAccount(account)?.connection?.sendIqWithResponseCallback(
                        GroupchatInviteListRevokeIQ(groupChat, inviteJid), stanzaResultListener, stanzaErrorListener
                    )
                } catch (e: java.lang.Exception) {
                    LogManager.exception(LOG_TAG, e)
                    failedRevokeRequests.add(inviteJid)
                    unfinishedRequestCount.getAndDecrement()
                    Application.getInstance()
                        .getUIListeners(OnGroupSelectorListToolbarActionResultListener::class.java)
                        .forEachOnUi { listener ->
                            listener.onPartialSuccess(
                                account, groupchatJid, successfulRevokeRequests,
                                failedRevokeRequests
                            )
                        }
                }
            }
        }
    }

}