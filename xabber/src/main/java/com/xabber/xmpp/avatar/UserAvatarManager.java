package com.xabber.xmpp.avatar;

import com.xabber.android.data.Application;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.groups.GroupMember;
import com.xabber.android.data.log.LogManager;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.jidtype.AbstractJidTypeFilter;
import org.jivesoftware.smack.filter.jidtype.FromJidTypeFilter;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.pep.PEPListener;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.EventElementType;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubElementType;
import org.jivesoftware.smackx.pubsub.PubSubFeature;
import org.jivesoftware.smackx.pubsub.PublishItem;
import org.jivesoftware.smackx.pubsub.filter.EventExtensionFilter;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentSkipListSet;


public final class UserAvatarManager extends Manager {

    public static final String DATA_NAMESPACE = "urn:xmpp:avatar:data";
    public static final String METADATA_NAMESPACE = "urn:xmpp:avatar:metadata";
    public static final String FEATURE_METADATA = METADATA_NAMESPACE + "+notify";
    private static final String LOG_TAG = UserAvatarManager.class.getName();

    private static final Map<XMPPConnection, UserAvatarManager> INSTANCES = new WeakHashMap<>();

    private final ServiceDiscoveryManager serviceDiscoveryManager;
    private static final StanzaFilter FROM_BARE_JID_WITH_EVENT_EXTENSION_FILTER = new AndFilter(
            new FromJidTypeFilter(AbstractJidTypeFilter.JidType.BareJid),
            EventExtensionFilter.INSTANCE);

    private final AvatarMetadataStore metadataStore = new AvatarMetadataStore();
    private final Set<String> groupchatMemberAvatarRequests = new ConcurrentSkipListSet<>();


    /**
     * Get the singleton instance of UserAvatarManager.
     *
     * @param connection {@link XMPPConnection}.
     * @return the instance of UserAvatarManager
     */
    public static synchronized UserAvatarManager getInstanceFor(XMPPConnection connection) {
        UserAvatarManager userAvatarManager = INSTANCES.get(connection);

        if (userAvatarManager == null) {
            userAvatarManager = new UserAvatarManager(connection);
            INSTANCES.put(connection, userAvatarManager);
        }

        return userAvatarManager;
    }

    private UserAvatarManager(XMPPConnection connection) {
        super(connection);
        this.serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);

        StanzaListener packetListener = stanza -> {
            Message message = (Message) stanza;
            EventElement event = EventElement.from(stanza);
            assert (event != null);
            EntityBareJid from = message.getFrom().asEntityBareJidIfPossible();
            assert (from != null);
            metadataExtensionListener.eventReceived(from, event, message);
        };
        connection.addAsyncStanzaListener(packetListener, FROM_BARE_JID_WITH_EVENT_EXTENSION_FILTER);
    }

    /**
     * Returns true if User Avatar publishing is supported by the server.
     * In order to support User Avatars the server must have support for XEP-0163: Personal Eventing Protocol (PEP).
     *
     * @return true if User Avatar is supported by the server.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0163.html">XEP-0163: Personal Eventing Protocol</a>
     */
    public boolean isSupportedByServer()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        BareJid localBareJid = connection().getUser().asBareJid();
        return serviceDiscoveryManager.supportsFeatures(localBareJid, REQUIRED_FEATURES);
    }

    /**
     * XEP-163 5.
     */
    private static final PubSubFeature[] REQUIRED_FEATURES = new PubSubFeature[] {
            // @formatter:off
            PubSubFeature.auto_create,
            PubSubFeature.auto_subscribe,
            PubSubFeature.filtered_notifications,
            // @formatter:on
    };

    /**
     * Announce support for User Avatars and start receiving avatar updates.
     */
    public void enable() {
        serviceDiscoveryManager.addFeature(FEATURE_METADATA);
        LogManager.d(LOG_TAG, "added +notify feature && avatar pep listener");
    }

    /**
     * Stop receiving avatar updates.
     */
    public void disable() {
        serviceDiscoveryManager.removeFeature(FEATURE_METADATA);
    }

    /**
     * Publish a PNG Avatar and its metadata to PubSub.
     */
    public void publishAvatar(byte[] data, int height, int width) throws XMPPErrorException, NotConnectedException,
            InterruptedException, NoResponseException {

        String id = publishAvatarData(data);
        publishAvatarMetadata(id, data.length, ImageType.PNG.getValue(), height, width);
    }

    /**
     * Publish a JPG Avatar and its metadata to PubSub.
     */
    public void publishAvatarJPG(byte[] data, int height, int width) throws XMPPErrorException, NotConnectedException,
            InterruptedException, NoResponseException {

        String id = publishAvatarData(data);
        publishAvatarMetadata(id, data.length, ImageType.JPEG.getValue(), height, width);
    }

    public byte[] fetchAvatarFromPubSub(EntityBareJid from, MetadataInfo metadataInfo)
            throws InterruptedException, NoResponseException, NotConnectedException, XMPPErrorException {

        ItemsExtension items = new ItemsExtension(ItemsExtension.ItemsElementType.items, DATA_NAMESPACE,
                Collections.singletonList(new Item(metadataInfo.getId())));

        PubSub avatarRequest = PubSub.createPubsubPacket(from, IQ.Type.get, items, null);
        PubSub reply = connection().createStanzaCollectorAndSend(avatarRequest).nextResultOrThrow(120000);
        ItemsExtension receivedItems = reply.getExtension(PubSubElementType.ITEMS);
        for (ExtensionElement itm : (receivedItems).getExtensions()) {
            if (!(itm instanceof PayloadItem<?>)) {
                continue;
            }
            PayloadItem<?> payloadItem = (PayloadItem<?>) itm;
            if ((payloadItem.getPayload() instanceof DataExtension)) {

                DataExtension data = (DataExtension) payloadItem.getPayload();
                return data.getData();
            }
        }

        return null;
    }

    public byte[] fetchAvatarFromUrl(URL url, int length) {

        try {
            InputStream is = url.openConnection().getInputStream();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[length];
            int read;

            while ((read = is.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, read);
            }

            baos.flush();

            return  baos.toByteArray();

        } catch (Exception e) {
            LogManager.exception(LOG_TAG + " Url: " + url.toString(), e);
        }
        return null;
    }

    public void requestAvatarOfGroupchatMember(GroupMember groupMember) {
        if (groupMember == null
                || groupMember.getGroupJid() == null
                || groupMember.getAvatarHash() == null) {
            return;
        }
        if (groupchatMemberAvatarRequests.contains(groupMember.getAvatarHash())) {
            return;
        }
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> {
            String avatarHash = groupMember.getAvatarHash();
            if (groupchatMemberAvatarRequests.contains(avatarHash)) {
                return;
            }
            groupchatMemberAvatarRequests.add(avatarHash);

            ItemsExtension itemsExtension =
                    new ItemsExtension(ItemsExtension.ItemsElementType.items,
                            (DATA_NAMESPACE + "#" + groupMember.getId()),
                            Collections.singletonList(new Item(avatarHash)));
            BareJid groupchatJid = null;
            try {
                groupchatJid = JidCreate.bareFrom(groupMember.getGroupJid());
            } catch (XmppStringprepException e) {
                LogManager.exception(LOG_TAG, e);
                groupchatMemberAvatarRequests.remove(avatarHash);
            }
            if (groupchatJid == null) return;
            PubSub avatarRequest = PubSub.createPubsubPacket(groupchatJid, IQ.Type.get, itemsExtension, null);
            PubSub reply;
            try {
                 reply = connection().createStanzaCollectorAndSend(avatarRequest).nextResultOrThrow();
            } catch (NoResponseException
                    | XMPPErrorException
                    | InterruptedException
                    | NotConnectedException e) {
                LogManager.exception(LOG_TAG, e);
                groupchatMemberAvatarRequests.remove(avatarHash);
                return;
            }

            ItemsExtension receivedItems = reply.getExtension(PubSubElementType.ITEMS);
            for (ExtensionElement itm : (receivedItems).getExtensions()) {
                if (!(itm instanceof PayloadItem<?>)) {
                    continue;
                }
                PayloadItem<?> payloadItem = (PayloadItem<?>) itm;
                if ((payloadItem.getPayload() instanceof DataExtension)) {
                    DataExtension data = (DataExtension) payloadItem.getPayload();
                    byte[] avatarData = data.getData();
                    if (avatarData == null) {
                        groupchatMemberAvatarRequests.remove(avatarHash);
                        return;
                    }
                    AvatarManager.getInstance().onGroupchatMemberAvatarReceived(avatarHash, avatarData);
                    groupchatMemberAvatarRequests.remove(avatarHash);
                }
            }
        });
    }

    public String publishAvatarData(byte[] data) throws NoResponseException, NotConnectedException,
            XMPPErrorException, InterruptedException{

        String itemId = AvatarManager.getAvatarHash(data);
        publishAvatarData(data, itemId);
        return itemId;
    }


    private void publishAvatarData(byte[] data, String itemId)
            throws NoResponseException, NotConnectedException, XMPPErrorException, InterruptedException {

        DataExtension dataExtension = new DataExtension(data);
        PayloadItem<DataExtension> item = new PayloadItem<>(itemId, dataExtension);
        PublishItem<PayloadItem<DataExtension>> publishItem = new PublishItem<>(DATA_NAMESPACE, item);
        PubSub packet = PubSub.createPubsubPacket(null, IQ.Type.set, publishItem, null);
        connection().createStanzaCollectorAndSend(packet).nextResultOrThrow(60000);
    }

    /**
     * Publish metadata about an avatar to the metadata node.
     *
     * @param itemId SHA-1 sum of the image of type image/png
     * @param info info element containing metadata of the file
     * @param pointers list of metadata pointer elements
     */
    public void publishAvatarMetadata(String itemId, MetadataInfo info, List<MetadataPointer> pointers)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {

        publishAvatarMetadata(itemId, Collections.singletonList(info), pointers);
    }

    /**
     * Publish avatar metadata.
     *
     * @param itemId SHA-1 sum of the avatar image representation of type image/png
     * @param infos list of metadata elements
     * @param pointers list of pointer elements
     */
    public void publishAvatarMetadata(String itemId, List<MetadataInfo> infos, List<MetadataPointer> pointers)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {

        MetadataExtension metadataExtension = new MetadataExtension(infos, pointers);

        PayloadItem<MetadataExtension> item = new PayloadItem<>(itemId, metadataExtension);
        PublishItem<PayloadItem<MetadataExtension>> publishItem = new PublishItem<>(METADATA_NAMESPACE, item);
        PubSub packet = PubSub.createPubsubPacket(null, IQ.Type.set, publishItem, null);
        connection().createStanzaCollectorAndSend(packet).nextResultOrThrow(45000);

        // Mark our own avatar as locally available so that we don't get updates for it
        metadataStore.setAvatarAvailable(connection().getUser().asEntityBareJidOrThrow(), itemId);
    }

    /**
     * Publish avatar metadata with its size in pixels.
     */
    public void publishAvatarMetadata(String itemId, long bytes, String type, int pixelsHeight, int pixelsWidth)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException{

        MetadataInfo info = new MetadataInfo(itemId, null, bytes, type, pixelsHeight, pixelsWidth);
        publishAvatarMetadata(itemId, info, null);
    }

    /**
     * Publish an empty metadata element to disable avatar publishing.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0084.html#proto-meta">§4.2 Metadata Element</a>
     */
    public void unpublishAvatar() throws NoResponseException, XMPPErrorException, NotConnectedException,
            InterruptedException {

        PayloadItem<MetadataExtension> item = new PayloadItem<>(null, new MetadataExtension(null));
        PublishItem<PayloadItem<MetadataExtension>> publishItem = new PublishItem<>(METADATA_NAMESPACE, item);
        PubSub packet = PubSub.createPubsubPacket(null, IQ.Type.set, publishItem, null);
        connection().createStanzaCollectorAndSend(packet).nextResultOrThrow(45000);
    }


    private final PEPListener metadataExtensionListener = (from, event, message) -> {
        if (!EventElementType.items.equals(event.getEventType())) {
            // Totally not of interest for us.
            return;
        }

        for (ExtensionElement items : event.getExtensions()) {
            if (!(items instanceof ItemsExtension)) {
                continue;
            }

            for (ExtensionElement item : ((ItemsExtension) items).getExtensions()) {
                if (!(item instanceof PayloadItem<?>)) {
                    continue;
                }

                PayloadItem<?> payloadItem = (PayloadItem<?>) item;

                if ((payloadItem.getPayload() instanceof MetadataExtension)) {

                    MetadataExtension metadataExtension = (MetadataExtension) payloadItem.getPayload();
                    if (metadataExtension.getInfoElements() == null) {
                        //save as empty bitmap
                        AvatarManager.getInstance().onAvatarReceived(from, "", null, "xep");

                    } else for (MetadataInfo info : metadataExtension.getInfoElements()) {
                        if (metadataStore.hasAvatarAvailable(from, info.getId())) {
                            AvatarManager am = AvatarManager.getInstance();
                            // If we have a locally saved copy of the avatar, check if its hash
                            // matches the hash of the current PEP-avatar(XEP-0084)
                            // and if not, set it as the current one.
                            if (am.getCurrentXEPHash(from) != null) {
                                if (am.getCurrentXEPHash(from).equals(info.getId()))
                                    continue;
                                am.setXEPHashAsCurrent(from, info.getId());
                            }
                        }
                        try {
                            byte[] avatar = info.getUrl() != null ?
                                    fetchAvatarFromUrl(info.getUrl(), info.getBytes()) : fetchAvatarFromPubSub(from, info);

                            if (avatar == null) continue;
                            String sh1 = info.getId();
                            metadataStore.setAvatarAvailable(from, info.getId());
                            AvatarManager.getInstance().onAvatarReceived(from, sh1, avatar, "xep");
                        } catch (Exception e) {
                            LogManager.exception(LOG_TAG, e);
                        }
                    }
                }
            }
        }
    };

    public enum ImageType{
        PNG,
        JPEG;

        public String getValue(){
            switch (this){
                case PNG: return "image/png";
                case JPEG: return "image/jpeg";
                default: return "image";
            }
        }

    }

}