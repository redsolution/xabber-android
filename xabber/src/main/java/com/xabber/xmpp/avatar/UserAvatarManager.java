package com.xabber.xmpp.avatar;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.xabber.android.data.extension.avatar.AvatarManager;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.pep.PEPListener;
import org.jivesoftware.smackx.pep.PEPManager;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.EventElementType;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jxmpp.jid.EntityBareJid;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;


public final class UserAvatarManager extends Manager {

    public static final String DATA_NAMESPACE = "urn:xmpp:avatar:data";
    public static final String METADATA_NAMESPACE = "urn:xmpp:avatar:metadata";
    public static final String FEATURE_METADATA = METADATA_NAMESPACE + "+notify";

    private static final Map<XMPPConnection, UserAvatarManager> INSTANCES = new WeakHashMap<>();

    private final PEPManager pepManager;
    private final ServiceDiscoveryManager serviceDiscoveryManager;

    private AvatarMetadataStore metadataStore = new AvatarMetadataStore();
    private final Set<AvatarListener> avatarListeners = new HashSet<>();


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
        this.pepManager = PEPManager.getInstanceFor(connection);
        this.serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
        //connection.setReplyTimeout(120000);
    }

    /**
     * Returns true if User Avatar publishing is supported by the server.
     * In order to support User Avatars the server must have support for XEP-0163: Personal Eventing Protocol (PEP).
     *
     * @return true if User Avatar is supported by the server.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0163.html">XEP-0163: Personal Eventing Protocol</a>
     *
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public boolean isSupportedByServer()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return pepManager.isSupported();
    }

    /**
     * Announce support for User Avatars and start receiving avatar updates.
     */
    public void enable() {
        pepManager.addPEPListener(metadataExtensionListener);
        serviceDiscoveryManager.addFeature(FEATURE_METADATA);
    }

    /**
     * Stop receiving avatar updates.
     */
    public void disable() {
        serviceDiscoveryManager.removeFeature(FEATURE_METADATA);
        pepManager.addPEPListener(metadataExtensionListener);
    }

    public void refreshCaps() {
        disableNotification();
        enableNotification();
    }

    public void enableNotification() {serviceDiscoveryManager.addFeature(FEATURE_METADATA);}

    public void disableNotification() {serviceDiscoveryManager.removeFeature(FEATURE_METADATA);}

    /**
     * Set an {@link AvatarMetadataStore} which is used to store information about the local availability of avatar
     * data.
     * @param metadataStore metadata store
     */
    public void setAvatarMetadataStore(AvatarMetadataStore metadataStore) {
        this.metadataStore = metadataStore;
    }

    /**
     * Register an {@link AvatarListener} in order to be notified about incoming avatar metadata updates.
     *
     * @param listener listener
     * @return true if the set of listeners did not already contain the listener
     */
    public synchronized boolean addAvatarListener(AvatarListener listener) {
        return avatarListeners.add(listener);
    }

    /**
     * Unregister an {@link AvatarListener} to stop being notified about incoming avatar metadata updates.
     *
     * @param listener listener
     * @return true if the set of listeners contained the listener
     */
    public synchronized boolean removeAvatarListener(AvatarListener listener) {
        return avatarListeners.remove(listener);
    }

    /**
     * Get the data node.
     * This node contains the avatar image data.
     *
     * @return the data node
     * @throws NoResponseException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws XMPPErrorException
     */
    private LeafNode getOrCreateDataNode()
            throws NoResponseException, NotConnectedException, InterruptedException, XMPPErrorException, PubSubException.NotALeafNodeException {
        return PubSubManager.getInstance(connection(),null).getOrCreateLeafNode(DATA_NAMESPACE);
    }

    /**
     * Get the metadata node.
     * This node contains lightweight metadata information about the data in the data node.
     *
     * @return the metadata node
     * @throws NoResponseException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws XMPPErrorException
     */
    private LeafNode getOrCreateMetadataNode()
            throws NoResponseException, NotConnectedException, InterruptedException, XMPPErrorException, PubSubException.NotALeafNodeException {
        return PubSubManager.getInstance(connection(), null).getOrCreateLeafNode(METADATA_NAMESPACE);
    }

    /**
     * Publish a PNG Avatar and its metadata to PubSub.
     *
     * @param data
     * @param height
     * @param width
     * @throws XMPPErrorException
     * @throws PubSubException.NotALeafNodeException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NoResponseException
     */
    public void publishAvatar(byte[] data, int height, int width)
            throws XMPPErrorException, PubSubException.NotALeafNodeException, NotConnectedException,
            InterruptedException, NoResponseException {
        String id = publishAvatarData(data);
        publishAvatarMetadata(id, data.length, "image/png", height, width);
    }

    /**
     * Publish a JPG Avatar and its metadata to PubSub.
     *
     * @param data
     * @param height
     * @param width
     * @throws XMPPErrorException
     * @throws PubSubException.NotALeafNodeException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NoResponseException
     */
    public void publishAvatarJPG(byte[] data, int height, int width)
            throws XMPPErrorException, PubSubException.NotALeafNodeException, NotConnectedException,
            InterruptedException, NoResponseException {
        String id = publishAvatarData(data);
        publishAvatarMetadata(id, data.length, "image/jpeg", height, width);
    }

    /**
     * Publish a PNG avatar and its metadata to PubSub.
     *
     * @param pngFile PNG File
     * @param height height of the image
     * @param width width of the image
     *
     * @throws IOException
     * @throws XMPPErrorException
     * @throws PubSubException.NotALeafNodeException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NoResponseException
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void publishAvatar(File pngFile, int height, int width)
            throws IOException, XMPPErrorException, PubSubException.NotALeafNodeException, NotConnectedException,
            InterruptedException, NoResponseException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream((int) pngFile.length());
             InputStream in = new BufferedInputStream(new FileInputStream(pngFile))) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            byte[] bytes = out.toByteArray();
            publishAvatar(bytes, height, width);
        }
    }

    public byte[] fetchAvatarFromPubSub(EntityBareJid from, MetadataInfo metadataInfo)
            throws InterruptedException, PubSubException.NotALeafNodeException, NoResponseException,
            NotConnectedException, XMPPErrorException {
        //LeafNode dataNode = PubSubManager.getInstance(connection(), null)
        //        .getLeafNode(DATA_NAMESPACE);


        LeafNode dataNode = PubSubManager.getInstance(connection(), from)
                .getLeafNode(DATA_NAMESPACE);
        List<String> id = new ArrayList<>();
        id.add(metadataInfo.getId());
        //List<PayloadItem<DataExtension>> dataItems = dataNode.getItems(1, metadataInfo.getId());
        //List<PayloadItem<DataExtension>> dataItems = dataNode.getItems(-1, metadataInfo.getId());
        List<PayloadItem<DataExtension>> dataItems = dataNode.getItems(id);

        DataExtension extension = dataItems.get(0).getPayload();
        if (metadataStore != null) {
            metadataStore.setAvatarAvailable(from, metadataInfo.getId());
        }
        return extension.getData();
    }

    /*public void discoverAvatar(final BareJid contact, final AccountItem accountItem) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {

                try {
                    //LeafNode data = PubSubManager.getInstance(accountItem.getConnection(), contact).getLeafNode(METADATA_NAMESPACE);
                    //final List<PayloadItem<MetadataExtension>> e = data.getItems();

                    //DiscoverInfo info = new DiscoverInfo();
                    //info.setTo(contact);

                    DiscoverItems items = new DiscoverItems();
                    items.setTo(contact);

                    //DiscoverInfo infoReply = connection().createStanzaCollectorAndSend(info).nextResultOrThrow();
                    DiscoverItems itemsReply = connection().createStanzaCollectorAndSend(items).nextResultOrThrow();
                    List<DiscoverItems.Item> list = itemsReply.getItems();
                    *//*for (DiscoverItems.Item item : list){
                        if(item.getNode().equals(METADATA_NAMESPACE)){
                            LeafNode metadata = PubSubManager.getInstance(accountItem.getConnection(), contact).getLeafNode(METADATA_NAMESPACE);
                            metadata.subscribe(accountItem.getRealJid().toString());

                            SubscribeExtension subEx = new SubscribeExtension(accountItem.getRealJid().asBareJid().toString(), METADATA_NAMESPACE);
                            PubSub ps = PubSub.createPubsubPacket(contact, IQ.Type.set, subEx, null);
                            PubSub reply = (PubSub) connection().createStanzaCollectorAndSend(ps).nextResultOrThrow();
                            Subscription sss = reply.getExtension(PubSubElementType.SUBSCRIPTION);

                            *//**//*LeafNode sutest = PubSubManager.getInstance(accountItem.getConnection(), contact).getLeafNode(METADATA_NAMESPACE);
                            sutest.unsubscribe(accountItem.getRealJid().asBareJid().toString());
                            List<Subscription> subsC = new ArrayList<>();
                            Subscription ssTest = sutest.subscribe(accountItem.getRealJid().asBareJid().toString());
                            Subscription.State statetest = ssTest.getState();*//**//*
                        }
                    }*//*
                    List<Subscription> sub = new ArrayList<>();
                    sub = PubSubManager.getInstance(accountItem.getConnection(), contact).getLeafNode(METADATA_NAMESPACE).getSubscriptions();

                    List<Subscription> subs = new ArrayList<>();
                    subs = PubSubManager.getInstance(accountItem.getConnection(), null).getSubscriptions();

                    *//*if (sub.size()==0) {

                    LeafNode sutest = PubSubManager.getInstance(accountItem.getConnection(), null).getLeafNode(METADATA_NAMESPACE);
                        sutest.unsubscribe(accountItem.getRealJid().asBareJid().toString());
                        List<Subscription> subsC = new ArrayList<>();
                        List<Subscription> subsCTest = new ArrayList<>();
                        Subscription ss = su.subscribe(accountItem.getRealJid().asBareJid().toString());
                        Subscription ssTest = sutest.subscribe(accountItem.getRealJid().asBareJid().toString());
                        Subscription.State state = ss.getState();
                        Subscription.State statetest = ssTest.getState();

                    }*//*
                    //DataExtension s = (DataExtension)e.get(0).getPayload();
                    *//*final byte[] k = s.getData();
                    Application.getInstance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (metadataStore != null && !metadataStore.hasAvatarAvailable((EntityBareJid) contact, e.get(0).getId())) {
                                String id = e.get(0).getId();
                                metadataStore.setAvatarAvailable((EntityBareJid) contact, e.get(0).getId());
                            }
                            String sh1 = AvatarManager.getAvatarHash(k);
                            AvatarManager.getInstance().onAvatarReceived(contact, sh1, k, "xep");
                        }
                    });*//*
                } catch (PubSubException.NotALeafNodeException e) {
                    e.printStackTrace();
                } catch (SmackException.NoResponseException e) {
                    e.printStackTrace();
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (XMPPException.XMPPErrorException e) {
                    e.printStackTrace();
                }
            }
        });
    }
*/

    public String publishAvatarData(byte[] data)
            throws NoResponseException, NotConnectedException, XMPPErrorException, InterruptedException, PubSubException.NotALeafNodeException {
        //String itemId = Base64.encodeToString(SHA1.bytes(data));
        //String s = Base64.encodeToString(data);
        //String test = encryptThisByte(data);
        String itemId = AvatarManager.getAvatarHash(data);
        //itemId = itemId.toLowerCase();
        publishAvatarData(data, itemId);
        return itemId;
    }

    /*public static String encryptThisByte(byte[] input)
    {
        try {
            // getInstance() method is called with algorithm SHA-1
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            // digest() method is called
            // to calculate message digest of the input string
            // returned as array of byte
            byte[] messageDigest = md.digest(input);

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);

            // Add preceding 0s to make it 32 bit
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }

            // return the HashText
            return hashtext;
        }

        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }*/

    private void publishAvatarData(byte[] data, String itemId)
            throws NoResponseException, NotConnectedException, XMPPErrorException, InterruptedException, PubSubException.NotALeafNodeException {
        DataExtension dataExtension = new DataExtension(data);
        getOrCreateDataNode().publish(new PayloadItem<>(itemId, dataExtension));
    }

    /**
     * Publish metadata about an avatar to the metadata node.
     *
     * @param itemId SHA-1 sum of the image of type image/png
     * @param info info element containing metadata of the file
     * @param pointers list of metadata pointer elements
     *
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void publishAvatarMetadata(String itemId, MetadataInfo info, List<MetadataPointer> pointers)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotALeafNodeException {
        publishAvatarMetadata(itemId, Collections.singletonList(info), pointers);
    }

    /**
     * Publish avatar metadata.
     *
     * @param itemId SHA-1 sum of the avatar image representation of type image/png
     * @param infos list of metadata elements
     * @param pointers list of pointer elements
     *
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void publishAvatarMetadata(String itemId, List<MetadataInfo> infos, List<MetadataPointer> pointers)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotALeafNodeException {
        MetadataExtension metadataExtension = new MetadataExtension(infos, pointers);
        getOrCreateMetadataNode().publish(new PayloadItem<>(itemId, metadataExtension));

        if (metadataStore == null) {
            return;
        }
        // Mark our own avatar as locally available so that we don't get updates for it
        metadataStore.setAvatarAvailable(connection().getUser().asEntityBareJidOrThrow(), itemId);
    }

    /**
     * Publish metadata about an avatar available via HTTP.
     * This method can be used together with HTTP File Upload as an alternative to PubSub for avatar publishing.
     *
     * @param itemId SHA-1 sum of the avatar image file.
     * @param url HTTP(S) Url of the image file.
     * @param bytes size of the file in bytes
     * @param type content type of the file
     * @param pixelsHeight height of the image file in pixels
     * @param pixelsWidth width of the image file in pixels
     *
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void publishHttpAvatarMetadata(String itemId, URL url, long bytes, String type,
                                          int pixelsHeight, int pixelsWidth)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotALeafNodeException {
        MetadataInfo info = new MetadataInfo(itemId, url, bytes, type, pixelsHeight, pixelsWidth);
        publishAvatarMetadata(itemId, info, null);
    }

    /**
     * Publish avatar metadata with its size in pixels.
     *
     * @param itemId
     * @param bytes
     * @param type
     * @param pixelsHeight
     * @param pixelsWidth
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void publishAvatarMetadata(String itemId, long bytes, String type, int pixelsHeight,
                                      int pixelsWidth)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotALeafNodeException {
        MetadataInfo info = new MetadataInfo(itemId, null, bytes, type, pixelsHeight, pixelsWidth);
        publishAvatarMetadata(itemId, info, null);
    }

    /**
     * Publish an empty metadata element to disable avatar publishing.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0084.html#proto-meta">§4.2 Metadata Element</a>
     *
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void unpublishAvatar()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, PubSubException.NotALeafNodeException {
        getOrCreateMetadataNode().publish(new PayloadItem<>(new MetadataExtension(null)));
    }


    private final PEPListener metadataExtensionListener = new PEPListener() {
        @Override
        public void eventReceived(EntityBareJid from, EventElement event, Message message) {
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
                        if (metadataStore!=null && metadataStore.hasAvatarAvailable(from, ((PayloadItem<?>) item).getId())) {
                            // The metadata store implies that we have a local copy of the published image already. Skip.
                            continue;
                        }

                        for (AvatarListener listener : avatarListeners) {
                            listener.onAvatarUpdateReceived(from, metadataExtension);
                        }

                        for (MetadataInfo info : metadataExtension.getInfoElements()){
                            try {
                                byte[] avatar = fetchAvatarFromPubSub(from, info);
                                String sh1 = info.getId();
                                //String sh1 = AvatarManager.getAvatarHash(avatar);
                                AvatarManager.getInstance().onAvatarReceived(from,sh1,avatar, "xep");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (PubSubException.NotALeafNodeException e) {
                                e.printStackTrace();
                            } catch (NoResponseException e) {
                                e.printStackTrace();
                            } catch (NotConnectedException e) {
                                e.printStackTrace();
                            } catch (XMPPErrorException e) {
                                e.printStackTrace();
                            }
                        }
                    } /*else if (payloadItem.getPayload() instanceof DataExtension){
                        DataExtension dataExtension = (DataExtension) payloadItem.getPayload();
                        byte[] avatar = dataExtension.getData();
                        String sh1 = AvatarManager.getAvatarHash(avatar);
                        AvatarManager.getInstance().onAvatarReceived(from, sh1, avatar, "xep");
                    }*/
                }
            }
        }
    };


    /*private final PEPListener metadataExtensionListener = new PEPListener() {
        @Override
        public void eventReceived(EntityBareJid from, EventElement event, Message message) {
            if (!MetadataExtension.NAMESPACE.equals(event.getNamespace())) {
                // Totally not of interest for us.
                return;
            }

            if (!MetadataExtension.ELEMENT.equals(event.getElementName())) {
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

                    if (!(payloadItem.getPayload() instanceof MetadataExtension)) {
                        continue;
                    }

                    MetadataExtension metadataExtension = (MetadataExtension) payloadItem.getPayload();
                    if (metadataStore != null && metadataStore.hasAvatarAvailable(from, ((PayloadItem<?>) item).getId())) {
                        // The metadata store implies that we have a local copy of the published image already. Skip.
                        continue;
                    }

                    for (AvatarListener listener : avatarListeners) {
                        listener.onAvatarUpdateReceived(from, metadataExtension);
                    }

                    for (MetadataInfo info : metadataExtension.getInfoElements()){
                        try {
                            byte[] avatar = fetchAvatarFromPubSub(from, info);
                            String sh1 = AvatarManager.getAvatarHash(avatar);
                            AvatarManager.getInstance().onAvatarReceived(from,sh1,avatar, "xep");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (PubSubException.NotALeafNodeException e) {
                            e.printStackTrace();
                        } catch (NoResponseException e) {
                            e.printStackTrace();
                        } catch (NotConnectedException e) {
                            e.printStackTrace();
                        } catch (XMPPErrorException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    };*/

}