package com.xabber.android.data.xaccount;

import java.util.ArrayList;
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

    public XabberAccount(int id, String username, String firstName, String lastName, String registerDate, List<XMPPUser> xmppUsers) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.registerDate = registerDate;
        this.xmppUsers = new ArrayList<>();
        this.xmppUsers.addAll(xmppUsers);
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

}
