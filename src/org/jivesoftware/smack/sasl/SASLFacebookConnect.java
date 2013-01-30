package org.jivesoftware.smack.sasl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.harmony.javax.security.auth.callback.CallbackHandler;
import de.measite.smack.Sasl;

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.util.Base64;

/**
 * This class is originally from http://code.google.com/p/fbgc/source/browse/trunk/daemon/src/main/java/org/albino/mechanisms/FacebookConnectSASLMechanism.java
 * I just adapted to match the SMACK package scheme and 
 */
public class SASLFacebookConnect extends SASLMechanism {

        private String sessionKey = "";
        private String sessionSecret = "";
        private String apiKey = "";
        
        static{
        	SASLAuthentication.registerSASLMechanism("X-FACEBOOK-PLATFORM",
                    SASLFacebookConnect.class);
        	SASLAuthentication.supportSASLMechanism("X-FACEBOOK-PLATFORM", 0);
        }
        
        public SASLFacebookConnect(SASLAuthentication saslAuthentication) {
                super(saslAuthentication);
        }

        // protected void authenticate() throws IOException, XMPPException {
        // String[] mechanisms = { getName() };
        // Map<String, String> props = new HashMap<String, String>();
        // sc = Sasl.createSaslClient(mechanisms, null, "xmpp", hostname, props,
        // this);
        //
        // super.authenticate();
        // }

        protected void authenticate() throws IOException, XMPPException {
                final StringBuilder stanza = new StringBuilder();
                stanza.append("<auth mechanism=\"").append(getName());
                stanza.append("\" xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
                stanza.append("</auth>");

                // Send the authentication to the server
                getSASLAuthentication().send(new Packet(){

					@Override
					public String toXML() {
						return stanza.toString();
					}    	
                	
                });
        }

        public void authenticate(String apiKeyAndSessionKey, String host, String sessionSecret)
                        throws IOException, XMPPException {

                if(apiKeyAndSessionKey==null || sessionSecret==null)
                        throw new IllegalStateException("Invalid parameters!");
                
                String[] keyArray = apiKeyAndSessionKey.split("\\|");
                
                if(keyArray==null || keyArray.length != 2)
                        throw new IllegalStateException("Api key or session key is not present!");
                
                this.apiKey = keyArray[0];
                this.sessionKey = keyArray[1];
                this.sessionSecret = sessionSecret;
                
                this.authenticationId = sessionKey;
                this.password = sessionSecret;
                this.hostname = host;

                String[] mechanisms = { "DIGEST-MD5" };
                Map<String, String> props = new HashMap<String, String>();
                sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, this);
                authenticate();
        }

        public void authenticate(String username, String host, CallbackHandler cbh)
                        throws IOException, XMPPException {
                String[] mechanisms = { "DIGEST-MD5" };
                Map<String, String> props = new HashMap<String, String>();
                sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, cbh);
                authenticate();
        }

        protected String getName() {
                return "X-FACEBOOK-PLATFORM";
        }

        public void challengeReceived(String challenge) throws IOException {
                // Build the challenge response stanza encoding the response text
                final StringBuilder stanza = new StringBuilder();

                byte response[] = null;
                if (challenge != null) {
                        String decodedResponse = new String(Base64.decode(challenge));
                        Map<String, String> parameters = getQueryMap(decodedResponse);

                        String version = "1.0";
                        String nonce = parameters.get("nonce");
                        String method = parameters.get("method");
                        
                        Long callId = new GregorianCalendar().getTimeInMillis()/1000;
                        
                        String sig = "api_key="+apiKey
                                                        +"call_id="+callId
                                                        +"method="+method
                                                        +"nonce="+nonce
                                                        +"session_key="+sessionKey
                                                        +"v="+version
                                                        +sessionSecret;
                        
                        try {
                                sig = MD5(sig);
                        } catch (NoSuchAlgorithmException e) {
                                throw new IllegalStateException(e);
                        }
                        
                        String composedResponse = "api_key="+apiKey+"&"
                                                                                +"call_id="+callId+"&"
                                                                                +"method="+method+"&"
                                                                                +"nonce="+nonce+"&"
                                                                                +"session_key="+sessionKey+"&"
                                                                                +"v="+version+"&"
                                                                                +"sig="+sig;
                        
                        response = composedResponse.getBytes();
                }

                String authenticationText="";

                if (response != null) {
                        authenticationText = Base64.encodeBytes(response, Base64.DONT_BREAK_LINES);
                }

                stanza.append("<response xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
                stanza.append(authenticationText);
                stanza.append("</response>");

                // Send the authentication to the server
                getSASLAuthentication().send(new Packet(){

					@Override
					public String toXML() {
						return stanza.toString();
					}
                	
                });
        }

        private Map<String, String> getQueryMap(String query) {
                String[] params = query.split("&");
                Map<String, String> map = new HashMap<String, String>();
                for (String param : params) {
                        String name = param.split("=")[0];
                        String value = param.split("=")[1];
                        map.put(name, value);
                }
                return map;
        }
        
    private String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        }
        return buf.toString();
    }
 
    public String MD5(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException  {
        MessageDigest md;
        md = MessageDigest.getInstance("MD5");
        byte[] md5hash = new byte[32];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        md5hash = md.digest();
        return convertToHex(md5hash);
    }
}

