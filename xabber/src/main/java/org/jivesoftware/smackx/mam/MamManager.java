/**
 * Copyright © 2015 Florian Schmaus
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.mam;

import android.support.annotation.Nullable;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.mam.filter.MamMessageResultFilter;
import org.jivesoftware.smackx.mam.packet.MamFinIQ;
import org.jivesoftware.smackx.mam.packet.MamPacket;
import org.jivesoftware.smackx.mam.packet.MamQueryIQ;
import org.jivesoftware.smackx.mam.packet.MamResultExtension;
import org.jivesoftware.smackx.rsm.packet.RSMSet;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.util.XmppDateTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;


/**
 * Implements MAM - Message Archive Management.
 *
 *
 * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313: Message Archive Management</a>
 *
 * The implementation is now according to version 0.3.
 *
 */
public class MamManager extends Manager {

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    private static final Map<XMPPConnection, MamManager> INSTANCES = new WeakHashMap<>();

    public static synchronized MamManager getInstanceFor(XMPPConnection connection) {
        MamManager mamManager = INSTANCES.get(connection);
        if (mamManager == null) {
            mamManager = new MamManager(connection);
            INSTANCES.put(connection, mamManager);
        }
        return mamManager;
    }

    private MamManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(MamPacket.NAMESPACE);
    }

    /**
     * Query the Object, identified by domain, using the following optional predefined filters:
     *
     * <ul>
     * <li>start - Filtering by time received</li>
     * <li>end - Filtering by time received</li>
     * <li>with - Filtering by JID</li>
     * <li>max - limit to the number of results transmitted at a time.</li>
     * </ul>
     *
     * @param domain a jid specifying a user or room or an other entity.  When null the users own JID will be used.
     * @param max limit to the number of results transmitted at a time. Can be null.
     * @param start used to filter out messages before a certain date/time. Can be null.
     * @param end used to exclude from the results messages after a certain point in time. Can be null.
     * @param withJid a JID against which to match messages. Can be null.
     * @return a result set with a list of results
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public MamQueryResult queryArchive(String domain, Integer max, Date start, Date end, String withJid) throws NoResponseException,
            XMPPErrorException, NotConnectedException, InterruptedException {
        return queryArchive(domain, null, max, start, end, withJid, false);
    }

    public MamQueryResult queryArchiveLast(Integer max, String withJid) throws NoResponseException,
            XMPPErrorException, NotConnectedException, InterruptedException {
        return queryArchive(null, null, max, null, null, withJid, true);
    }

    /**
     * Query the Object, identified by domain and node, using the following optional predefined filters:
     *
     * <ul>
     * <li>start - Filtering by time received</li>
     * <li>end - Filtering by time received</li>
     * <li>with - Filtering by JID</li>
     * <li>max - limit to the number of results transmitted at a time.</li>
     * </ul>
     *
     * @param domain a jid specifying a user or room or an other entity.  When null the users own JID will be used.
     * @param node
     * @param max limit to the number of results transmitted at a time. Can be null.
     * @param start used to filter out messages before a certain date/time. Can be null.
     * @param end used to exclude from the results messages after a certain point in time. Can be null.
     * @param withJid a JID against which to match messages. Can be null.
     * @return a result set with a list of results
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public MamQueryResult queryArchive(String domain, String node, Integer max, Date start, Date end, String withJid, boolean last) throws NoResponseException,
            XMPPErrorException, NotConnectedException, InterruptedException {
        DataForm dataForm = createDataForm(start, end, withJid);

        String queryId = UUID.randomUUID().toString();

        MamQueryIQ mamQueryIQ = new MamQueryIQ(queryId, node, dataForm);
        mamQueryIQ.setType(IQ.Type.set);
        mamQueryIQ.setTo(domain);
        if (max != null) {
            RSMSet rsmSet;
            if (last) {
                rsmSet = new RSMSet(null, "", -1, -1, null, max, null, -1);
            } else {
                rsmSet = new RSMSet(max);
            }

            mamQueryIQ.addExtension(rsmSet);
        }
        return queryArchive(mamQueryIQ, 0);
    }

    public MamQueryResult queryPage(String withJid, int max, String after, String before) throws XMPPErrorException, NotConnectedException, InterruptedException, NoResponseException {
        DataForm dataForm = createDataForm(null, null, withJid);
        String queryId = UUID.randomUUID().toString();
        MamQueryIQ mamQueryIQ = new MamQueryIQ(queryId, null, dataForm);
        mamQueryIQ.setType(IQ.Type.set);
        mamQueryIQ.addExtension(new RSMSet(after, before, -1, -1, null, max, null, -1));
        return queryArchive(mamQueryIQ, 0);
    }

    @Nullable
    private DataForm createDataForm(Date start, Date end, String withJid) {
        DataForm dataForm = null;
        if (start != null || end != null || withJid != null) {
            dataForm = getNewMamForm();
            if (start != null) {
                FormField formField = new FormField("start");
                formField.addValue(XmppDateTime.formatXEP0082Date(start));
                dataForm.addField(formField);
            }
            if (end != null) {
                FormField formField = new FormField("end");
                formField.addValue(XmppDateTime.formatXEP0082Date(end));
                dataForm.addField(formField);
            }
            if (withJid != null) {
                FormField formField = new FormField("with");
                formField.addValue(withJid);
                dataForm.addField(formField);
            }
        }
        return dataForm;
    }

    /**
     * Query the Object, identified by domain, using the the filters as specified in the answerform.
     *
     * @param domain a jid specifying a user or room or an other entity.  When null the users own JID will be used.
     * @param max limit to the number of results transmitted at a time. Can be null.
     * @return a result set with a list of results
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public MamQueryResult queryArchive(String objectJID, Form answerForm, Integer max) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return queryArchive(objectJID, null, answerForm, max);
    }


    /**
     * Query the Object, identified by domain and node, using the the filters as specified in the answerform.
     *
     * @param domain a jid specifying a user or room or an other entity.  When null the users own JID will be used.
     * @param node
     * @param max limit to the number of results transmitted at a time. Can be null.
     * @return a result set with a list of results
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public MamQueryResult queryArchive(String objectJID, String node, Form answerForm, Integer max) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {

        String queryId = UUID.randomUUID().toString();
        MamQueryIQ mamQueryIQ = new MamQueryIQ(queryId, node, answerForm.getDataFormToSend());
        mamQueryIQ.setType(IQ.Type.set);
        mamQueryIQ.setTo(objectJID);
        if (max != null) {
            RSMSet rsmSet = new RSMSet(max);
            mamQueryIQ.addExtension(rsmSet);
        }
        return queryArchive(mamQueryIQ, 0);
    }

    /**
     * Query for the next result set, following on the result set as described by mamQueryResult.
     *
     * @param mamQueryResult
     * @param count limit to the number of results in the result set.
     * @return a result set with a list of results
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public MamQueryResult pageNext(MamQueryResult mamQueryResult, int count) throws NoResponseException,
            XMPPErrorException, NotConnectedException, InterruptedException {
        RSMSet previousResultRsmSet = mamQueryResult.mamFin.getRsmSet();
        RSMSet requestRsmSet = new RSMSet(count, previousResultRsmSet.getLast(), RSMSet.PageDirection.after);
        return page(mamQueryResult, requestRsmSet);
    }

    public MamQueryResult pageBefore(MamQueryResult mamQueryResult, int count) throws NoResponseException,
            XMPPErrorException, NotConnectedException, InterruptedException {
        RSMSet previousResultRsmSet = mamQueryResult.mamFin.getRsmSet();
        RSMSet requestRsmSet = new RSMSet(count, previousResultRsmSet.getFirst(), RSMSet.PageDirection.before);
        return page(mamQueryResult, requestRsmSet);
    }

    /**
     * Query for a result set, based on the query defined by the result set as described by mamQueryResult, but constrained by the Result Set in rsmSet.
     *
     * @param mamQueryResult the query
     * @param rsmSet a Result set constrain.
     * @return a result set with a list of results
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public MamQueryResult page(MamQueryResult mamQueryResult, RSMSet rsmSet) throws NoResponseException,
            XMPPErrorException, NotConnectedException, InterruptedException {
        MamQueryIQ mamQueryIQ = new MamQueryIQ(UUID.randomUUID().toString(), mamQueryResult.form);
        mamQueryIQ.setType(IQ.Type.set);
        mamQueryIQ.addExtension(rsmSet);
        return queryArchive(mamQueryIQ, 0);
    }

    private MamQueryResult queryArchive(MamQueryIQ mamQueryIq, long extraTimeout) throws NoResponseException,
            XMPPErrorException, NotConnectedException, InterruptedException {
        if (extraTimeout < 0) {
            throw new IllegalArgumentException("extra timeout must be zero or positive");
        }
        final XMPPConnection connection = connection();

        PacketCollector.Configuration resultCollectorConfiguration = PacketCollector.newConfiguration().setStanzaFilter(
                new MamMessageResultFilter(mamQueryIq));
        PacketCollector resultCollector = connection.createPacketCollector(resultCollectorConfiguration);

        MamFinIQ mamFinIQ;
        try {
            mamFinIQ = connection.createPacketCollectorAndSend(mamQueryIq).nextResultOrThrow();
        } finally {
            resultCollector.cancel();
        }
        List<Forwarded> messages = new ArrayList<>(resultCollector.getCollectedCount());
        for (Message resultMessage = resultCollector.pollResult(); resultMessage != null; resultMessage = resultCollector.pollResult()) {
//            // XEP-313 § 4.2
            MamResultExtension mamResultExtension = MamResultExtension.from(resultMessage);
            messages.add(mamResultExtension.getForwarded());
        }
        return new MamQueryResult(messages, mamFinIQ, DataForm.from(mamQueryIq));
    }

    public static class MamQueryResult {
        public final List<Forwarded> messages;
        public final MamFinIQ mamFin;
        private final DataForm form;

        private MamQueryResult(List<Forwarded> messages, MamFinIQ mamFin, DataForm form) {
            this.messages = messages;
            this.mamFin = mamFin;
            this.form = form;
        }
    }

    /**
     * Returns true if Message Archive Management is supported by the server.
     *
     * @return true if Message Archive Management is supported by the server.
     * @throws NotConnectedException
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws InterruptedException
     */
    public boolean isSupportedByServer() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).serverSupportsFeature(MamPacket.NAMESPACE);
    }

    private static DataForm getNewMamForm() {
        FormField field = new FormField(FormField.FORM_TYPE);
        field.setType(FormField.Type.hidden);
        field.addValue(MamPacket.NAMESPACE);
        DataForm form = new DataForm(DataForm.Type.submit);
        form.addField(field);
        return form;
    }

    /**
     * Find out about additional filters the server might support. Filters are specified in a <a href="http://xmpp.org/extensions/xep-0004.html">Data Forms (XEP-0004)</a>.
     *
     * @param objectJID
     * @return
     * @throws NotConnectedException
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws InterruptedException
     */
    public Form getSearchForm(String objectJID) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return getSearchForm(objectJID, null);
    }

    /**
     * Find out about additional filters the server might support. Filters are specified in a <a href="http://xmpp.org/extensions/xep-0004.html">Data Forms (XEP-0004)</a>.
     *
     * @param domain
     * @param node
     * @return
     * @throws NotConnectedException
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws InterruptedException
     */
    public Form getSearchForm(String domain, String node) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MamQueryIQ search = new MamQueryIQ();
        search.setType(IQ.Type.get);
        search.setTo(domain);
        search.setNode(node);

        IQ response = (IQ) connection().createPacketCollectorAndSend(search).nextResultOrThrow();
        return Form.getFormFrom(response);
    }

}
