package com.xabber.android.data.connection;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.initializer.SmackInitializer;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.dns.DNSResolver;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jivesoftware.smack.util.dns.SRVRecord;

import org.xbill.DNS.ExtLookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by valery.miller on 12.05.17.
 */

public class ExtDNSJavaResolver extends DNSResolver implements SmackInitializer {

    private static ExtDNSJavaResolver instance = new ExtDNSJavaResolver();

    public static DNSResolver getInstance() {
        return instance;
    }

    public ExtDNSJavaResolver() {
        super(false);
    }

    @Override
    protected List<SRVRecord> lookupSRVRecords0(String name, List<HostAddress> failedAddresses, ConnectionConfiguration.DnssecMode dnssecMode) {
        List<SRVRecord> res = new ArrayList<SRVRecord>();

        ExtLookup lookup;
        try {
            lookup = new ExtLookup(name, Type.SRV);
        }
        catch (TextParseException e) {
            throw new IllegalStateException(e);
        }

        Record[] recs = lookup.run();
        if (recs == null)
            return res;

        for (Record record : recs) {
            org.xbill.DNS.SRVRecord srvRecord = (org.xbill.DNS.SRVRecord) record;
            if (srvRecord != null && srvRecord.getTarget() != null) {
                String host = srvRecord.getTarget().toString();
                int port = srvRecord.getPort();
                int priority = srvRecord.getPriority();
                int weight = srvRecord.getWeight();

                List<InetAddress> hostAddresses = lookupHostAddress0(host, failedAddresses, dnssecMode);
                if (hostAddresses == null) {
                    continue;
                }

                SRVRecord r = new SRVRecord(host, port, priority, weight, hostAddresses);
                res.add(r);
            }
        }

        return res;
    }

    public static void setup() {
        DNSUtil.setDNSResolver(getInstance());
    }

    @Override
    public List<Exception> initialize() {
        setup();
        return null;
    }

}
