package org.jivesoftware.smack;

import java.util.List;

import org.jivesoftware.smack.packet.RosterPacket;

/**
 * This is an interface for persistent roster storage needed to implement XEP-0237
 * @author Till Klocke
 *
 */

public interface RosterStorage {
	
	/**
	 * This method returns a List object with all RosterEntries contained in this store.
	 * @return List object with all entries in local roster storage
	 */
	public List<RosterPacket.Item> getEntries();
	/**
	 * This method returns the RosterEntry which belongs to a specific user.
	 * @param bareJid The bare JID of the RosterEntry
	 * @return The RosterEntry which belongs to that user
	 */
	public RosterPacket.Item getEntry(String bareJid);
	/**
	 * Returns the number of entries in this roster store
	 * @return the number of entries
	 */
	public int getEntryCount();
	/**
	 * This methos returns the version number as specified by the "ver" attribute
	 * of the local store. Should return an emtpy string if store is empty.
	 * @return local roster version
	 */
	public String getRosterVersion();
	/**
	 * This method stores a new RosterEntry in this store or overrides an existing one.
	 * If ver is null an IllegalArgumentException should be thrown.
	 * @param entry the entry to save
	 * @param ver the version this roster push contained
	 */
	public void addEntry(RosterPacket.Item item, String ver);
	/**
	 * Removes an entry from the persistent storage
	 * @param bareJid The bare JID of the entry to be removed
	 */
	public void removeEntry(String bareJid);
	/**
	 * Update an entry which has been modified locally
	 * @param entry the entry to be updated
	 */
	public void updateLocalEntry(RosterPacket.Item item);
}
