package com.xabber.android.data.extension.groups;

import android.content.Context;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.BaseIqResultUiListener;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.database.repositories.AccountRepository;
import com.xabber.android.data.database.repositories.MessageRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.archive.MessageArchiveManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.GroupChat;
import com.xabber.android.data.message.chat.RegularChat;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.OnGroupDefaultRestrictionsListener;
import com.xabber.android.ui.OnGroupPresenceUpdatedListener;
import com.xabber.android.ui.OnGroupSettingsResultsListener;
import com.xabber.android.ui.OnGroupStatusResultListener;
import com.xabber.xmpp.avatar.DataExtension;
import com.xabber.xmpp.avatar.MetadataExtension;
import com.xabber.xmpp.avatar.MetadataInfo;
import com.xabber.xmpp.avatar.UserAvatarManager;
import com.xabber.xmpp.groups.GroupPinMessageIQ;
import com.xabber.xmpp.groups.GroupPresenceExtensionElement;
import com.xabber.xmpp.groups.create.CreateGroupchatIQ;
import com.xabber.xmpp.groups.restrictions.GroupDefaultRestrictionsDataFormResultIQ;
import com.xabber.xmpp.groups.restrictions.RequestGroupDefaultRestrictionsDataFormIQ;
import com.xabber.xmpp.groups.restrictions.RequestToChangeGroupDefaultRestrictionsIQ;
import com.xabber.xmpp.groups.settings.GroupSettingsDataFormResultIQ;
import com.xabber.xmpp.groups.settings.GroupSettingsRequestFormQueryIQ;
import com.xabber.xmpp.groups.settings.SetGroupSettingsRequestIQ;
import com.xabber.xmpp.groups.status.GroupSetStatusRequestIQ;
import com.xabber.xmpp.groups.status.GroupStatusDataFormIQ;
import com.xabber.xmpp.groups.status.GroupStatusFormRequestIQ;
import com.xabber.xmpp.groups.system_message.GroupSystemMessageExtensionElement;
import com.xabber.xmpp.smack.XMPPTCPConnection;
import com.xabber.xmpp.vcard.VCard;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PublishItem;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.xabber.xmpp.avatar.UserAvatarManager.DATA_NAMESPACE;
import static com.xabber.xmpp.avatar.UserAvatarManager.METADATA_NAMESPACE;

public class GroupsManager implements OnPacketListener, OnLoadListener {

    public static final String NAMESPACE = "https://xabber.com/protocol/groups";
    public static final String SYSTEM_MESSAGE_NAMESPACE = NAMESPACE + GroupSystemMessageExtensionElement.HASH_BLOCK;
    private static final String LOG_TAG = GroupsManager.class.getSimpleName();
    private static GroupsManager instance;

    private final Map<AccountJid, List<Jid>> availableGroupServers = new HashMap<>();
    private final Map<AccountJid, List<String>> customGroupServers = new HashMap<>();

    public static GroupsManager getInstance() {
        if (instance == null) instance = new GroupsManager();
        return instance;
    }

    @Override
    public void onLoad() {
        availableGroupServers.clear();
        availableGroupServers.putAll(AccountRepository.getGroupServers());

        customGroupServers.clear();
        customGroupServers.putAll(AccountRepository.getCustomGroupServers());
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {
        if (packet instanceof Presence && packet.hasExtension(GroupPresenceExtensionElement.NAMESPACE)) {
            processPresence(connection, packet);
        } else if (packet instanceof DiscoverItems) processDiscoInfoIq(connection, packet);
    }

    private void processDiscoInfoIq(ConnectionItem connectionItem, Stanza packet) {
        try {
            AccountJid accountJid = connectionItem.getAccount();

            if (availableGroupServers.get(accountJid) == null)
                availableGroupServers.remove(accountJid);

            availableGroupServers.put(accountJid, new ArrayList<>());

            for (DiscoverItems.Item item : ((DiscoverItems) packet).getItems()) {
                if (NAMESPACE.equals(item.getNode())){
                    Jid srvJid = ContactJid.from(item.getEntityID()).getBareJid();
                    availableGroupServers.get(accountJid).add(srvJid);

                }
            }

            AccountRepository.saveOrUpdateGroupServers(accountJid, availableGroupServers.get(accountJid));
            LogManager.d(LOG_TAG, "Got a group server list");
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        }
    }

    private void processPresence(ConnectionItem connection, Stanza packet) {
        try {
            GroupPresenceExtensionElement presence = (GroupPresenceExtensionElement) packet.getExtension(GroupPresenceExtensionElement.NAMESPACE);

            AccountJid accountJid = AccountJid.from(packet.getTo().toString());
            ContactJid contactJid = ContactJid.from(packet.getFrom()).getBareUserJid();

            AbstractChat chat = ChatManager.getInstance().getChat(accountJid, contactJid);

            if (chat == null) chat = ChatManager.getInstance().createGroupChat(accountJid, contactJid);

            if (chat instanceof RegularChat) {
                ChatManager.getInstance().removeChat(chat);
                chat = ChatManager.getInstance().createGroupChat(accountJid, contactJid);
            }

            GroupChat groupChat = (GroupChat) chat;

            if (presence.getPinnedMessageId() != null
                    && !presence.getPinnedMessageId().isEmpty()
                    && !presence.getPinnedMessageId().equals("0")) {
                MessageRealmObject pinnedMessage = MessageRepository
                        .getMessageFromRealmByStanzaId(presence.getPinnedMessageId());
                if (pinnedMessage == null || pinnedMessage.getTimestamp() == null) {

                    MessageArchiveManager.INSTANCE.loadMessageByStanzaId(groupChat, presence.getPinnedMessageId());
                }
                groupChat.setPinnedMessageId(presence.getPinnedMessageId());
            }

            groupChat.setName(presence.getName());
            groupChat.setNumberOfOnlineMembers(presence.getPresentMembers());

            for (OnGroupPresenceUpdatedListener listener :
                    Application.getInstance().getUIListeners(OnGroupPresenceUpdatedListener.class)){
                listener.onGroupPresenceUpdated(contactJid);
            }
            //todo etc...
            ChatManager.getInstance().saveOrUpdateChatDataToRealm(groupChat);
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
        }
    }

    public void processVcard(AccountJid accountJid, ContactJid groupJid, VCard vcard){
        AbstractChat abstractChat = ChatManager.getInstance().getChat(accountJid, groupJid);
        if (!(abstractChat instanceof GroupChat)) return;

        GroupChat groupChat = (GroupChat) abstractChat;
        groupChat.setDescription(vcard.getDescription());
        groupChat.setPrivacyType(vcard.getPrivacyType());
        groupChat.setIndexType(vcard.getIndexType());
        groupChat.setMembershipType(vcard.getMembershipType());
        if (vcard.getNickName() != null && !vcard.getNickName().isEmpty()) groupChat.setName(vcard.getNickName());
        groupChat.setNumberOfMembers(vcard.getMembersCount());
        ChatManager.getInstance().saveOrUpdateChatDataToRealm(groupChat);
    }

    /**
     * Call when account blocked or kicked from group
     * @param accountJid account that got unavailable presence
     * @param contactJid of group that sent unavailable presence
     */
    public void onUnsubscribePresence(AccountJid accountJid, ContactJid contactJid){
        LogManager.d(LOG_TAG, "Subscription state for contact "
                        + contactJid.toString()
                        + RosterManager.getInstance().getSubscriptionState(accountJid, contactJid).getSubscriptionType());
        ChatManager.getInstance().removeChat(accountJid, contactJid);
    }

    public boolean isSupported(XMPPTCPConnection connection) {
        try {
            return ServiceDiscoveryManager.getInstanceFor(connection)
                    .serverSupportsFeature(NAMESPACE);
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
            return false;
        }
    }

    public boolean isSupported(AccountJid accountJid) {
        return isSupported(AccountManager.getInstance().getAccount(accountJid).getConnection());
    }

    public void sendCreateGroupchatRequest(AccountJid accountJid, String server, String groupName, String description,
                                           String localpart, GroupMembershipType membershipType,
                                           GroupIndexType indexType, GroupPrivacyType privacyType,
                                           BaseIqResultUiListener listener) {
        CreateGroupchatIQ iq = new CreateGroupchatIQ(accountJid.getFullJid(),
                server, groupName, localpart, description, membershipType, privacyType, indexType);

        try {
            listener.onSend();
            AccountManager.getInstance().getAccount(accountJid).getConnection()
                    .sendIqWithResponseCallback(iq, packet -> {
                        if (packet instanceof IQ && ((IQ) packet).getType() == IQ.Type.result) {
                            LogManager.d(LOG_TAG, "Groupchat successfully created");

                            if (packet instanceof CreateGroupchatIQ.ResultIq) {
                                try {
                                    ContactJid contactJid = ContactJid.from(((CreateGroupchatIQ.ResultIq) packet).getJid());
                                    AccountJid account = AccountJid.from(packet.getTo().toString());
                                    PresenceManager.getInstance().addAutoAcceptGroupSubscription(account, contactJid);
                                    PresenceManager.getInstance().requestSubscription(account, contactJid, false);

                                    GroupChat createdGroup = ChatManager.getInstance().createGroupChat(account, contactJid);

                                    createdGroup.setName(groupName);
                                    createdGroup.setDescription(description);
                                    createdGroup.setIndexType(indexType);
                                    createdGroup.setMembershipType(membershipType);
                                    createdGroup.setPrivacyType(privacyType);

                                    listener.onResult();
                                } catch (Exception e) {
                                    LogManager.exception(LOG_TAG, e);
                                    listener.onOtherError(e);
                                }
                            }

                        }
                    }, listener);
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
            listener.onOtherError(e);
        }

    }

    public List<Jid> getAvailableGroupchatServersForAccountJid(AccountJid accountJid) {
        return availableGroupServers.get(accountJid);
    }

    public List<String> getCustomGroupServers(AccountJid accountJid){
        return customGroupServers.get(accountJid);
    }

    public void saveCustomGroupServer(AccountJid accountJid, String server){
        if (customGroupServers.get(accountJid) == null){
            customGroupServers.put(accountJid, new ArrayList<>());
        }
        customGroupServers.get(accountJid).add(server);
        AccountRepository.saveCustomGroupServer(accountJid, server);
    }

    public void removeCustomGroupServer(AccountJid accountJid, String string){
        customGroupServers.get(accountJid).remove(string);
        AccountRepository.removeCustomGroupServer(accountJid, string);
    }

    public void requestGroupStatusForm(GroupChat groupchat) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try {
                AccountManager.getInstance().getAccount(groupchat.getAccount()).getConnection()
                        .sendIqWithResponseCallback(new GroupStatusFormRequestIQ(groupchat), packet -> {
                            if (packet instanceof GroupStatusDataFormIQ
                                    && ((GroupStatusDataFormIQ) packet).getType() == IQ.Type.result)
                                for (OnGroupStatusResultListener listener : Application.getInstance().getUIListeners(OnGroupStatusResultListener.class)) {
                                    listener.onStatusDataFormReceived(groupchat, ((GroupStatusDataFormIQ) packet).getDataForm());
                                }
                        }, exception -> {
                            for (OnGroupStatusResultListener listener : Application.getInstance().getUIListeners(OnGroupStatusResultListener.class)) {
                                listener.onError(groupchat);
                            }
                        });

            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
                for (OnGroupStatusResultListener listener : Application.getInstance().getUIListeners(OnGroupStatusResultListener.class)) {
                    listener.onError(groupchat);
                }
            }
        });
    }

    public void sendSetGroupchatStatusRequest(GroupChat groupChat, DataForm dataForm) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try {
                AccountManager.getInstance().getAccount(groupChat.getAccount()).getConnection()
                        .sendIqWithResponseCallback(new GroupSetStatusRequestIQ(groupChat, dataForm), packet -> {
                            if (packet instanceof IQ && ((IQ) packet).getType() == IQ.Type.result)
                                for (OnGroupStatusResultListener listener : Application.getInstance().getUIListeners(OnGroupStatusResultListener.class)) {
                                    listener.onStatusSuccessfullyChanged(groupChat);
                                }
                        }, exception -> {
                            for (OnGroupStatusResultListener listener :
                                    Application.getInstance().getUIListeners(OnGroupStatusResultListener.class)) {
                                listener.onError(groupChat);
                            }
                        });

            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
                for (OnGroupStatusResultListener listener : Application.getInstance().getUIListeners(OnGroupStatusResultListener.class)) {
                    listener.onError(groupChat);
                }
            }
        });
    }

    public void requestGroupDefaultRestrictionsDataForm(GroupChat groupchat){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try {
                AccountManager.getInstance().getAccount(groupchat.getAccount()).getConnection()
                        .sendIqWithResponseCallback(new RequestGroupDefaultRestrictionsDataFormIQ(groupchat), packet -> {
                            if (packet instanceof GroupDefaultRestrictionsDataFormResultIQ
                                    && ((GroupDefaultRestrictionsDataFormResultIQ) packet).getType() == IQ.Type.result)
                                for (OnGroupDefaultRestrictionsListener listener : Application.getInstance().getUIListeners(OnGroupDefaultRestrictionsListener.class)) {
                                    listener.onDataFormReceived(groupchat, ((GroupDefaultRestrictionsDataFormResultIQ) packet).getDataForm());
                                }
                        }, exception -> {
                            for (OnGroupDefaultRestrictionsListener listener :
                                    Application.getInstance().getUIListeners(OnGroupDefaultRestrictionsListener.class)) {
                                listener.onError(groupchat);
                            }
                        });

            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
                for (OnGroupDefaultRestrictionsListener listener : Application.getInstance().getUIListeners(OnGroupDefaultRestrictionsListener.class)) {
                    listener.onError(groupchat);
                }
            }
        });
    }

    public void requestSetGroupDefaultRestrictions(GroupChat groupChat, DataForm dataForm){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try {
                AccountManager.getInstance().getAccount(groupChat.getAccount()).getConnection()
                        .sendIqWithResponseCallback(new RequestToChangeGroupDefaultRestrictionsIQ(groupChat, dataForm), packet -> {
                            if (packet instanceof GroupDefaultRestrictionsDataFormResultIQ
                                    && ((GroupDefaultRestrictionsDataFormResultIQ) packet).getType() == IQ.Type.result)
                                for (OnGroupDefaultRestrictionsListener listener : Application.getInstance().getUIListeners(OnGroupDefaultRestrictionsListener.class)) {
                                    listener.onSuccessful(groupChat);
                                }
                        }, exception -> {
                            for (OnGroupDefaultRestrictionsListener listener : Application.getInstance().getUIListeners(OnGroupDefaultRestrictionsListener.class)) {
                                listener.onError(groupChat);
                            }
                        });

            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
                for (OnGroupDefaultRestrictionsListener listener : Application.getInstance().getUIListeners(OnGroupDefaultRestrictionsListener.class)) {
                    listener.onError(groupChat);
                }
            }
        });
    }

    public void requestGroupSettingsForm(GroupChat groupchat) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try {
                AccountManager.getInstance().getAccount(groupchat.getAccount()).getConnection()
                        .sendIqWithResponseCallback(new GroupSettingsRequestFormQueryIQ(groupchat), packet -> {
                            if (packet instanceof GroupSettingsDataFormResultIQ
                                    && ((GroupSettingsDataFormResultIQ) packet).getType() == IQ.Type.result)
                                for (OnGroupSettingsResultsListener listener : Application.getInstance().getUIListeners(OnGroupSettingsResultsListener.class)) {
                                    listener.onDataFormReceived(groupchat, ((GroupSettingsDataFormResultIQ) packet).getDataFrom());
                                }
                        }, exception -> {
                            for (OnGroupSettingsResultsListener listener :
                                    Application.getInstance().getUIListeners(OnGroupSettingsResultsListener.class)) {
                                listener.onErrorAtDataFormRequesting(groupchat);
                            }
                        });

            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
                for (OnGroupSettingsResultsListener listener : Application.getInstance().getUIListeners(OnGroupSettingsResultsListener.class)) {
                    listener.onErrorAtDataFormRequesting(groupchat);
                }
            }
        });
    }

    public void sendSetGroupSettingsRequest(GroupChat groupchat, DataForm dataForm) {
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try {
                AccountManager.getInstance().getAccount(groupchat.getAccount()).getConnection()
                        .sendIqWithResponseCallback(new SetGroupSettingsRequestIQ(groupchat, dataForm), packet -> {
                            if (packet instanceof IQ && ((IQ) packet).getType() == IQ.Type.result)
                                for (OnGroupSettingsResultsListener listener : Application.getInstance().getUIListeners(OnGroupSettingsResultsListener.class)) {
                                    listener.onGroupSettingsSuccessfullyChanged(groupchat);
                                }
                        }, exception -> {
                            for (OnGroupSettingsResultsListener listener : Application.getInstance().getUIListeners(OnGroupSettingsResultsListener.class)) {
                                listener.onErrorAtSettingsSetting(groupchat);
                            }
                        });

            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
                for (OnGroupSettingsResultsListener listener : Application.getInstance().getUIListeners(OnGroupSettingsResultsListener.class)) {
                    listener.onErrorAtSettingsSetting(groupchat);
                }
            }
        });
    }

    public void sendRemoveGroupAvatarRequest(GroupChat groupChat){
        Application.getInstance().runInBackground(() -> {
            try{
                PayloadItem<MetadataExtension> item = new PayloadItem<>(null, new MetadataExtension(null));
                PublishItem<PayloadItem<MetadataExtension>> publishItem = new PublishItem<>(METADATA_NAMESPACE, item);

                PubSub packet = PubSub.createPubsubPacket(groupChat.getContactJid().getBareJid(),
                        IQ.Type.set, publishItem, null);

                AccountManager.getInstance().getAccount(groupChat.getAccount()).getConnection()
                        .createStanzaCollectorAndSend(packet).nextResultOrThrow(45000);
            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            }
        });
    }

    public void sendPublishGroupAvatar(GroupChat groupChat, String memberId, byte[] data, int height,
                                    int width, UserAvatarManager.ImageType type){
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try{
                XMPPConnection connectionItem = AccountManager.getInstance()
                        .getAccount(groupChat.getAccount()).getConnection();

                String avatarHash = AvatarManager.getAvatarHash(data);

                DataExtension dataExtension = new DataExtension(data);
                PayloadItem<DataExtension> dataItem = new PayloadItem<>(avatarHash, dataExtension);
                PublishItem<PayloadItem<DataExtension>> dataPublishItem = new PublishItem<>(
                        DATA_NAMESPACE + "#" + memberId, dataItem);
                PubSub dataPacket = PubSub.createPubsubPacket(groupChat.getContactJid().getBareJid(),
                        IQ.Type.set, dataPublishItem, null);

                connectionItem.createStanzaCollectorAndSend(dataPacket).nextResultOrThrow(60000);

                MetadataInfo metadataInfo = new MetadataInfo(avatarHash,
                        null, data.length, type.getValue(), height, width);
                MetadataExtension metadataExtension = new MetadataExtension(
                        Collections.singletonList(metadataInfo), null);
                PayloadItem<MetadataExtension> metadataItem = new PayloadItem<>(avatarHash,
                        metadataExtension);
                PublishItem<PayloadItem<MetadataExtension>> metadataPublishItem = new PublishItem<>(
                        METADATA_NAMESPACE + "#" + memberId, metadataItem);
                PubSub metadataPacket = PubSub.createPubsubPacket(
                        groupChat.getContactJid().getBareJid(), IQ.Type.set, metadataPublishItem,
                        null);

                connectionItem.createStanzaCollectorAndSend(metadataPacket).nextResultOrThrow(45000);

            } catch (Exception e){
                LogManager.exception(LOG_TAG, e);
            }
        });
    }

    public void sendUnPinMessageRequest(GroupChat groupChat) {
        //todo add privilege checking
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            try {
                GroupPinMessageIQ iq = new GroupPinMessageIQ(groupChat.getFullJidIfPossible(), "");

                AccountManager.getInstance().getAccount(groupChat.getAccount()).getConnection()
                        .sendIqWithResponseCallback(iq, packet -> {
                            if (packet instanceof IQ && ((IQ) packet).getType().equals(IQ.Type.result))
                                LogManager.d(LOG_TAG, "Message successfully unpinned");
                        }, exception -> {
                            LogManager.exception(LOG_TAG, exception);
                            Context context = Application.getInstance().getApplicationContext();
                            Application.getInstance().runOnUiThread(() -> Toast.makeText(
                                    context,
                                    context.getText(R.string.groupchat_failed_to_unpin_message),
                                    Toast.LENGTH_SHORT).show());
                        });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            }
        });
    }

    public void sendPinMessageRequest(MessageRealmObject message) {
        //todo add privilege checking

        final AccountJid account = message.getAccount();
        final ContactJid contact = message.getUser();
        final String messageId = message.getStanzaId();

        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {

            try {
                FullJid fullJid = ((GroupChat) ChatManager.getInstance().getChat(account, contact)).getFullJidIfPossible();
                GroupPinMessageIQ iq = new GroupPinMessageIQ(fullJid, messageId);

                AccountManager.getInstance().getAccount(account).getConnection()
                        .sendIqWithResponseCallback(iq, packet -> {
                            if (packet instanceof IQ && ((IQ) packet).getType().equals(IQ.Type.result))
                                LogManager.d(LOG_TAG, "Message successfully pinned");
                        }, exception -> {
                            LogManager.d(LOG_TAG, "Failed to pin message");
                            Context context = Application.getInstance().getApplicationContext();
                            Application.getInstance().runOnUiThread(() -> Toast.makeText(context,
                                    context.getText(R.string.groupchat_failed_to_pin_message),
                                    Toast.LENGTH_SHORT).show());
                        });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            }
        });
    }

}
