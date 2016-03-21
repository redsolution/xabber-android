package org.jivesoftware.smackx.mam.packet;

import org.jivesoftware.smack.packet.ExtensionElement;

public abstract class AbstractMamExtension implements ExtensionElement {
    public final String queryId;

    protected AbstractMamExtension(String queryId) {
        this.queryId = queryId;
    }

    public final String getQueryId() {
        return queryId;
    }


    @Override
    public final String getNamespace() {
        return MamPacket.NAMESPACE;
    }

}
