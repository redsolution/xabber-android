package com.xabber.android.data.xaccount;

/**
 * Created by valery.miller on 19.07.17.
 */

public class XMPPUser {

    private int id;
    private String username;
    private String host;
    private String registerDate;

    public XMPPUser(int id, String username, String host, String registerDate) {
        this.id = id;
        this.username = username;
        this.host = host;
        this.registerDate = registerDate;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getHost() {
        return host;
    }

    public String getRegisterDate() {
        return registerDate;
    }
}
