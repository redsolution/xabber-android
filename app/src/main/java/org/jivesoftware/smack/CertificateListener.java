package org.jivesoftware.smack;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Listener for certificate.
 * 
 * Certificate can still pass validation independently of its valid state if
 * appropriate callback will return <code>true</code> value.
 * 
 * @author alexander.ivanov
 * 
 */
public interface CertificateListener {

	boolean onValid(X509Certificate[] chain);

	boolean onInvalidChain(X509Certificate[] chain,
			CertificateException exception);

	boolean onSelfSigned(X509Certificate certificate,
			CertificateException exception);

	boolean onInvalidTarget(X509Certificate certificate,
			CertificateException exception);

}