package com.xabber.android.data.xaccount;

/**
 * Created by valery.miller on 18.07.17.
 */

public class XMPPUserDTO {
    private int id;
    private String username;
    private String host;
    private String registration_date;

    public XMPPUserDTO(int id, String username, String host, String registration_date) {
        this.id = id;
        this.username = username;
        this.host = host;
        this.registration_date = registration_date;
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

    public String getRegistrationDate() {
        return registration_date;
    }
}

