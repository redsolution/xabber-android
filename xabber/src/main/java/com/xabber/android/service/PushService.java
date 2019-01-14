package com.xabber.android.service;

import android.util.Base64;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;

import java.util.Map;

public class PushService extends FirebaseMessagingService {

    private static final String FIELD_TARGET_TYPE = "target_type";
    private static final String FIELD_TARGET = "target";
    private static final String FIELD_BODY = "body";

    private static final String ACTION_SETTINGS_UPDATED = "settings_updated";
    private static final String ACTION_ACCOUNT_UPDATED = "account_updated";
    private static final String TARGET_TYPE_XACCOUNT = "xaccount";
    private static final String TARGET_TYPE_NODE = "node";

    Gson gson = new Gson();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            String targetType = data.get(FIELD_TARGET_TYPE);
            String target = data.get(FIELD_TARGET);
            String encodedBody = data.get(FIELD_BODY);

            if (targetType != null && target != null && encodedBody != null) {
                String decodedBody = new String(Base64.decode(encodedBody, Base64.NO_WRAP));

                switch (targetType) {
                    case TARGET_TYPE_XACCOUNT:
                        onXAccountPushReceived(target, decodedBody);
                        break;
                    case TARGET_TYPE_NODE:
                        onXMPPPushReceived(target, decodedBody);
                        break;
                    default:
                        Log.d(PushService.class.getSimpleName(), "Unexpected target type - " + targetType);
                }
            }
        }
    }

    private void onXAccountPushReceived(String target, String body) {
        XabberAccount xabberAccount = XabberAccountManager.getInstance().getAccount();
        if (xabberAccount != null && xabberAccount.getFullUsername().equals(target)) {
            XAccountPushData data = gson.fromJson(body, XAccountPushData.class);

            if (data != null && xabberAccount.getFullUsername().equals(data.getUsername())
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
    }

    private void onXMPPPushReceived(String target, String body) {
        /** Will be used for XMPP pushes */
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
}
