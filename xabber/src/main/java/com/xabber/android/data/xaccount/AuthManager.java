package com.xabber.android.data.xaccount;

import android.util.Base64;

import com.xabber.android.BuildConfig;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.privatestorage.PrivateStorageManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.utils.ExternalAPIs;

import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import rx.Single;

/**
 * Created by valery.miller on 17.07.17.
 */

public class AuthManager {

    public static final String PROVIDER_TWITTER = "twitter";
    public static final String PROVIDER_GOOGLE = "google";
    private static final String LOG_TAG = AuthManager.class.getSimpleName();
    private static final String SOURCE_NAME = "Xabber Android";

    public static Single<XAccountTokenDTO> login(String login, String pass) {
        SettingsManager.setSyncAllAccounts(true);

        String credentials = login + ":" + pass;
        byte[] data = credentials.getBytes();
        String encodedCredentials = Base64.encodeToString(data, Base64.NO_WRAP);

        return HttpApiManager.getXabberApi()
                .login("Basic " + encodedCredentials, new Source(getSource()));
    }

    public static Single<ResponseBody> logout() {

        return HttpApiManager.getXabberApi().logout(getXabberTokenHeader())
                .flatMap(responseBody -> {
                    if (XabberAccountManager.getInstance().deleteXabberAccountFromRealm())
                        return Single.just(responseBody);
                    else
                        return Single.error(new Throwable("Realm: xabber account deletion error"));
                })
                .flatMap(responseBody -> {
                    if (XabberAccountManager.getInstance().deleteSyncStatesFromRealm())
                        return Single.just(responseBody);
                    else
                        return Single.error(new Throwable("Realm: sync states deletion error"));
                })
                .flatMap(responseBody -> {
                    String token = ExternalAPIs.getPushEndpointToken();
                    if (token != null)
                        return unregisterFCMEndpoint(token);
                    else return Single.just(responseBody);
                });
    }

    public static Single<XAccountTokenDTO> loginSocial(String provider, String credentials) {
        SettingsManager.setSyncAllAccounts(true);
        return HttpApiManager.getXabberApi().loginSocial(new SocialAuthRequest(provider,
                credentials, getSource()));
    }

    public static Single<XabberAccount> getAccount(final String token) {
        return HttpApiManager.getXabberApi().getAccount("Token " + token)
                .flatMap(xabberAccountDTO -> {
                    if (xabberAccountDTO.getLanguage() != null
                            && !xabberAccountDTO.getLanguage().equals(""))
                        return Single.just(xabberAccountDTO);
                    else return updateAccount(token, new Account(xabberAccountDTO.getFirstName(),
                            xabberAccountDTO.getLastName(), Locale.getDefault().getLanguage()));
                })
                .flatMap(xabberAccountDTO -> XabberAccountManager.getInstance()
                        .saveOrUpdateXabberAccountToRealm(xabberAccountDTO, token));
    }

    private static Single<XabberAccountDTO> updateAccount(final String token, Account account) {
        return HttpApiManager.getXabberApi().updateAccount("Token " + token, account);
    }

    public static Single<List<XMPPAccountSettings>> getClientSettings() {
        return HttpApiManager.getXabberApi().getClientSettings(getXabberTokenHeader())
                .flatMap(listClientSettingsDTO -> XabberAccountManager.getInstance()
                        .clientSettingsDTOListToPOJO(listClientSettingsDTO))
                .flatMap(xmppAccounts -> {
                    // add only new accounts from server to sync map
                    Map<String, Boolean> syncState = new HashMap<>();
                    for (XMPPAccountSettings account : xmppAccounts) {
                        if (XabberAccountManager.getInstance()
                                .getAccountSyncState(account.getJid()) == null)
                            syncState.put(account.getJid(), true);
                    }
                    XabberAccountManager.getInstance().setAccountSyncState(syncState);

                    if (AccountManager.getInstance().isLoaded()) {
                        // update last synchronization time
                        SettingsManager.setLastSyncDate(XabberAccountManager.getCurrentTimeString());

                        return XabberAccountManager.getInstance().updateLocalAccounts(xmppAccounts);
                    } else {
                        return Single.just(xmppAccounts);
                    }
                });
    }

    public static Single<List<XMPPAccountSettings>> patchClientSettings(
            List<XMPPAccountSettings> accountSettingsList) {
        List<OrderDTO> listOrder = new ArrayList<>();
        List<ClientSettingsDTO> listSettings = new ArrayList<>();

        // divide all data into two lists: settings and orders
        for (XMPPAccountSettings account : accountSettingsList) {
            listSettings.add(new ClientSettingsDTO(account.getJid(),
                    new SettingsValuesDTO(account.getOrder(), account.getColor(),
                            account.getToken(), account.getUsername()), account.getTimestamp()));

            if (account.getOrder() > 0)
                listOrder.add(new OrderDTO(account.getJid(), account.getOrder()));
        }

        // prepare dto for settings
        ClientSettingsWithoutOrderDTO settingsDTO = new ClientSettingsWithoutOrderDTO(listSettings);

        // prepare dto for orders
        OrderDataDTO orderDataDTO = new OrderDataDTO(listOrder,
                XabberAccountManager.getInstance().getLastOrderChangeTimestamp());
        final ClientSettingsOrderDTO orderDTO = new ClientSettingsOrderDTO(orderDataDTO);

        // patch settings to server
        return HttpApiManager.getXabberApi()
                .updateClientSettings(getXabberTokenHeader(), settingsDTO)
                .flatMap(listClientSettingsDTO -> {
                    // patch orders to server
                    return HttpApiManager.getXabberApi()
                            .updateClientSettings(getXabberTokenHeader(), orderDTO);
                })
                .flatMap(listClientSettingsDTO -> {
                    // convert dto to pojo
                    return XabberAccountManager.getInstance()
                            .clientSettingsDTOListToPOJO(listClientSettingsDTO);
                })
                .flatMap(xmppAccounts -> {
                    // add only new accounts from server to sync map
                    Map<String, Boolean> syncState = new HashMap<>();
                    for (XMPPAccountSettings account : xmppAccounts) {
                        if (XabberAccountManager.getInstance().getAccountSyncState(account.getJid())
                                == null) {
                            syncState.put(account.getJid(), true);
                        }
                    }
                    XabberAccountManager.getInstance().setAccountSyncState(syncState);

                    // update last synchronization time
                    SettingsManager.setLastSyncDate(XabberAccountManager.getCurrentTimeString());

                    // update local accounts
                    return XabberAccountManager.getInstance().updateLocalAccounts(xmppAccounts);
                });
    }

    public static Single<List<XMPPAccountSettings>> deleteClientSettings(String jid) {
        // delete settings from server
        return HttpApiManager.getXabberApi()
                .deleteClientSettings(getXabberTokenHeader(), new Jid(jid))
                .flatMap(listClientSettingsDTO -> {
                    // convert dto to pojo
                    return XabberAccountManager.getInstance()
                            .clientSettingsDTOListToPOJO(listClientSettingsDTO);
                })
                .flatMap(xmppAccounts -> {
                    // add only new accounts from server to sync map
                    Map<String, Boolean> syncState = new HashMap<>();
                    for (XMPPAccountSettings account : xmppAccounts) {
                        if (XabberAccountManager.getInstance()
                                .getAccountSyncState(account.getJid()) == null) {
                            syncState.put(account.getJid(), true);
                        }
                    }
                    XabberAccountManager.getInstance().setAccountSyncState(syncState);

                    // update last synchronization time
                    SettingsManager.setLastSyncDate(XabberAccountManager.getCurrentTimeString());

                    // update local accounts
                    return XabberAccountManager.getInstance().updateLocalAccounts(xmppAccounts);
                });
    }

    public static Single<XAccountTokenDTO> signup(String email) {
        SettingsManager.setSyncAllAccounts(true);
        return HttpApiManager.getXabberApi().signup(new Email(email, getSource()));
    }

    public static Single<XabberAccount> confirmEmail(String code) {
        return HttpApiManager.getXabberApi().confirmEmail(getXabberTokenHeader(), new Code(code))
                .flatMap(xabberAccountDTO -> XabberAccountManager.getInstance()
                        .saveOrUpdateXabberAccountToRealm(xabberAccountDTO, getXabberToken()));
    }

    public static Single<XabberAccount> confirmEmailWithKey(String key) {
        return HttpApiManager.getXabberApi().confirmEmail(new Key(key, getSource()))
                .flatMap(xabberAccountDTO -> XabberAccountManager.getInstance()
                        .saveOrUpdateXabberAccountToRealm(xabberAccountDTO, getXabberToken()));
    }

    public static Single<ResponseBody> addEmail(String email) {
        return HttpApiManager.getXabberApi().addEmail(getXabberTokenHeader(),
                new Email(email, getSource()));
    }

    public static Single<ResponseBody> deleteEmail(int emailId) {
        return HttpApiManager.getXabberApi().deleteEmail(getXabberTokenHeader(), emailId);
    }

    // API v2

    public static Single<XMPPCode> requestXMPPCode(String jid) {
        return HttpApiManager.getXabberApi().requestXMPPCode(new XMPPCodeRequest(jid, "iq"));
    }

    public static Single<XabberAccount> confirmXMPP(final String jid, String code) {
        SettingsManager.setSyncAllAccounts(true);
        return HttpApiManager.getXabberApi().confirmXMPP(new CodeConfirm(code, jid))
                .flatMap(xabberAccountDTO -> {
                    LogManager.d(LOG_TAG, "started setXabberAccountBinding...");
                    try {
                        PrivateStorageManager.getInstance()
                                .setXabberAccountBinding(AccountJid.from(jid), true);
                    } catch (XmppStringprepException e) {
                        e.printStackTrace();
                    }

                    return XabberAccountManager.getInstance()
                            .saveOrUpdateXabberAccountToRealm(xabberAccountDTO,
                                    xabberAccountDTO.getToken());
                });
    }

    public static Single<HostResponse> getHosts() {
        return HttpApiManager.getXabberApi().getHosts();
    }

    public static Single<XabberAccount> signupv2(String username, String host, String password,
                                                 String captchaToken) {
        SettingsManager.setSyncAllAccounts(true);
        return HttpApiManager.getXabberApi().signupv2(new SignUpFields(username, host,
                password, captchaToken))
                .flatMap(xabberAccountDTO -> XabberAccountManager.getInstance()
                        .saveOrUpdateXabberAccountToRealm(xabberAccountDTO,
                                xabberAccountDTO.getToken()));
    }

    public static Single<XabberAccount> signupv2(String username, String host, String password,
                                                 String provider, String credentials) {
        SettingsManager.setSyncAllAccounts(true);
        return HttpApiManager.getXabberApi().signupv2(new SignUpFields(username, host,
                password, provider, credentials))
                .flatMap(xabberAccountDTO -> XabberAccountManager.getInstance()
                        .saveOrUpdateXabberAccountToRealm(xabberAccountDTO,
                                xabberAccountDTO.getToken()));
    }

    public static Single<ResponseBody> bindSocial(String provider, String credentials) {
        return HttpApiManager.getXabberApi().bindSocial(getXabberTokenHeader(),
                new SocialAuthRequest(provider, credentials, getSource()));
    }

    public static Single<ResponseBody> unbindSocial(String provider) {
        return HttpApiManager.getXabberApi().unbindSocial(getXabberTokenHeader(),
                new Provider(provider));
    }

    public static Single<ResponseBody> registerFCMEndpoint(String endpoint) {
        return HttpApiManager.getXabberApi().registerFCMEndpoint(getXabberTokenHeader(),
                new Endpoint(endpoint));
    }

    public static Single<ResponseBody> unregisterFCMEndpoint(String endpoint) {
        return HttpApiManager.getXabberApi().unregisterFCMEndpoint(new Endpoint(endpoint));
    }

    public static Single<ResponseBody> changePassword(String oldPass, String pass,
                                                      String passConfirm) {
        return HttpApiManager.getXabberApi().changePassword(getXabberTokenHeader(),
                new ChangePassFields(oldPass, pass, passConfirm));
    }

    public static Single<ResponseBody> requestResetPassword(String email) {
        return HttpApiManager.getXabberApi().requestResetPassword(new ResetPassFields(email));
    }

    // support

    private static String getXabberTokenHeader() {
        return "Token " + getXabberToken();
    }

    private static String getXabberToken() {
        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null)
            return account.getToken();
        else return null;
    }

    private static String getSource() {
        return SOURCE_NAME + " " + BuildConfig.FLAVOR + " " + BuildConfig.VERSION_NAME;
    }

    public static String getProviderName(String provider) {
        switch (provider) {
            case AuthManager.PROVIDER_TWITTER:
                return "Twitter";
            case AuthManager.PROVIDER_GOOGLE:
                return "Google";
            default:
                return "";
        }
    }

    // models

    public static class ChangePassFields {
        final String old_password;
        final String password;
        final String confirm_password;

        public ChangePassFields(String oldPassword, String password, String confirmPassword) {
            this.old_password = oldPassword;
            this.password = password;
            this.confirm_password = confirmPassword;
        }
    }

    public static class ResetPassFields {
        final String email;

        public ResetPassFields(String email) {
            this.email = email;
        }
    }

    public static class Endpoint {
        final String endpoint_key;

        public Endpoint(String endpoint_key) {
            this.endpoint_key = endpoint_key;
        }
    }

    public static class SignUpFields {
        final String username;
        final String host;
        final String password;
        final boolean create_token;
        final String source;
        String captcha_token;
        String provider;
        String credentials;
        String first_name;
        String last_name;
        String language;
        String source_ip;

        public SignUpFields(String username, String host, String password,
                            String captcha_token) {
            this.username = username;
            this.host = host;
            this.password = password;
            this.captcha_token = captcha_token;
            this.create_token = true;
            this.source = getSource();
        }

        public SignUpFields(String username, String host, String password, String provider,
                            String credentials) {
            this.username = username;
            this.host = host;
            this.password = password;
            this.provider = provider;
            this.credentials = credentials;
            this.create_token = true;
            this.source = getSource();
        }
    }

    public static class CodeConfirm {
        final String code;
        final String jid;

        public CodeConfirm(String code, String jid) {
            this.code = code;
            this.jid = jid;
        }
    }

    public static class HostResponse {
        final List<Host> results;

        public HostResponse(List<Host> results) {
            this.results = results;
        }

        public List<Host> getHosts() {
            return results;
        }
    }

    public static class Host {
        final String host;
        final String description;
        final String price;
        final boolean is_free;

        public Host(String host, String description, String price, boolean is_free) {
            this.host = host;
            this.description = description;
            this.price = price;
            this.is_free = is_free;
        }

        public String getHost() {
            return host;
        }

        public String getDescription() {
            return description;
        }

        public String getPrice() {
            return price;
        }

        public boolean isFree() {
            return is_free;
        }
    }

    public static class XMPPCode {
        final String request_id;
        final String api_jid;

        public XMPPCode(String request_id, String api_jid) {
            this.request_id = request_id;
            this.api_jid = api_jid;
        }

        public String getRequestId() {
            return request_id;
        }

        public String getApiJid() {
            return api_jid;
        }
    }

    public static class XMPPCodeRequest {
        final String jid;
        final String type;

        public XMPPCodeRequest(String jid, String type) {
            this.jid = jid;
            this.type = type;
        }
    }

    public static class Account {
        final String first_name;
        final String last_name;
        final String language;

        public Account(String first_name, String last_name, String language) {
            this.first_name = first_name;
            this.last_name = last_name;
            this.language = language;
        }
    }

    public static class Source {
        final String source;

        public Source(String source) {
            this.source = source;
        }
    }

    public static class Jid {
        final String jid;

        public Jid(String jid) {
            this.jid = jid;
        }
    }

    public static class Key {
        final String key;
        final String source;

        public Key(String key, String source) {
            this.key = key;
            this.source = source;
        }
    }

    public static class Code {
        final String code;

        public Code(String code) {
            this.code = code;
        }
    }

    public static class Email {
        final String email;
        final String source;

        public Email(String email, String source) {
            this.email = email;
            this.source = source;
        }
    }

    public static class Provider {
        final String provider;

        public Provider(String provider) {
            this.provider = provider;
        }
    }

    public static class SocialAuthRequest {
        final String provider;
        final String credentials;
        final String source;

        public SocialAuthRequest(String provider, String credentials, String source) {
            this.provider = provider;
            this.credentials = credentials;
            this.source = source;
        }
    }

    public static class AccessToken {
        final String access_token;

        public AccessToken(String access_token) {
            this.access_token = access_token;
        }
    }

    public static class TwitterAccessToken {
        final TwitterTokens access_token;
        final String secret;
        final String key;

        public TwitterAccessToken(TwitterTokens access_token, String secret, String key) {
            this.access_token = access_token;
            this.secret = secret;
            this.key = key;
        }
    }

    public static class TwitterTokens {
        final String oauth_token_secret;
        final String oauth_token;

        public TwitterTokens(String oauth_token_secret, String oauth_token) {
            this.oauth_token_secret = oauth_token_secret;
            this.oauth_token = oauth_token;
        }
    }

    public static class UpdateClientSettings {
        final String jid;
        final UpdateSettingsValues settings;

        public UpdateClientSettings(String jid, UpdateSettingsValues settings) {
            this.jid = jid;
            this.settings = settings;
        }
    }

    public static class UpdateSettingsValues {
        final int order;

        public UpdateSettingsValues(int order) {
            this.order = order;
        }
    }

    public static class ListClientSettingsDTO {
        final List<ClientSettingsDTO> settings_data;
        final OrderDataDTO order_data;
        final List<DeletedDTO> deleted;

        public ListClientSettingsDTO(List<ClientSettingsDTO> settings_data, OrderDataDTO order_data,
                                     List<DeletedDTO> deleted) {
            this.settings_data = settings_data;
            this.order_data = order_data;
            this.deleted = deleted;
        }

        public List<ClientSettingsDTO> getSettings() {
            return settings_data;
        }

        public OrderDataDTO getOrderData() {
            return order_data;
        }

        public List<DeletedDTO> getDeleted() {
            return deleted;
        }
    }

    public static class DeletedDTO {
        final String jid;
        final int timestamp;

        public DeletedDTO(String jid, int timestamp) {
            this.jid = jid;
            this.timestamp = timestamp;
        }

        public String getJid() {
            return jid;
        }

        public int getTimestamp() {
            return timestamp;
        }
    }

    public static class ClientSettingsWithoutOrderDTO {
        final List<ClientSettingsDTO> settings_data;

        public ClientSettingsWithoutOrderDTO(List<ClientSettingsDTO> settings_data) {
            this.settings_data = settings_data;
        }

        public List<ClientSettingsDTO> getSettings() {
            return settings_data;
        }
    }

    public static class ClientSettingsOrderDTO {
        final OrderDataDTO order_data;

        public ClientSettingsOrderDTO(OrderDataDTO order_data) {
            this.order_data = order_data;
        }

        public OrderDataDTO getOrder_data() {
            return order_data;
        }
    }

    public static class OrderDataDTO {
        final List<OrderDTO> settings;
        final int timestamp;

        public OrderDataDTO(List<OrderDTO> settings, int timestamp) {
            this.settings = settings;
            this.timestamp = timestamp;
        }

        public List<OrderDTO> getSettings() {
            return settings;
        }

        public int getTimestamp() {
            return timestamp;
        }
    }

    public static class OrderDTO {
        final String jid;
        final int order;

        public OrderDTO(String jid, int order) {
            this.jid = jid;
            this.order = order;
        }

        public String getJid() {
            return jid;
        }

        public int getOrder() {
            return order;
        }
    }

    public static class ClientSettingsDTO {
        final String jid;
        final SettingsValuesDTO settings;
        final int timestamp;

        public ClientSettingsDTO(String jid, SettingsValuesDTO settings, int timestamp) {
            this.jid = jid;
            this.settings = settings;
            this.timestamp = timestamp;
        }

        public String getJid() {
            return jid;
        }

        public SettingsValuesDTO getSettings() {
            return settings;
        }

        public int getTimestamp() {
            return timestamp;
        }
    }

    public static class SettingsValuesDTO {
        final int order;
        final String color;
        final String token;
        final String username;

        public SettingsValuesDTO(int order, String color, String token, String username) {
            this.order = order;
            this.color = color;
            this.token = token;
            this.username = username;
        }

        public int getOrder() {
            return order;
        }

        public String getColor() {
            return color;
        }

        public String getToken() {
            return token;
        }

        public String getUsername() {
            return username;
        }
    }
}
