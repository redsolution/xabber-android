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
package com.xabber.android.data.connection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.CertificateListener;
import org.jivesoftware.smack.util.StringUtils;

import android.content.res.AssetManager;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.OnClearListener;
import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.notification.BaseNotificationProvider;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.androiddev.R;

/**
 * Manage certificate exceptions.
 * 
 * Key store in "<CertificateInvalidReason>.bsk" asset will be used as default
 * allowed certificates.
 * 
 * @author alexander.ivanov
 * 
 */
public class CertificateManager implements OnLoadListener, OnClearListener {

	private static final String INVALID = "invalid";

	private static final char[] PASSWORD = "password".toCharArray();

	/**
	 * File to be used to store user certificates.
	 */
	private static final Map<CertificateInvalidReason, File> KEY_FILES;

	private static final CertificateManager instance;

	static {
		instance = new CertificateManager();
		Application.getInstance().addManager(instance);

		KEY_FILES = new HashMap<CertificateInvalidReason, File>();
		for (CertificateInvalidReason reason : CertificateInvalidReason
				.values())
			KEY_FILES.put(reason, new File(Application.getInstance()
					.getFilesDir(), reason.toString() + ".bsk"));
	}

	public static CertificateManager getInstance() {
		return instance;
	}

	/**
	 * Key store for confirmed certificates.
	 */
	private final Map<CertificateInvalidReason, KeyStore> keyStores;

	/**
	 * Key store for preset certificates.
	 */
	private final Map<CertificateInvalidReason, KeyStore> defaultStores;

	private final BaseNotificationProvider<PendingCertificate> pendingCertificateProvider = new BaseNotificationProvider<PendingCertificate>(
			R.drawable.ic_stat_auth_failed) {

		@Override
		public void clearNotifications() {
			ignoreCertificates.addAll(getNotifications());
			super.clearNotifications();
		}

	};

	/**
	 * Certificate issues not to be displayed to the user.
	 */
	private final Collection<PendingCertificate> ignoreCertificates;

	private CertificateManager() {
		defaultStores = new HashMap<CertificateInvalidReason, KeyStore>();
		keyStores = new ConcurrentHashMap<CertificateInvalidReason, KeyStore>();
		ignoreCertificates = new ArrayList<PendingCertificate>();
	}

	@Override
	public void onLoad() {
		final Map<CertificateInvalidReason, KeyStore> defaultStores = new HashMap<CertificateInvalidReason, KeyStore>();
		final Map<CertificateInvalidReason, KeyStore> keyStores = new HashMap<CertificateInvalidReason, KeyStore>();
		AssetManager assetManager = Application.getInstance().getResources()
				.getAssets();
		for (CertificateInvalidReason reason : CertificateInvalidReason
				.values()) {
			InputStream stream;
			try {
				stream = assetManager.open(reason.toString() + ".bsk");
			} catch (IOException e) {
				stream = null;
			}
			defaultStores.put(reason, loadKeyStore(stream));
			stream = getInputStream(KEY_FILES.get(reason));
			if (stream != null)
				keyStores.put(reason, loadKeyStore(stream));
		}
		Application.getInstance().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				onLoaded(defaultStores, keyStores);
			}
		});
	}

	private static InputStream getInputStream(File file) {
		if (file.exists()) {
			try {
				return new FileInputStream(file);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		} else {
			return null;
		}
	}

	private static KeyStore loadKeyStore(InputStream stream) {
		KeyStore keyStore;
		try {
			keyStore = KeyStore.getInstance("BKS");
		} catch (KeyStoreException e) {
			throw new RuntimeException(e);
		}
		try {
			keyStore.load(stream, PASSWORD);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (CertificateException e) {
			throw new RuntimeException(e);
		}
		try {
			if (stream != null)
				stream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return keyStore;
	}

	/**
	 * Create new key store instance based on default key store.
	 * 
	 * @param reason
	 * @return
	 * @throws KeyStoreException
	 */
	private KeyStore createKeyStore(CertificateInvalidReason reason) {
		KeyStore source = defaultStores.get(reason);
		Enumeration<String> enumeration;
		try {
			enumeration = source.aliases();
		} catch (KeyStoreException e) {
			throw new RuntimeException(e);
		}
		KeyStore keyStore = loadKeyStore(null);
		while (enumeration.hasMoreElements()) {
			String alias = enumeration.nextElement();
			try {
				keyStore.setCertificateEntry(alias,
						source.getCertificate(alias));
			} catch (KeyStoreException e) {
				throw new RuntimeException(e);
			}
		}
		return keyStore;
	}

	private void onLoaded(
			Map<CertificateInvalidReason, KeyStore> defaultStores,
			Map<CertificateInvalidReason, KeyStore> keyStores) {
		this.defaultStores.putAll(defaultStores);
		this.keyStores.putAll(keyStores);
		for (CertificateInvalidReason reason : CertificateInvalidReason
				.values())
			if (!this.keyStores.containsKey(reason))
				this.keyStores.put(reason, createKeyStore(reason));
		NotificationManager.getInstance().registerNotificationProvider(
				pendingCertificateProvider);
	}

	/**
	 * Verify whether this certificate was previously allowed by user. And
	 * create pending notification to accept or decline it.
	 * 
	 * @param server
	 * @param x509Certificate
	 *            invalid certificate.
	 * @param reason
	 *            reason of invalidation.
	 * @return whether this certificate was allowed by user.
	 */
	private boolean isTrustedCertificate(final String server,
			final X509Certificate x509Certificate,
			final CertificateInvalidReason reason) {
		KeyStore keyStore = keyStores.get(reason);
		try {
			if (keyStore != null
					&& keyStore.getCertificateAlias(x509Certificate) != null)
				return true;
		} catch (KeyStoreException e) {
			LogManager.exception(this, e);
		}
		final String fingerprint = getFingerprint(x509Certificate);
		Application.getInstance().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (getPendingCertificate(fingerprint, reason) != null
						|| getPendingCertificate(fingerprint, reason,
								ignoreCertificates) != null)
					return;
				pendingCertificateProvider.add(new PendingCertificate(server,
						reason, x509Certificate, fingerprint), true);
			}
		});
		return false;
	}

	/**
	 * @param fingerprint
	 * @param reason
	 * @param collection
	 * @return Pending certificate or <code>null</code>.
	 */
	private static PendingCertificate getPendingCertificate(String fingerprint,
			CertificateInvalidReason reason,
			Collection<PendingCertificate> collection) {
		for (PendingCertificate pendingCertificate : collection)
			if (pendingCertificate.getFingerprint().equals(fingerprint)
					&& reason == pendingCertificate.getReason())
				return pendingCertificate;
		return null;
	}

	public PendingCertificate getPendingCertificate(String fingerprint,
			CertificateInvalidReason reason) {
		return getPendingCertificate(fingerprint, reason,
				pendingCertificateProvider.getNotifications());
	}

	/**
	 * Accept pending certificate.
	 * 
	 * @param fingerprint
	 * @param reason
	 */
	public void accept(String fingerprint, final CertificateInvalidReason reason) {
		PendingCertificate pendingCertificate = getPendingCertificate(
				fingerprint, reason);
		if (pendingCertificate == null)
			return;
		String alias;
		while (true) {
			alias = StringUtils.randomString(8);
			try {
				if (!keyStores.get(reason).containsAlias(alias))
					break;
			} catch (KeyStoreException e) {
				LogManager.exception(this, e);
				return;
			}
		}
		try {
			keyStores.get(reason).setEntry(
					alias,
					new KeyStore.TrustedCertificateEntry(pendingCertificate
							.getX509Certificate()), null);
		} catch (KeyStoreException e) {
			LogManager.exception(this, e);
			return;
		}
		pendingCertificateProvider.remove(pendingCertificate);
		Application.getInstance().runInBackground(new Runnable() {
			@Override
			public void run() {
				FileOutputStream out;
				try {
					out = new FileOutputStream(KEY_FILES.get(reason));
				} catch (FileNotFoundException e) {
					LogManager.exception(CertificateManager.this, e);
					return;
				}
				try {
					keyStores.get(reason).store(out, PASSWORD);
				} catch (KeyStoreException e) {
					LogManager.exception(CertificateManager.this, e);
				} catch (NoSuchAlgorithmException e) {
					LogManager.exception(CertificateManager.this, e);
				} catch (IOException e) {
					LogManager.exception(CertificateManager.this, e);
				} catch (CertificateException e) {
					LogManager.exception(CertificateManager.this, e);
				}
				try {
					out.close();
				} catch (IOException e) {
					LogManager.exception(CertificateManager.this, e);
				}
			}
		});
	}

	/**
	 * Ignore pending certificate.
	 * 
	 * @param fingerprint
	 * @param reason
	 */
	public void discard(String fingerprint, CertificateInvalidReason reason) {
		PendingCertificate pendingCertificate = getPendingCertificate(
				fingerprint, reason);
		if (pendingCertificate == null)
			return;
		pendingCertificateProvider.remove(pendingCertificate);
		ignoreCertificates.add(pendingCertificate);
	}

	/**
	 * @param x509Certificate
	 * @return Finger print for the given certificate.
	 */
	private static String getFingerprint(X509Certificate x509Certificate) {
		byte[] data;
		try {
			data = x509Certificate.getEncoded();
		} catch (CertificateEncodingException e) {
			LogManager.exception(PendingCertificate.class, e);
			return INVALID;
		}
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			LogManager.exception(PendingCertificate.class, e);
			return INVALID;
		}
		digest.update(data);
		byte[] bytes = digest.digest();
		return StringUtils.encodeHex(bytes);
	}

	/**
	 * @param fingerprint
	 * @return Formatted fingerprint to be shown.
	 */
	public static String showFingerprint(String fingerprint) {
		if (fingerprint == null)
			return null;
		StringBuffer buffer = new StringBuffer();
		for (int index = 0; index < fingerprint.length(); index++) {
			if (index > 0 && index % 2 == 0)
				buffer.append(':');
			buffer.append(fingerprint.charAt(index));
		}
		return buffer.toString().toUpperCase();
	}

	@Override
	public void onClear() {
		for (File file : KEY_FILES.values())
			file.delete();
	}

	/**
	 * Removes all certificates.
	 */
	public void removeCertificates() {
		pendingCertificateProvider.clearNotifications();
		ignoreCertificates.clear();
		for (CertificateInvalidReason reason : CertificateInvalidReason
				.values())
			keyStores.put(reason, createKeyStore(reason));
		Application.getInstance().runInBackground(new Runnable() {

			@Override
			public void run() {
				for (File file : KEY_FILES.values())
					file.delete();
			}

		});
	}

	public CertificateListener createCertificateListener(
			ConnectionItem connectionItem) {
		final String server = connectionItem.getConnectionSettings()
				.getServerName();
		return new CertificateListener() {

			@Override
			public boolean onValid(X509Certificate[] chain) {
				return true;
			}

			@Override
			public boolean onSelfSigned(X509Certificate certificate,
					CertificateException exception) {
				LogManager.exception(CertificateManager.this, exception);
				return isTrustedCertificate(server, certificate,
						CertificateInvalidReason.selfSigned);
			}

			@Override
			public boolean onInvalidTarget(X509Certificate certificate,
					CertificateException exception) {
				LogManager.exception(CertificateManager.this, exception);
				return isTrustedCertificate(server, certificate,
						CertificateInvalidReason.invalidTarget);
			}

			@Override
			public boolean onInvalidChain(X509Certificate[] chain,
					CertificateException exception) {
				LogManager.exception(CertificateManager.this, exception);
				return isTrustedCertificate(server, chain[0],
						CertificateInvalidReason.invalidChane);
			}

		};
	}

}
