package com.xabber.android.data.xaccount;

/**
 * Created by valery.miller on 26.07.17.
 */

public class EmailDTO {

    private int id;
    private String email;
    private boolean verified;
    private boolean primary;

    public EmailDTO(int id, String email, boolean verified, boolean primary) {
        this.id = id;
        this.email = email;
        this.verified = verified;
        this.primary = primary;
    }

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public boolean isVerified() {
        return verified;
    }

    public boolean isPrimary() {
        return primary;
    }
}
