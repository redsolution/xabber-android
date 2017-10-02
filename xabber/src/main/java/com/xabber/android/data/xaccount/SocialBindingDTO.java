package com.xabber.android.data.xaccount;

/**
 * Created by valery.miller on 01.08.17.
 */

public class SocialBindingDTO {

    private int id;
    private String provider;
    private String uid;
    private String first_name;
    private String last_name;

    public SocialBindingDTO(int id, String provider, String uid, String first_name, String last_name) {
        this.id = id;
        this.provider = provider;
        this.uid = uid;
        this.first_name = first_name;
        this.last_name = last_name;
    }

    public int getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getUid() {
        return uid;
    }

    public String getFirstName() {
        return first_name;
    }

    public String getLastName() {
        return last_name;
    }
}
