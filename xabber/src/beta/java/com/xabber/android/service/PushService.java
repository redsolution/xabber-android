package com.xabber.android.service;

import android.util.Base64;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.xabber.android.data.push.PushManager;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;

import java.util.Map;

public class PushService extends FirebaseMessagingService {

    private static final String FIELD_TARGET_TYPE = "target_type";
    private static final String FIELD_BODY = "body";

    private static final String ACTION_SETTINGS_UPDATED = "settings_updated";
    private static final String ACTION_ACCOUNT_UPDATED = "account_updated";
    private static final String ACTION_REGJID = "regjid";
    private static final String ACTION_MESSAGE = "message";
    private static final String TARGET_TYPE_XACCOUNT = "xaccount";
    private static final String TARGET_TYPE_NODE = "node";
    private static final String REGJID_SUCCESS_RESULT = "success";

    Gson gson = new Gson();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            String targetType = data.get(FIELD_TARGET_TYPE);
            String encodedBody = data.get(FIELD_BODY);

            if (targetType != null && encodedBody != null) {
                String decodedBody = new String(Base64.decode(encodedBody, Base64.NO_WRAP));

                switch (targetType) {
                    case TARGET_TYPE_XACCOUNT:
                        onXAccountPushReceived(decodedBody);
                        break;
                    case TARGET_TYPE_NODE:
                        onXMPPPushReceived(decodedBody);
                        break;
                    default:
                        Log.d(PushService.class.getSimpleName(), "Unexpected target type - " + targetType);
                }
            }
        }
    }

    private void onXAccountPushReceived(String body) {
        XAccountPushData data = gson.fromJson(body, XAccountPushData.class);
        XabberAccount xabberAccount = XabberAccountManager.getInstance().getAccount();

        if (xabberAccount != null && data != null
                && xabberAccount.getFullUsername().equals(data.getUsername())
                && !xabberAccount.getToken().equals(data.getFromToken())) {

            switch (data.getAction()) {
                case ACTION_SETTINGS_UPDATED:
                    XabberAccountManager.getInstance().updateAccountSettings();
                    // used async function updateAccountSettings
                    // inside function exist check that prevents simultaneous calls
                    break;
                case ACTION_ACCOUNT_UPDATED:
                    XabberAccountManager.getInstance().updateAccountInfo();
                    break;
                default:
                    Log.d(PushService.class.getSimpleName(),
                            "Unexpected action in Xabber Account push - " + data.getAction());
            }
        }
    }

    private void onXMPPPushReceived(String body) {
        EndpointRegPushData data = gson.fromJson(body, EndpointRegPushData.class);
        if (data != null) {
            switch (data.getAction()) {
                case ACTION_REGJID:
                    if (REGJID_SUCCESS_RESULT.equals(data.getResult()))
                        PushManager.getInstance().onEndpointRegistered(data.getJid(), data.getService(), data.getNode());
                    break;
                case ACTION_MESSAGE:
                    PushManager.getInstance().onNewMessagePush(this, data.getNode());
                    break;
                default:
                    Log.d(PushService.class.getSimpleName(),
                            "Unexpected action in Node push - " + data.getAction());
            }
        }
    }

    private static class XAccountPushData {
        private final String action;
        private final String username;
        private final String from_token;

        public XAccountPushData(String action, String username, String from_token) {
            this.action = action;
            this.username = username;
            this.from_token = from_token;
        }

        public String getAction() {
            return action;
        }

        public String getUsername() {
            return username;
        }

        public String getFromToken() {
            return from_token;
        }
    }

    private static class EndpointRegPushData {
        private final String action;
        private final String result;
        private final String jid;
        private final String node;
        private final String service;

        public EndpointRegPushData(String action, String result, String jid, String node, String service) {
            this.action = action;
            this.result = result;
            this.jid = jid;
            this.node = node;
            this.service = service;
        }

        public String getAction() {
            return action;
        }

        public String getResult() {
            return result;
        }

        public String getJid() {
            return jid;
        }

        public String getNode() {
            return node;
        }

        public String getService() {
            return service;
        }
    }
}
