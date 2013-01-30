/**
 * $RCSfile$
 * $Revision: 9920 $
 * $Date: 2008-02-16 00:32:46 +0800 (Sat, 16 Feb 2008) $
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.smackx;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.Base64;
import org.jivesoftware.smackx.packet.CapsExtension;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DataForm;

import java.util.*;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manages discovery of services in XMPP entities. This class provides:
 * <ol>
 * <li>A registry of supported features in this XMPP entity.
 * <li>Automatic response when this XMPP entity is queried for information.
 * <li>Ability to discover items and information of remote XMPP entities.
 * <li>Ability to publish publicly available items.
 * </ol>  
 * 
 * @author Gaston Dombiak
 */
public class ServiceDiscoveryManager {

    private static String identityName = "aSmack";
    private static String identityType = "handheld";
    private static String entityNode = "http://www.igniterealtime.org/projects/smack/";
    
    private static boolean cacheNonCaps=true;

    private String currentCapsVersion = null;
    private boolean sendPresence = false;
    
    private Map<String,DiscoverInfo> nonCapsCache =
    	new ConcurrentHashMap<String,DiscoverInfo>();

    private EntityCapsManager capsManager;

    private static Map<Connection, WeakReference<ServiceDiscoveryManager>> instances =
        new WeakHashMap<Connection, WeakReference<ServiceDiscoveryManager>>();

    private Connection connection;
    private final List<String> features = new ArrayList<String>();
    private DataForm extendedInfo = null;
    private Map<String, NodeInformationProvider> nodeInformationProviders =
            new ConcurrentHashMap<String, NodeInformationProvider>();

    // Create a new ServiceDiscoveryManager on every established connection
    static {
        // Add service discovery for normal XMPP c2s connections
        XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(Connection connection) {
                new ServiceDiscoveryManager(connection);
            }
        });
    }

    /**
     * Creates a new ServiceDiscoveryManager for a given connection. This means that the 
     * service manager will respond to any service discovery request that the connection may
     * receive. 
     * 
     * @param connection the connection to which a ServiceDiscoveryManager is going to be created.
     */
    public ServiceDiscoveryManager(Connection connection) {
        this.connection = connection;

        // For every XMPPConnection, add one EntityCapsManager.
        if (connection instanceof XMPPConnection) {
            setEntityCapsManager(new EntityCapsManager());
            capsManager.addCapsVerListener(new CapsPresenceRenewer());
        }

        renewEntityCapsVersion();

        init();
    }

    /**
     * Returns the ServiceDiscoveryManager instance associated with a given Connection.
     * 
     * @param connection the connection used to look for the proper ServiceDiscoveryManager.
     * @return the ServiceDiscoveryManager associated with a given Connection.
     */
    public synchronized static ServiceDiscoveryManager getInstanceFor(Connection connection) {
        WeakReference<ServiceDiscoveryManager> reference = instances.get(connection);
        if (reference == null)
            return null;
        else
            return reference.get();
    }

    /**
     * Returns the name of the client that will be returned when asked for the client identity
     * in a disco request. The name could be any value you need to identity this client.
     * 
     * @return the name of the client that will be returned when asked for the client identity
     *          in a disco request.
     */
    public static String getIdentityName() {
        return identityName;
    }

    /**
     * Sets the name of the client that will be returned when asked for the client identity
     * in a disco request. The name could be any value you need to identity this client.
     * 
     * @param name the name of the client that will be returned when asked for the client identity
     *          in a disco request.
     */
    public static void setIdentityName(String name) {
        identityName = name;
    }

    /**
     * Returns the type of client that will be returned when asked for the client identity in a 
     * disco request. The valid types are defined by the category client. Follow this link to learn 
     * the possible types: <a href="http://www.jabber.org/registrar/disco-categories.html#client">Jabber::Registrar</a>.
     * 
     * @return the type of client that will be returned when asked for the client identity in a 
     *          disco request.
     */
    public static String getIdentityType() {
        return identityType;
    }

    /**
     * Sets the type of client that will be returned when asked for the client identity in a 
     * disco request. The valid types are defined by the category client. Follow this link to learn 
     * the possible types: <a href="http://www.jabber.org/registrar/disco-categories.html#client">Jabber::Registrar</a>.
     * 
     * @param type the type of client that will be returned when asked for the client identity in a 
     *          disco request.
     */
    public static void setIdentityType(String type) {
        identityType = type;
    }
    
    /**
     * Enables caching of non caps entities to reduce traffic. If enabled discover infos of 
     * entities without xep-0115 are store in a String,DiscoverInfo map and
     * discoverInfo(String) queries this map before sending a real discover info to the
     * remote entity. Enabled by default.
     */
    public static void setNonCapsCaching(boolean set){
    	cacheNonCaps = true;
    }
    
    /**
     * Check if caching of non caps entities is enabled
     */
    
    public static boolean isNonCapsCachingEnabled(){
    	return cacheNonCaps;
    }

    /**
     * Add discover info response data.
     *
     * @param response the discover info response packet
     */
    public void addDiscoverInfoTo(DiscoverInfo response) {
        // Set this client identity
        DiscoverInfo.Identity identity = new DiscoverInfo.Identity("client",
                getIdentityName());
        identity.setType(getIdentityType());
        response.addIdentity(identity);
        response.addFeature("http://jabber.org/protocol/disco#info");
        response.addFeature("http://jabber.org/protocol/disco#items");
        // Add the registered features to the response
        synchronized (features) {
            // Add Entity Capabilities (XEP-0115) feature node.
            response.addFeature("http://jabber.org/protocol/caps");

            for (Iterator<String> it = getFeatures(); it.hasNext();) {
                response.addFeature(it.next());
            }
            if (extendedInfo != null) {
                response.addExtension(extendedInfo);
            }
        }
    }

    /**
     * Get a DiscoverInfo for the current entity caps node.
     *
     * @return a DiscoverInfo for the current entity caps node
     */
    public DiscoverInfo getOwnDiscoverInfo() {
        DiscoverInfo di = new DiscoverInfo();
        di.setType(IQ.Type.RESULT);
        di.setNode(capsManager.getNode() + "#" + getEntityCapsVersion());

        // Add discover info
        addDiscoverInfoTo(di);

        return di;
    }

    /**
     * Initializes the packet listeners of the connection that will answer to any
     * service discovery request. 
     */
    private void init() {
        // Register the new instance and associate it with the connection 
        synchronized (ServiceDiscoveryManager.class) {
            instances.put(connection, new WeakReference<ServiceDiscoveryManager>(this));
        }
        // Add a listener to the connection that removes the registered instance when
        // the connection is closed
        connection.addConnectionListener(new ConnectionListener() {
            public void connectionClosed() {
                // Unregister this instance since the connection has been closed
                synchronized (ServiceDiscoveryManager.class) {
                    instances.remove(connection);
                }
            }

            public void connectionClosedOnError(Exception e) {
                // Unregister this instance since the connection has been closed
                synchronized (ServiceDiscoveryManager.class) {
                    instances.remove(connection);
                }
            }

            public void reconnectionSuccessful() {
                // Register this instance since the connection has been
                // reestablished
                synchronized (ServiceDiscoveryManager.class) {
                    instances.put(connection, new WeakReference<ServiceDiscoveryManager>(ServiceDiscoveryManager.this));
                }
            }

            public void reconnectionFailed(Exception e) {
                // ignore
            }

            public void reconnectingIn(int seconds) {
                // ignore
            }
        });

        // Intercept presence packages and add caps data when inteded.
        // XEP-0115 specifies that a client SHOULD include entity capabilities
        // with every presence notification it sends.
        PacketFilter capsPacketFilter = new PacketTypeFilter(Presence.class);
        PacketInterceptor packetInterceptor = new PacketInterceptor() {
            public void interceptPacket(Packet packet) {
                if (capsManager != null) {
                    String ver = getEntityCapsVersion();
                    CapsExtension caps = new CapsExtension(capsManager.getNode(), ver, "sha-1");
                    packet.addExtension(caps);
                }
            }
        };
        connection.addPacketInterceptor(packetInterceptor, capsPacketFilter);

        // Listen for disco#items requests and answer with an empty result        
        PacketFilter packetFilter = new PacketTypeFilter(DiscoverItems.class);
        PacketListener packetListener = new PacketListener() {
            public void processPacket(Packet packet) {
                DiscoverItems discoverItems = (DiscoverItems) packet;
                // Send back the items defined in the client if the request is of type GET
                if (discoverItems != null && discoverItems.getType() == IQ.Type.GET) {
                    DiscoverItems response = new DiscoverItems();
                    response.setType(IQ.Type.RESULT);
                    response.setTo(discoverItems.getFrom());
                    response.setPacketID(discoverItems.getPacketID());
                    response.setNode(discoverItems.getNode());

                    // Add the defined items related to the requested node. Look for 
                    // the NodeInformationProvider associated with the requested node.  
                    NodeInformationProvider nodeInformationProvider =
                            getNodeInformationProvider(discoverItems.getNode());
                    if (nodeInformationProvider != null) {
                        // Specified node was found
                        List<DiscoverItems.Item> items = nodeInformationProvider.getNodeItems();
                        if (items != null) {
                            for (DiscoverItems.Item item : items) {
                                response.addItem(item);
                            }
                        }
                    } else if(discoverItems.getNode() != null) {
                        // Return <item-not-found/> error since client doesn't contain
                        // the specified node
                        response.setType(IQ.Type.ERROR);
                        response.setError(new XMPPError(XMPPError.Condition.item_not_found));
                    }
                    connection.sendPacket(response);
                }
            }
        };
        connection.addPacketListener(packetListener, packetFilter);

        // Listen for disco#info requests and answer the client's supported features 
        // To add a new feature as supported use the #addFeature message        
        packetFilter = new PacketTypeFilter(DiscoverInfo.class);
        packetListener = new PacketListener() {
            public void processPacket(Packet packet) {
                DiscoverInfo discoverInfo = (DiscoverInfo) packet;
                // Answer the client's supported features if the request is of the GET type
                if (discoverInfo != null && discoverInfo.getType() == IQ.Type.GET) {
                    DiscoverInfo response = new DiscoverInfo();
                    response.setType(IQ.Type.RESULT);
                    response.setTo(discoverInfo.getFrom());
                    response.setPacketID(discoverInfo.getPacketID());
                    response.setNode(discoverInfo.getNode());
                    // Add the client's identity and features if "node" is
                    // null or our entity caps version.
                    if (discoverInfo.getNode() == null || 
                            capsManager == null ||
                             (capsManager.getNode() + "#" +
                              getEntityCapsVersion()).equals(discoverInfo.getNode())) {
                        addDiscoverInfoTo(response);
                    }
                    else {
                        // Disco#info was sent to a node. Check if we have information of the
                        // specified node
                        NodeInformationProvider nodeInformationProvider =
                                getNodeInformationProvider(discoverInfo.getNode());
                        if (nodeInformationProvider != null) {
                            // Node was found. Add node features
                            List<String> features = nodeInformationProvider.getNodeFeatures();
                            if (features != null) {
                                for(String feature : features) {
                                    response.addFeature(feature);
                                }
                            }
                            // Add node identities
                            List<DiscoverInfo.Identity> identities =
                                    nodeInformationProvider.getNodeIdentities();
                            if (identities != null) {
                                for (DiscoverInfo.Identity identity : identities) {
                                    response.addIdentity(identity);
                                }
                            }
                        }
                        else {
                            // Return <item-not-found/> error since specified node was not found
                            response.setType(IQ.Type.ERROR);
                            response.setError(new XMPPError(XMPPError.Condition.item_not_found));
                        }
                    }
                    connection.sendPacket(response);
                }
            }
        };
        connection.addPacketListener(packetListener, packetFilter);
    }

    /**
     * Returns the NodeInformationProvider responsible for providing information 
     * (ie items) related to a given node or <tt>null</null> if none.<p>
     * 
     * In MUC, a node could be 'http://jabber.org/protocol/muc#rooms' which means that the
     * NodeInformationProvider will provide information about the rooms where the user has joined.
     * 
     * @param node the node that contains items associated with an entity not addressable as a JID.
     * @return the NodeInformationProvider responsible for providing information related 
     * to a given node.
     */
    private NodeInformationProvider getNodeInformationProvider(String node) {
        if (node == null) {
            return null;
        }
        return nodeInformationProviders.get(node);
    }

    /**
     * Sets the NodeInformationProvider responsible for providing information 
     * (ie items) related to a given node. Every time this client receives a disco request
     * regarding the items of a given node, the provider associated to that node will be the 
     * responsible for providing the requested information.<p>
     * 
     * In MUC, a node could be 'http://jabber.org/protocol/muc#rooms' which means that the
     * NodeInformationProvider will provide information about the rooms where the user has joined. 
     * 
     * @param node the node whose items will be provided by the NodeInformationProvider.
     * @param listener the NodeInformationProvider responsible for providing items related
     *      to the node.
     */
    public void setNodeInformationProvider(String node, NodeInformationProvider listener) {
        nodeInformationProviders.put(node, listener);
    }

    /**
     * Removes the NodeInformationProvider responsible for providing information 
     * (ie items) related to a given node. This means that no more information will be
     * available for the specified node.
     * 
     * In MUC, a node could be 'http://jabber.org/protocol/muc#rooms' which means that the
     * NodeInformationProvider will provide information about the rooms where the user has joined. 
     * 
     * @param node the node to remove the associated NodeInformationProvider.
     */
    public void removeNodeInformationProvider(String node) {
        nodeInformationProviders.remove(node);
    }

    /**
     * Returns the supported features by this XMPP entity.
     * 
     * @return an Iterator on the supported features by this XMPP entity.
     */
    public Iterator<String> getFeatures() {
        synchronized (features) {
            return Collections.unmodifiableList(new ArrayList<String>(features)).iterator();
        }
    }

    /**
     * Registers that a new feature is supported by this XMPP entity. When this client is 
     * queried for its information the registered features will be answered.<p>
     *
     * Since no packet is actually sent to the server it is safe to perform this operation
     * before logging to the server. In fact, you may want to configure the supported features
     * before logging to the server so that the information is already available if it is required
     * upon login.
     *
     * @param feature the feature to register as supported.
     */
    public void addFeature(String feature) {
        synchronized (features) {
            features.add(feature);
            renewEntityCapsVersion();
        }
    }

    /**
     * Removes the specified feature from the supported features by this XMPP entity.<p>
     *
     * Since no packet is actually sent to the server it is safe to perform this operation
     * before logging to the server.
     *
     * @param feature the feature to remove from the supported features.
     */
    public void removeFeature(String feature) {
        synchronized (features) {
            features.remove(feature);
            renewEntityCapsVersion();
        }
    }

    /**
     * Returns true if the specified feature is registered in the ServiceDiscoveryManager.
     *
     * @param feature the feature to look for.
     * @return a boolean indicating if the specified featured is registered or not.
     */
    public boolean includesFeature(String feature) {
        synchronized (features) {
            return features.contains(feature);
        }
    }

    /**
     * Registers extended discovery information of this XMPP entity. When this
     * client is queried for its information this data form will be returned as
     * specified by XEP-0128.
     * <p>
     *
     * Since no packet is actually sent to the server it is safe to perform this
     * operation before logging to the server. In fact, you may want to
     * configure the extended info before logging to the server so that the
     * information is already available if it is required upon login.
     *
     * @param info
     *            the data form that contains the extend service discovery
     *            information.
     */
    public void setExtendedInfo(DataForm info) {
        extendedInfo = info;
        renewEntityCapsVersion();
    }

    /**
     * Removes the dataform containing extended service discovery information
     * from the information returned by this XMPP entity.<p>
     *
     * Since no packet is actually sent to the server it is safe to perform this
     * operation before logging to the server.
     */
    public void removeExtendedInfo() {
        extendedInfo = null;
        renewEntityCapsVersion();
    }

    /**
     * Returns the discovered information of a given XMPP entity addressed by its JID
     * if it's known by the entity caps manager.
     *
     * @param entityID the address of the XMPP entity
     * @return the disovered info or null if no such info is available from the
     * entity caps manager.
     * @throws XMPPException if the operation failed for some reason.
     */
    public DiscoverInfo discoverInfoByCaps(String entityID) throws XMPPException {
        DiscoverInfo info = capsManager.getDiscoverInfoByUser(entityID);

        if (info != null) {
            DiscoverInfo newInfo = cloneDiscoverInfo(info);
            newInfo.setFrom(entityID);
            return newInfo;
        }
        else {
            return null;
        }
    }

    /**
     * Returns the discovered information of a given XMPP entity addressed by its JID.
     * 
     * @param entityID the address of the XMPP entity.
     * @return the discovered information.
     * @throws XMPPException if the operation failed for some reason.
     */
    public DiscoverInfo discoverInfo(String entityID) throws XMPPException {
        // Check if the have it cached in the Entity Capabilities Manager
        DiscoverInfo info = discoverInfoByCaps(entityID);

        if (info != null) {
            return info;
        }
        else {
            // If the caps node is known, use it in the request.
            String node = null;

            if (capsManager != null) {
                // Get the newest node#version
                node = capsManager.getNodeVersionByUser(entityID);
            }

            //Check if we cached DiscoverInfo for nonCaps entity
            if(cacheNonCaps && node==null && nonCapsCache.containsKey(entityID)){
            	return nonCapsCache.get(entityID);
            }
            // Discover by requesting from the remote client
            info = discoverInfo(entityID, node);

            // If the node version is known, store the new entry.
            if (node != null && capsManager != null) {
                EntityCapsManager.addDiscoverInfoByNode(node, info);
            }
            // If this is a non caps entity store the discover in nonCapsCache map
            else if(cacheNonCaps && node == null){
            	nonCapsCache.put(entityID, info);
            }
            return info;
        }
    }

    /**
     * Returns the discovered information of a given XMPP entity addressed by its JID and
     * note attribute. Use this message only when trying to query information which is not 
     * directly addressable.
     * 
     * @param entityID the address of the XMPP entity.
     * @param node the attribute that supplements the 'jid' attribute.
     * @return the discovered information.
     * @throws XMPPException if the operation failed for some reason.
     */
    public DiscoverInfo discoverInfo(String entityID, String node) throws XMPPException {
        // Discover the entity's info
        DiscoverInfo disco = new DiscoverInfo();
        disco.setType(IQ.Type.GET);
        disco.setTo(entityID);
        disco.setNode(node);

        // Create a packet collector to listen for a response.
        PacketCollector collector =
            connection.createPacketCollector(new PacketIDFilter(disco.getPacketID()));

        connection.sendPacket(disco);

        // Wait up to 5 seconds for a result.
        IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        collector.cancel();
        if (result == null) {
            throw new XMPPException("No response from the server.");
        }
        if (result.getType() == IQ.Type.ERROR) {
            throw new XMPPException(result.getError());
        }
        return (DiscoverInfo) result;
    }

    /**
     * Returns the discovered items of a given XMPP entity addressed by its JID.
     * 
     * @param entityID the address of the XMPP entity.
     * @return the discovered information.
     * @throws XMPPException if the operation failed for some reason.
     */
    public DiscoverItems discoverItems(String entityID) throws XMPPException {
        return discoverItems(entityID, null);
    }

    /**
     * Returns the discovered items of a given XMPP entity addressed by its JID and
     * note attribute. Use this message only when trying to query information which is not 
     * directly addressable.
     * 
     * @param entityID the address of the XMPP entity.
     * @param node the attribute that supplements the 'jid' attribute.
     * @return the discovered items.
     * @throws XMPPException if the operation failed for some reason.
     */
    public DiscoverItems discoverItems(String entityID, String node) throws XMPPException {
        // Discover the entity's items
        DiscoverItems disco = new DiscoverItems();
        disco.setType(IQ.Type.GET);
        disco.setTo(entityID);
        disco.setNode(node);

        // Create a packet collector to listen for a response.
        PacketCollector collector =
            connection.createPacketCollector(new PacketIDFilter(disco.getPacketID()));

        connection.sendPacket(disco);

        // Wait up to 5 seconds for a result.
        IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        collector.cancel();
        if (result == null) {
            throw new XMPPException("No response from the server.");
        }
        if (result.getType() == IQ.Type.ERROR) {
            throw new XMPPException(result.getError());
        }
        return (DiscoverItems) result;
    }

    /**
     * Returns true if the server supports publishing of items. A client may wish to publish items
     * to the server so that the server can provide items associated to the client. These items will
     * be returned by the server whenever the server receives a disco request targeted to the bare
     * address of the client (i.e. user@host.com).
     * 
     * @param entityID the address of the XMPP entity.
     * @return true if the server supports publishing of items.
     * @throws XMPPException if the operation failed for some reason.
     */
    public boolean canPublishItems(String entityID) throws XMPPException {
        DiscoverInfo info = discoverInfo(entityID);
        return canPublishItems(info);
    }

    /**
     * Returns true if the server supports publishing of items. A client may wish to publish items
     * to the server so that the server can provide items associated to the client. These items will
     * be returned by the server whenever the server receives a disco request targeted to the bare
     * address of the client (i.e. user@host.com).
     * 
     * @param DiscoverInfo the discover info packet to check.
     * @return true if the server supports publishing of items.
     */
    public static boolean canPublishItems(DiscoverInfo info) {
        return info.containsFeature("http://jabber.org/protocol/disco#publish");
    }

    /**
     * Publishes new items to a parent entity. The item elements to publish MUST have at least 
     * a 'jid' attribute specifying the Entity ID of the item, and an action attribute which 
     * specifies the action being taken for that item. Possible action values are: "update" and 
     * "remove".
     * 
     * @param entityID the address of the XMPP entity.
     * @param discoverItems the DiscoveryItems to publish.
     * @throws XMPPException if the operation failed for some reason.
     */
    public void publishItems(String entityID, DiscoverItems discoverItems)
            throws XMPPException {
        publishItems(entityID, null, discoverItems);
    }

    /**
     * Publishes new items to a parent entity and node. The item elements to publish MUST have at 
     * least a 'jid' attribute specifying the Entity ID of the item, and an action attribute which 
     * specifies the action being taken for that item. Possible action values are: "update" and 
     * "remove".
     * 
     * @param entityID the address of the XMPP entity.
     * @param node the attribute that supplements the 'jid' attribute.
     * @param discoverItems the DiscoveryItems to publish.
     * @throws XMPPException if the operation failed for some reason.
     */
    public void publishItems(String entityID, String node, DiscoverItems discoverItems)
            throws XMPPException {
        discoverItems.setType(IQ.Type.SET);
        discoverItems.setTo(entityID);
        discoverItems.setNode(node);

        // Create a packet collector to listen for a response.
        PacketCollector collector =
            connection.createPacketCollector(new PacketIDFilter(discoverItems.getPacketID()));

        connection.sendPacket(discoverItems);

        // Wait up to 5 seconds for a result.
        IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        collector.cancel();
        if (result == null) {
            throw new XMPPException("No response from the server.");
        }
        if (result.getType() == IQ.Type.ERROR) {
            throw new XMPPException(result.getError());
        }
    }

    private DiscoverInfo cloneDiscoverInfo(DiscoverInfo disco) {
        return disco.clone();
    }

    /**
     * Entity Capabilities
     */

    public void setEntityCapsManager(EntityCapsManager manager) {
        capsManager = manager;
        if(connection.getCapsNode()!=null && connection.getHost()!=null){
        	capsManager.addUserCapsNode(connection.getHost(), connection.getCapsNode());
        }
        capsManager.addPacketListener(connection);
    }


    private void renewEntityCapsVersion() {
        // If a XMPPConnection is the managed one, see that the new
        // version is updated
        if (connection instanceof XMPPConnection) {
            if (capsManager != null) {
                capsManager.calculateEntityCapsVersion(getOwnDiscoverInfo(),
                        identityType, identityName, extendedInfo);
                //capsManager.notifyCapsVerListeners();
            }
        }
    }

    private String getEntityCapsVersion() {
        if (capsManager != null) {
            return capsManager.getCapsVersion();
        }
        else {
            return null;
        }
    }
    
    public EntityCapsManager getEntityCapsManager(){
    	return capsManager;
    }

    private void setSendPresence() {
        sendPresence = true;
    }

    private boolean isSendPresence() {
        return sendPresence;
    }


    private class CapsPresenceRenewer implements CapsVerListener {
        public void capsVerUpdated(String ver) {
            // Send an empty presence, and let the packet interceptor
            // add a <c/> node to it.
            if (((XMPPConnection)connection).isAuthenticated() &&
                    (((XMPPConnection)connection).isSendPresence() ||
                     isSendPresence())) {
                Presence presence = new Presence(Presence.Type.available);
                connection.sendPacket(presence);
            }
        }
    }


   /* public static void spam() {
        for (ServiceDiscoveryManager m : instances.values()) {
            m.capsManager.spam();
        }
    }*/
}
