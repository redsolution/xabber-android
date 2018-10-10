package com.xabber.android.data.database.realm;

import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by valery.miller on 19.07.17.
 */

public class XabberAccountRealm extends RealmObject {

    @PrimaryKey
    @Required
    private String id;
    private String accountStatus;
    private String token;
    private String username;
    private String domain;
    private String firstName;
    private String lastName;
    private String registerDate;
    private String language;
    private String phone;
    private boolean needToVerifyPhone;
    private boolean hasPassword;
    private RealmList<XMPPUserRealm> xmppUsers;
    private RealmList<EmailRealm> emails;
    private RealmList<SocialBindingRealm> socialBindings;

    public XabberAccountRealm(String id) {
        this.id = id;
    }

    public XabberAccountRealm() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(String registerDate) {
        this.registerDate = registerDate;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public RealmList<XMPPUserRealm> getXmppUsers() {
        return xmppUsers;
    }

    public void setXmppUsers(RealmList<XMPPUserRealm> xmppUsers) {
        this.xmppUsers = xmppUsers;
    }

    public RealmList<EmailRealm> getEmails() {
        return emails;
    }

    public void setEmails(RealmList<EmailRealm> emails) {
        this.emails = emails;
    }

    public RealmList<SocialBindingRealm> getSocialBindings() {
        return socialBindings;
    }

    public void setSocialBindings(RealmList<SocialBindingRealm> socialBindings) {
        this.socialBindings = socialBindings;
    }

    public boolean isNeedToVerifyPhone() {
        return needToVerifyPhone;
    }

    public void setNeedToVerifyPhone(boolean needToVerifyPhone) {
        this.needToVerifyPhone = needToVerifyPhone;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean hasPassword() {
        return hasPassword;
    }

    public void setHasPassword(boolean hasPassword) {
        this.hasPassword = hasPassword;
    }
}

