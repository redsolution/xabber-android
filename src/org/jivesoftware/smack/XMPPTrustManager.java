package org.jivesoftware.smack;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.ssl.AbstractVerifier;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;

class XMPPTrustManager implements X509TrustManager {

	private final X509TrustManager trustManager;
	private final String hostname;
	private final AbstractVerifier verifier;
	private final CertificateListener listener;
	private final boolean chainCheck;
	private final boolean domainCheck;
	private final boolean allowSelfSigned;

	public XMPPTrustManager(KeyStore trustStore, String hostname,
			CertificateListener listener, boolean chainCheck,
			boolean domainCheck, boolean allowSelfSigned)
			throws NoSuchAlgorithmException, KeyManagementException,
			KeyStoreException {
		TrustManagerFactory trustManagerFactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(trustStore);
		this.trustManager = chooseTrustManager(trustManagerFactory
				.getTrustManagers());
		this.listener = listener;
		this.verifier = new BrowserCompatHostnameVerifier();
		// if IPv6 strip off the "[]"
		if (hostname != null && hostname.startsWith("[")
				&& hostname.endsWith("]"))
			this.hostname = hostname.substring(1, hostname.length() - 1);
		else
			this.hostname = hostname;
		this.chainCheck = chainCheck;
		this.domainCheck = domainCheck;
		this.allowSelfSigned = allowSelfSigned;
	}

	private X509TrustManager chooseTrustManager(TrustManager[] tm)
			throws KeyManagementException {
		// We only use the first instance of X509TrustManager passed to us.
		for (int i = 0; tm != null && i < tm.length; i++)
			if (tm[i] instanceof X509TrustManager)
				return (X509TrustManager) tm[i];
		throw new KeyManagementException();
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		trustManager.checkClientTrusted(chain, authType);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		if (chainCheck)
			checkChain(chain, authType);
		if (domainCheck)
			checkTarget(chain[0]);
		if (listener.onValid(chain))
			return;
		throw new CertificateException();
	}

	private void checkChain(X509Certificate[] chain, String authType)
			throws CertificateException {
		try {
			trustManager.checkClientTrusted(chain, authType);
		} catch (CertificateException e) {
			if (allowSelfSigned && isSelfSigned(chain)) {
				if (listener.onSelfSigned(chain[0], e))
					return;
			} else {
				if (listener.onInvalidChain(chain, e))
					return;
			}
			throw e;
		}
	}

	private boolean isSelfSigned(X509Certificate[] chain) {
		return chain.length == 1
				&& chain[0].getIssuerDN().equals(chain[0].getSubjectDN());
	}

	private void checkTarget(X509Certificate certificate)
			throws CertificateException {
		try {
			verifier.verify(hostname, certificate);
		} catch (SSLException e) {
			CertificateException exception = new CertificateException(e);
			if (listener.onInvalidTarget(certificate, exception))
				return;
			throw exception;
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return trustManager.getAcceptedIssuers();
	}

}