package org.jivesoftware.smack;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

/**
 * Storage for {@link KeyStore} objects.
 * 
 * @author alexander.ivanov
 * 
 */
public class KeyStoreManager {

	private static Map<KeyStoreOptions, KeyStore> stores = new HashMap<KeyStoreOptions, KeyStore>();

	private KeyStoreManager() {
	}

	synchronized static public KeyStore getOrCreateKeyStore(
			ConnectionConfiguration configuration) {
		KeyStoreOptions options = new KeyStoreOptions(
				configuration.getTruststoreType(),
				configuration.getTruststorePath(),
				configuration.getTruststorePassword());
		if (stores.containsKey(options))
			return stores.get(options);
		InputStream in = null;
		KeyStore trustStore = null;
		try {
			trustStore = KeyStore.getInstance(options.getType());
			if (options.getPath() != null)
				in = new BufferedInputStream(new FileInputStream(
						options.getPath()));
			char[] chars = null;
			if (options.getPassword() != null)
				chars = options.getPassword().toCharArray();
			trustStore.load(in, chars);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ioe) {
					// Ignore.
				}
			}
		}
		stores.put(options, trustStore);
		return trustStore;
	}

	private static class KeyStoreOptions {
		private final String type;
		private final String path;
		private final String password;

		public KeyStoreOptions(String type, String path, String password) {
			super();
			this.type = type;
			this.path = path;
			this.password = password;
		}

		public String getType() {
			return type;
		}

		public String getPath() {
			return path;
		}

		public String getPassword() {
			return password;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((password == null) ? 0 : password.hashCode());
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			KeyStoreOptions other = (KeyStoreOptions) obj;
			if (password == null) {
				if (other.password != null)
					return false;
			} else if (!password.equals(other.password))
				return false;
			if (path == null) {
				if (other.path != null)
					return false;
			} else if (!path.equals(other.path))
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}
	}

}
