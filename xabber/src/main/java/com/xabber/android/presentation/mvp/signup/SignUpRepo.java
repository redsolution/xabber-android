package com.xabber.android.presentation.mvp.signup;

public class SignUpRepo {

    private String username;
    private String host;
    private String pass;
    private String socialCredentials;
    private String socialProvider;
    private String captchaToken;
    private String lastErrorMessage;

    private static SignUpRepo instance;

    public static SignUpRepo getInstance() {
        if (instance == null) instance = new SignUpRepo();
        return instance;
    }

    public void clearRepo() {
        username = null;
        host = null;
        pass = null;
        socialCredentials = null;
        socialProvider = null;
        captchaToken = null;
        lastErrorMessage = null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getSocialCredentials() {
        return socialCredentials;
    }

    public void setSocialCredentials(String socialCredentials) {
        this.socialCredentials = socialCredentials;
    }

    public String getSocialProvider() {
        return socialProvider;
    }

    public void setSocialProvider(String socialProvider) {
        this.socialProvider = socialProvider;
    }

    public String getCaptchaToken() {
        return captchaToken;
    }

    public void setCaptchaToken(String captchaToken) {
        this.captchaToken = captchaToken;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public boolean isCompleted() {
        return username != null && pass != null && host != null;
    }
}
