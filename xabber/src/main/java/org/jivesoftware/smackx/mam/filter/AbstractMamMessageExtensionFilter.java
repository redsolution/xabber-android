/**
 *
 * Copyright © 2015 Florian Schmaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.mam.filter;

import org.jivesoftware.smack.filter.FlexibleStanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.mam.packet.MamPacket.AbstractMamExtension;
import org.jivesoftware.smackx.mam.packet.MamQueryIQ;

public abstract class AbstractMamMessageExtensionFilter extends
        FlexibleStanzaTypeFilter<Message> {

    private final String queryId;

    public AbstractMamMessageExtensionFilter(MamQueryIQ mamQueryIQ) {
        super(Message.class);
        this.queryId = mamQueryIQ.getQueryId();
    }

    @Override
    protected boolean acceptSpecific(Message message) {
        AbstractMamExtension mamExtension = getMamExtension(message);
        if (mamExtension == null) {
            return false;
        }
        String resultQueryId = mamExtension.getQueryId();
        if (queryId == null && resultQueryId == null) {
            return true;
        } else if (queryId != null && queryId.equals(resultQueryId)) {
            return true;
        }
        return false;
    }

    protected abstract AbstractMamExtension getMamExtension(Message message);
}
