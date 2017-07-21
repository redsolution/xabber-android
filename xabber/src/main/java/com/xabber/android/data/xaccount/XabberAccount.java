package com.xabber.android.data.xaccount;

import java.util.List;

/**
 * Created by valery.miller on 19.07.17.
 */

public class XabberAccount {

    private int id;
    private String username;
    private String firstName;
    private String lastName;
    private String registerDate;
    private List<XMPPUser> xmppUsers;
    private String token;

    public XabberAccount(int id, String username, String firstName, String lastName, String registerDate, List<XMPPUser> xmppUsers, String token) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.registerDate = registerDate;
        this.xmppUsers = xmppUsers;
        this.token = token;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
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

    public List<XMPPUser> getXmppUsers() {
        return xmppUsers;
    }

    public String getToken() {
        return token;
    }
}
