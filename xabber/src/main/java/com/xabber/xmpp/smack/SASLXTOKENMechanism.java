package com.xabber.xmpp.smack;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.sasl.SASLMechanism;

import javax.security.auth.callback.CallbackHandler;

public class SASLXTOKENMechanism extends SASLMechanism {

    public static final String NAME = "X-TOKEN";

    @Override
    protected void authenticateInternal(CallbackHandler cbh) throws SmackException {
        throw new UnsupportedOperationException("CallbackHandler not (yet) supported");
    }

    @Override
    protected byte[] getAuthenticationText() throws SmackException {
        // Note that base64 encoding is done in SASLMechanism for the bytes return by getAuthenticationText().
        return toBytes('\u0000' + authenticationId + '\u0000' + password);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPriority() {
        return 420;
    }

    @Override
    public void checkIfSuccessfulOrThrow() throws SmackException {

    }

    @Override
    protected SASLMechanism newInstance() {
        return new SASLXTOKENMechanism();
    }
}
