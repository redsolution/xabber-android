package com.xabber.android.data.xaccount;

import java.util.List;

/**
 * Created by valery.miller on 19.07.17.
 */

public class XabberAccount {

    public static final String STATUS_NOT_CONFIRMED = "not_confirmed";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_REGISTERED = "registered";

    private int id;
    private String accountStatus;
    private String username;
    private String domain;
    private String firstName;
    private String lastName;
    private String registerDate;
    private String language;
    private List<XMPPUser> xmppUsers;
    private List<EmailDTO> emails;
    private List<SocialBindingDTO> socialBindings;
    private String token;
    private String phone;
    private boolean needToVerifyPhone;
    private boolean hasPassword;

    public XabberAccount(int id, String accountStatus, String username, String domain, String firstName,
                         String lastName, String registerDate, String language, List<XMPPUser> xmppUsers,
                         List<EmailDTO> emails, List<SocialBindingDTO> socialBindings, String token,
                         boolean needToVerifyPhone, String phone, boolean hasPassword) {
        this.id = id;
        this.accountStatus = accountStatus;
        this.username = username;
        this.domain = domain;
        this.firstName = firstName;
        this.lastName = lastName;
        this.registerDate = registerDate;
        this.language = language;
        this.xmppUsers = xmppUsers;
        this.emails = emails;
        this.socialBindings = socialBindings;
        this.token = token;
        this.needToVerifyPhone = needToVerifyPhone;
        this.phone = phone;
        this.hasPassword = hasPassword;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDomain() {
        return domain;
    }

    public String getFullUsername() {
        return username + "@" + domain;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getRegisterDate() {
        return registerDate;
    }

    public String getLanguage() {
        return language;
    }

    public List<XMPPUser> getXmppUsers() {
        return xmppUsers;
    }

    public List<EmailDTO> getEmails() {
        return emails;
    }

    public List<SocialBindingDTO> getSocialBindings() {
        return socialBindings;
    }

    public String getToken() {
        return token;
    }

    public boolean isNeedToVerifyPhone() {
        return needToVerifyPhone;
    }

    public String getPhone() {
        return phone;
    }

    public boolean hasPassword() {
        return hasPassword;
    }
}
