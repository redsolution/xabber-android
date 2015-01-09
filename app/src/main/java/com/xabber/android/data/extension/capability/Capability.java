/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 * 
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 * 
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data.extension.capability;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jivesoftware.smack.util.Base64;

import com.xabber.android.data.entity.BaseEntity;

class Capability extends BaseEntity {

	private static final String SHA1_METHOD = "sha-1";

	public static final String DIRECT_REQUEST_METHOD = "com.xabber.android.data.extension.Capability.DIRECT_REQUEST";

	/**
	 * Hash mehdod.
	 */
	private final String hash;

	/**
	 * Product code.
	 */
	private final String node;

	/**
	 * Hashed capabilities.
	 */
	private final String version;

	public Capability(String account, String user, String hash, String node,
			String version) {
		super((isLegacy(hash) || isSupportedHash(hash)) ? null : account,
				(isLegacy(hash) || isSupportedHash(hash)) ? null : user);
		this.hash = hash;
		this.node = node;
		this.version = version;
	}

	/**
	 * @return Hash method.
	 */
	public String getHash() {
		return hash;
	}

	/**
	 * @return Product code.
	 */
	public String getNode() {
		return node;
	}

	/**
	 * @return Hashed capabilities.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @param value
	 * @return hasded value using hash method.
	 */
	public String getHashedValue(String value) {
		try {
			MessageDigest md = MessageDigest.getInstance(hash.toUpperCase());
			byte[] digest = md.digest(value.getBytes());
			return Base64.encodeBytes(digest);
		} catch (NoSuchAlgorithmException nsae) {
			return null;
		}
	}

	/**
	 * @param hash
	 * @return Whether hash method is supported.
	 */
	private static boolean isSupportedHash(String hash) {
		return SHA1_METHOD.equals(hash);
	}

	/**
	 * @return Whether hash method is supported.
	 */
	public boolean isSupportedHash() {
		return isSupportedHash(hash);
	}

	/**
	 * @param hash
	 * @return Whether this is legacy capability.
	 */
	private static boolean isLegacy(String hash) {
		return hash == null;
	}

	/**
	 * @return Whether this is legacy capability.
	 */
	public boolean isLegacy() {
		return isLegacy(hash);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((hash == null) ? 0 : hash.hashCode());
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Capability other = (Capability) obj;
		if (hash == null) {
			if (other.hash != null)
				return false;
		} else if (!hash.equals(other.hash))
			return false;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (!node.equals(other.node))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return hash + ":" + node + "#" + version;
	}

}
