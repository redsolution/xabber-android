package com.xabber.android.data.xaccount;

import android.util.Base64;

import com.google.gson.Gson;
import com.xabber.android.BuildConfig;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.privatestorage.PrivateStorageManager;

import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import rx.Single;
import rx.functions.Func1;

/**
 * Created by valery.miller on 17.07.17.
 */

public class AuthManager {

    public static final String PROVIDER_FACEBOOK = "facebook";
    public static final String PROVIDER_TWITTER = "twitter";
    public static final String PROVIDER_GITHUB = "github";
    public static final String PROVIDER_GOOGLE = "google";

    private static final String SOURCE_NAME = "Xabber Android";

    public static Single<XAccountTokenDTO> login(String login, String pass) {
        SettingsManager.setSyncAllAccounts(true);

        String credentials = login + ":" + pass;
        byte[] data = credentials.getBytes();
        String encodedCredentials = Base64.encodeToString(data, Base64.NO_WRAP);

        return HttpApiManager.getXabberApi().login("Basic " + encodedCredentials, new Source(getSource()));
    }

    public static Single<ResponseBody> logout(final boolean deleteAccounts) {

        return HttpApiManager.getXabberApi().logout(getXabberTokenHeader())
                .flatMap(new Func1<ResponseBody, Single<? extends ResponseBody>>() {
                    @Override
                    public Single<? extends ResponseBody> call(ResponseBody responseBody) {
                        if (XabberAccountManager.getInstance().deleteXabberAccountFromRealm())
                            return Single.just(responseBody);
                        else
                            return Single.error(new Throwable("Realm: xabber account deletion error"));
                    }
                })
                .flatMap(new Func1<ResponseBody, Single<? extends ResponseBody>>() {
                    @Override
                    public Single<? extends ResponseBody> call(ResponseBody responseBody) {
                        if (XabberAccountManager.getInstance().deleteSyncStatesFromRealm())
                            return Single.just(responseBody);
                        else
                            return Single.error(new Throwable("Realm: xabber account deletion error"));
                    }
                });
    }

    public static Single<XAccountTokenDTO> loginSocial(String provider, String socialToken) {
        SettingsManager.setSyncAllAccounts(true);

        Gson gson = new Gson();
        String credentials = gson.toJson(new AccessToken(socialToken));
        return HttpApiManager.getXabberApi().loginSocial(new SocialAuthRequest(provider, credentials, getSource()));
    }

    public static Single<XAccountTokenDTO> loginSocialTwitter(
           String socialToken, String twitterTokenSecret, String secret, String key) {
        SettingsManager.setSyncAllAccounts(true);

        Gson gson = new Gson();
        String credentials = gson.toJson(new TwitterAccessToken(new TwitterTokens(twitterTokenSecret, socialToken), secret, key));
        return HttpApiManager.getXabberApi().loginSocial(new SocialAuthRequest(PROVIDER_TWITTER, credentials, getSource()));
    }

    public static Single<XabberAccount> getAccount(final String token) {
        return HttpApiManager.getXabberApi().getAccount("Token " + token)
                .flatMap(new Func1<XabberAccountDTO, Single<? extends XabberAccountDTO>>() {
                    @Override
                    public Single<? extends XabberAccountDTO> call(XabberAccountDTO xabberAccountDTO) {
                        if (xabberAccountDTO.getLanguage() != null && !xabberAccountDTO.getLanguage().equals(""))
                            return Single.just(xabberAccountDTO);
                        else return updateAccount(token, new Account(xabberAccountDTO.getFirstName(),
                                xabberAccountDTO.getLastName(), Locale.getDefault().getLanguage()));
                    }
                })
                .flatMap(new Func1<XabberAccountDTO, Single<? extends XabberAccount>>() {
                    @Override
                    public Single<? extends XabberAccount> call(XabberAccountDTO xabberAccountDTO) {
                        return XabberAccountManager.getInstance().saveOrUpdateXabberAccountToRealm(xabberAccountDTO, token);
                    }
                });
    }

    private static Single<XabberAccountDTO> updateAccount(final String token, Account account) {
        return HttpApiManager.getXabberApi().updateAccount("Token " + token, account);
    }

    public static Single<List<XMPPAccountSettings>> getClientSettings() {
        return HttpApiManager.getXabberApi().getClientSettings(getXabberTokenHeader())
                .flatMap(new Func1<ListClientSettingsDTO, Single<? extends List<XMPPAccountSettings>>>() {
                    @Override
                    public Single<? extends List<XMPPAccountSettings>> call(ListClientSettingsDTO listClientSettingsDTO) {
                        return XabberAccountManager.getInstance().clientSettingsDTOListToPOJO(listClientSettingsDTO);
                    }
                })
                .flatMap(new Func1<List<XMPPAccountSettings>, Single<? extends List<XMPPAccountSettings>>>() {
                    @Override
                    public Single<? extends List<XMPPAccountSettings>> call(List<XMPPAccountSettings> xmppAccounts) {
                        // add only new accounts from server to sync map
                        Map<String, Boolean> syncState = new HashMap<>();
                        for (XMPPAccountSettings account : xmppAccounts) {
                            if (XabberAccountManager.getInstance().getAccountSyncState(account.getJid()) == null)
                                syncState.put(account.getJid(), true);
                        }
                        XabberAccountManager.getInstance().setAccountSyncState(syncState);

                        return Single.just(xmppAccounts);
                    }
                });
    }

    public static Single<List<XMPPAccountSettings>> patchClientSettings(List<XMPPAccountSettings> accountSettingsList) {
        List<OrderDTO> listOrder = new ArrayList<>();
        List<ClientSettingsDTO> listSettings = new ArrayList<>();

        // divide all data into two lists: settings and orders
        for (XMPPAccountSettings account : accountSettingsList) {
            listSettings.add(new ClientSettingsDTO(account.getJid(), new SettingsValuesDTO(account.getOrder(),
                    account.getColor(), account.getToken(), account.getUsername()), account.getTimestamp()));

            if (account.getOrder() > 0)
                listOrder.add(new OrderDTO(account.getJid(), account.getOrder()));
        }

        // prepare dto for settings
        ClientSettingsWithoutOrderDTO settingsDTO = new ClientSettingsWithoutOrderDTO(listSettings);

        // prepare dto for orders
        OrderDataDTO orderDataDTO = new OrderDataDTO(listOrder, XabberAccountManager.getInstance().getLastOrderChangeTimestamp());
        final ClientSettingsOrderDTO orderDTO = new ClientSettingsOrderDTO(orderDataDTO);

        // patch settings to server
        return HttpApiManager.getXabberApi().updateClientSettings(getXabberTokenHeader(), settingsDTO)
                .flatMap(new Func1<ListClientSettingsDTO, Single<? extends ListClientSettingsDTO>>() {
                    @Override
                    public Single<? extends ListClientSettingsDTO> call(ListClientSettingsDTO listClientSettingsDTO) {
                        // patch orders to server
                        return HttpApiManager.getXabberApi().updateClientSettings(getXabberTokenHeader(), orderDTO);
                    }
                })
                .flatMap(new Func1<ListClientSettingsDTO, Single<? extends List<XMPPAccountSettings>>>() {
                    @Override
                    public Single<? extends List<XMPPAccountSettings>> call(ListClientSettingsDTO listClientSettingsDTO) {
                        // convert dto to pojo
                        return XabberAccountManager.getInstance().clientSettingsDTOListToPOJO(listClientSettingsDTO);
                    }
                })
                .flatMap(new Func1<List<XMPPAccountSettings>, Single<? extends List<XMPPAccountSettings>>>() {
                    @Override
                    public Single<? extends List<XMPPAccountSettings>> call(List<XMPPAccountSettings> xmppAccounts) {
                        // add only new accounts from server to sync map
                        Map<String, Boolean> syncState = new HashMap<>();
                        for (XMPPAccountSettings account : xmppAccounts) {
                            if (XabberAccountManager.getInstance().getAccountSyncState(account.getJid()) == null)
                                    syncState.put(account.getJid(), true);
                        }
                        XabberAccountManager.getInstance().setAccountSyncState(syncState);

                        // update last synchronization time
                        SettingsManager.setLastSyncDate(XabberAccountManager.getCurrentTimeString());

                        // update local accounts
                        return XabberAccountManager.getInstance().updateLocalAccounts(xmppAccounts);
                    }
                });
    }

    public static Single<List<XMPPAccountSettings>> deleteClientSettings(String jid) {
        // delete settings from server
        return HttpApiManager.getXabberApi().deleteClientSettings(getXabberTokenHeader(), new Jid(jid))
                .flatMap(new Func1<ListClientSettingsDTO, Single<? extends List<XMPPAccountSettings>>>() {
                    @Override
                    public Single<? extends List<XMPPAccountSettings>> call(ListClientSettingsDTO listClientSettingsDTO) {
                        // convert dto to pojo
                        return XabberAccountManager.getInstance().clientSettingsDTOListToPOJO(listClientSettingsDTO);
                    }
                })
                .flatMap(new Func1<List<XMPPAccountSettings>, Single<? extends List<XMPPAccountSettings>>>() {
                    @Override
                    public Single<? extends List<XMPPAccountSettings>> call(List<XMPPAccountSettings> xmppAccounts) {
                        // add only new accounts from server to sync map
                        Map<String, Boolean> syncState = new HashMap<>();
                        for (XMPPAccountSettings account : xmppAccounts) {
                            if (XabberAccountManager.getInstance().getAccountSyncState(account.getJid()) == null)
                                syncState.put(account.getJid(), true);
                        }
                        XabberAccountManager.getInstance().setAccountSyncState(syncState);

                        // update last synchronization time
                        SettingsManager.setLastSyncDate(XabberAccountManager.getCurrentTimeString());

                        // update local accounts
                        return XabberAccountManager.getInstance().updateLocalAccounts(xmppAccounts);
                    }
                });
    }

    public static Single<XAccountTokenDTO> signup(String email) {
        SettingsManager.setSyncAllAccounts(true);
        return HttpApiManager.getXabberApi().signup(new Email(email, getSource()));
    }

    public static Single<XabberAccount> confirmEmail(String code) {
        return HttpApiManager.getXabberApi().confirmEmail(getXabberTokenHeader(), new Code(code))
                .flatMap(new Func1<XabberAccountDTO, Single<? extends XabberAccount>>() {
                    @Override
                    public Single<? extends XabberAccount> call(XabberAccountDTO xabberAccountDTO) {
                        return XabberAccountManager.getInstance().saveOrUpdateXabberAccountToRealm(xabberAccountDTO, getXabberToken());
                    }
                });
    }

    public static Single<XabberAccount> confirmEmailWithKey(String key) {
        return HttpApiManager.getXabberApi().confirmEmail(new Key(key, getSource()))
                .flatMap(new Func1<XabberAccountDTO, Single<? extends XabberAccount>>() {
                    @Override
                    public Single<? extends XabberAccount> call(XabberAccountDTO xabberAccountDTO) {
                        return XabberAccountManager.getInstance().saveOrUpdateXabberAccountToRealm(xabberAccountDTO, getXabberToken());
                    }
                });
    }

    public static Single<XabberAccount> completeRegister(String username, String pass, String confirmPass,
                                                         String firstName, String lastName,
                                                         String host, String language, boolean createToken) {
        return HttpApiManager.getXabberApi().completeRegister(getXabberTokenHeader(),
                new CompleteRegister(username, pass, confirmPass, firstName, lastName, host, language, createToken))
                .flatMap(new Func1<XabberAccountDTO, Single<? extends XabberAccount>>() {
                    @Override
                    public Single<? extends XabberAccount> call(XabberAccountDTO xabberAccountDTO) {
                        return XabberAccountManager.getInstance().saveOrUpdateXabberAccountToRealm(xabberAccountDTO, getXabberToken());
                    }
                });
    }

    public static Single<ResponseBody> addEmail(String email) {
        return HttpApiManager.getXabberApi().addEmail(getXabberTokenHeader(), new Email(email, getSource()));
    }

    public static Single<ResponseBody> setPhoneNumber(String phoneNumber) {
        return HttpApiManager.getXabberApi().setPhoneNumber(getXabberTokenHeader(), new SetPhoneNumber("set", phoneNumber));
    }

    public static Single<ResponseBody> confirmPhoneNumber(String code) {
        return HttpApiManager.getXabberApi().confirmPhoneNumber(getXabberTokenHeader(), new ConfirmPhoneNumber("verify", code));
    }

    // API v2

    public static Single<XMPPCode> requestXMPPCode(String jid) {
        return HttpApiManager.getXabberApi().requestXMPPCode(new Jid(jid));
    }

    public static Single<XabberAccount> confirmXMPP(final String jid, String code) {
        return HttpApiManager.getXabberApi().confirmXMPP(new CodeConfirm(code, jid))
                .flatMap(new Func1<XabberAccountDTO, Single<? extends XabberAccount>>() {
                    @Override
                    public Single<? extends XabberAccount> call(XabberAccountDTO xabberAccountDTO) {
                        try {
                            PrivateStorageManager.getInstance().setXabberAccountBinding(AccountJid.from(jid), true);
                        } catch (XmppStringprepException e) {
                            e.printStackTrace();
                        }

                        return XabberAccountManager.getInstance().saveOrUpdateXabberAccountToRealm(xabberAccountDTO,
                                xabberAccountDTO.getToken());
                    }
                });
    }

    public static Single<List<Domain>> getHosts() {
        return HttpApiManager.getXabberApi().getHosts();
    }

    public static Single<XabberAccount> signupv2(String username, String host, String password,
                                                 String captchaToken) {
        return HttpApiManager.getXabberApi().signupv2(new SignUpFields(username, host,
            password, captchaToken))
            .flatMap(new Func1<XabberAccountDTO, Single<? extends XabberAccount>>() {
                @Override
                public Single<? extends XabberAccount> call(XabberAccountDTO xabberAccountDTO) {
                    return XabberAccountManager.getInstance().saveOrUpdateXabberAccountToRealm(xabberAccountDTO,
                            xabberAccountDTO.getToken());
                }
            });
    }

    public static Single<XabberAccount> signupv2(String username, String host, String password,
                                                 String provider, String socialToken) {

        Gson gson = new Gson();
        String credentials = gson.toJson(new AccessToken(socialToken));

        return HttpApiManager.getXabberApi().signupv2(new SignUpFields(username, host,
                password, provider, credentials))
                .flatMap(new Func1<XabberAccountDTO, Single<? extends XabberAccount>>() {
                    @Override
                    public Single<? extends XabberAccount> call(XabberAccountDTO xabberAccountDTO) {
                        return XabberAccountManager.getInstance().saveOrUpdateXabberAccountToRealm(xabberAccountDTO,
                                xabberAccountDTO.getToken());
                    }
                });
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

    // models

    public static class SignUpFields {
        final String username;
        final String host;
        final String password;
        String captcha_token;
        String provider;
        String credentials;
        final boolean create_token;
        String first_name;
        String last_name;
        String language;
        final String source;
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

    public static class Domain {
        final String domain;

        public Domain(String domain) {
            this.domain = domain;
        }

        public String getDomain() {
            return domain;
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

    public static class CompleteRegister {
        final String username;
        final String password;
        final String confirm_password;
        final String first_name;
        final String last_name;
        final String host;
        final String language;
        final boolean create_token;

        public CompleteRegister(String username, String password, String confirm_password,
                                String first_name, String last_name, String host, String language,
                                boolean create_token) {
            this.username = username;
            this.password = password;
            this.confirm_password = confirm_password;
            this.first_name = first_name;
            this.last_name = last_name;
            this.host = host;
            this.language = language;
            this.create_token = create_token;
        }
    }

    public static class SetPhoneNumber {
        final String action;
        final String phone;

        public SetPhoneNumber(String action, String phone) {
            this.action = action;
            this.phone = phone;
        }
    }

    public static class ConfirmPhoneNumber {
        final String action;
        final String code;

        public ConfirmPhoneNumber(String action, String code) {
            this.action = action;
            this.code = code;
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

        public ListClientSettingsDTO(List<ClientSettingsDTO> settings_data, OrderDataDTO order_data, List<DeletedDTO> deleted) {
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
