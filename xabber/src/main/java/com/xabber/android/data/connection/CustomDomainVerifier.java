package com.xabber.android.data.connection;

import android.util.Log;
import android.util.Pair;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.net.ssl.SSLSession;

/**
 * Created by valery.miller on 31.10.17.
 */

public class CustomDomainVerifier implements CustomHostnameVerifier {

    private static final String LOGTAG = "XmppDomainVerifier";

    private final String SRVName = "1.3.6.1.5.5.7.8.7";
    private final String xmppAddr = "1.3.6.1.5.5.7.8.5";

    public boolean verify(String domain, String hostname, SSLSession sslSession) {
        try {
            Certificate[] chain = sslSession.getPeerCertificates();
            if (chain.length == 0 || !(chain[0] instanceof X509Certificate)) {
                return false;
            }
            X509Certificate certificate = (X509Certificate) chain[0];
            if (isSelfSigned(certificate)) {
                List<String> domains = getCommonNames(certificate);
                if (domains.size() == 1 && domains.get(0).equals(domain)) {
                    Log.d(LOGTAG,"accepted CN in cert self signed cert for "+domain);
                    return true;
                }
            }
            Collection<List<?>> alternativeNames = certificate.getSubjectAlternativeNames();
            List<String> xmppAddrs = new ArrayList<>();
            List<String> srvNames = new ArrayList<>();
            List<String> domains = new ArrayList<>();
            if (alternativeNames != null) {
                for (List<?> san : alternativeNames) {
                    Integer type = (Integer) san.get(0);
                    if (type == 0) {
                        Pair<String, String> otherName = parseOtherName((byte[]) san.get(1));
                        if (otherName != null) {
                            switch (otherName.first) {
                                case SRVName:
                                    srvNames.add(otherName.second);
                                    break;
                                case xmppAddr:
                                    xmppAddrs.add(otherName.second);
                                    break;
                                default:
                                    Log.d(LOGTAG, "oid: " + otherName.first + " value: " + otherName.second);
                            }
                        }
                    } else if (type == 2) {
                        Object value = san.get(1);
                        if (value instanceof String) {
                            domains.add((String) value);
                        }
                    }
                }
            }
            if (srvNames.size() == 0 && xmppAddrs.size() == 0 && domains.size() == 0) {
                domains.addAll(domains);
            }
            Log.d(LOGTAG, "searching for " + domain + " in srvNames: " + srvNames + " xmppAddrs: " + xmppAddrs + " domains:" + domains);
            if (hostname != null) {
                Log.d(LOGTAG,"also trying to verify hostname "+hostname);
            }
            return xmppAddrs.contains(domain)
                    || srvNames.contains("_xmpp-client." + domain)
                    || matchDomain(domain, domains)
                    || (hostname != null && matchDomain(hostname,domains));
        } catch (Exception e) {
            return false;
        }
    }

    private static List<String> getCommonNames(X509Certificate certificate) {
        List<String> domains = new ArrayList<>();
        try {
            X500Name x500name = new JcaX509CertificateHolder(certificate).getSubject();
            RDN[] rdns = x500name.getRDNs(BCStyle.CN);
            for (int i = 0; i < rdns.length; ++i) {
                domains.add(IETFUtils.valueToString(x500name.getRDNs(BCStyle.CN)[i].getFirst().getValue()));
            }
            return domains;
        } catch (CertificateEncodingException e) {
            return domains;
        }
    }

    private static Pair<String, String> parseOtherName(byte[] otherName) {
        try {
            ASN1Primitive asn1Primitive = ASN1Primitive.fromByteArray(otherName);
            if (asn1Primitive instanceof DERTaggedObject) {
                ASN1Primitive inner = ((DERTaggedObject) asn1Primitive).getObject();
                if (inner instanceof DLSequence) {
                    DLSequence sequence = (DLSequence) inner;
                    if (sequence.size() >= 2 && sequence.getObjectAt(1) instanceof DERTaggedObject) {
                        String oid = sequence.getObjectAt(0).toString();
                        ASN1Primitive value = ((DERTaggedObject) sequence.getObjectAt(1)).getObject();
                        if (value instanceof DERUTF8String) {
                            return new Pair<>(oid, ((DERUTF8String) value).getString());
                        } else if (value instanceof DERIA5String) {
                            return new Pair<>(oid, ((DERIA5String) value).getString());
                        }
                    }
                }
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean matchDomain(String needle, List<String> haystack) {
        for (String entry : haystack) {
            if (entry.startsWith("*.")) {
                int i = needle.indexOf('.');
                Log.d(LOGTAG, "comparing " + needle.substring(i) + " and " + entry.substring(1));
                if (i != -1 && needle.substring(i).equals(entry.substring(1))) {
                    Log.d(LOGTAG, "domain " + needle + " matched " + entry);
                    return true;
                }
            } else {
                if (entry.equals(needle)) {
                    Log.d(LOGTAG, "domain " + needle + " matched " + entry);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSelfSigned(X509Certificate certificate) {
        try {
            certificate.verify(certificate.getPublicKey());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean verify(String domain, SSLSession sslSession) {
        return verify(domain,null,sslSession);
    }

}
