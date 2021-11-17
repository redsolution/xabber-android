package com.xabber.xmpp.smack;

import org.jivesoftware.smack.sasl.SASLMechanism;

import javax.security.auth.callback.CallbackHandler;

public class SaslHtopMechanism extends SASLMechanism {

    public static final String NAME = "HOTP";

    @Override
    protected void authenticateInternal(CallbackHandler cbh) {
        throw new UnsupportedOperationException("CallbackHandler not (yet) supported");
    }

    @Override
    protected byte[] getAuthenticationText() {
        // Note that base64 encoding is done in SASLMechanism for the bytes return by getAuthenticationText().
        return ('\u0000' + authenticationId + '\u0000' + password).getBytes();
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
    public void checkIfSuccessfulOrThrow() { }

    @Override
    protected SASLMechanism newInstance() {
        return new SaslHtopMechanism();
    }

}
