package com.xabber.xmpp.smack;

import com.xabber.android.data.log.LogManager;

import org.jivesoftware.smack.sasl.SASLMechanism;

import javax.security.auth.callback.CallbackHandler;

public class SASLXTOKENMechanism extends SASLMechanism {

    public static final String NAME = "X-TOKEN";

    @Override
    protected void authenticateInternal(CallbackHandler cbh) {
        throw new UnsupportedOperationException("CallbackHandler not (yet) supported");
    }

    @Override
    protected byte[] getAuthenticationText() {
        // Note that base64 encoding is done in SASLMechanism for the bytes return by getAuthenticationText().
        LogManager.d(this.getClass().getSimpleName(), "getAuthenticatedText() with password: " + password);
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
    public void checkIfSuccessfulOrThrow() { }

    @Override
    protected SASLMechanism newInstance() {
        return new SASLXTOKENMechanism();
    }

}
