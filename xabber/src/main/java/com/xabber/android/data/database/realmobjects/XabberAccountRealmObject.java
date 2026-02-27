package com.xabber.android.data.database.realmobjects;

import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Created by valery.miller on 19.07.17.
 */

public class XabberAccountRealmObject extends RealmObject {

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
    private RealmList<XMPPUserRealmObject> xmppUsers;
    private RealmList<EmailRealmObject> emails;
    private RealmList<SocialBindingRealmObject> socialBindings;

    public XabberAccountRealmObject(String id) {
        this.id = id;
    }

    public XabberAccountRealmObject() {
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

    public RealmList<XMPPUserRealmObject> getXmppUsers() {
        return xmppUsers;
    }

    public void setXmppUsers(RealmList<XMPPUserRealmObject> xmppUsers) {
        this.xmppUsers = xmppUsers;
    }

    public RealmList<EmailRealmObject> getEmails() {
        return emails;
    }

    public void setEmails(RealmList<EmailRealmObject> emails) {
        this.emails = emails;
    }

    public RealmList<SocialBindingRealmObject> getSocialBindings() {
        return socialBindings;
    }

    public void setSocialBindings(RealmList<SocialBindingRealmObject> socialBindings) {
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

