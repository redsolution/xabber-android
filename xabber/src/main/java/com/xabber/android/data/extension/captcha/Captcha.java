package com.xabber.android.data.extension.captcha;

/**
 * Created by valery.miller on 11.07.17.
 */

public class Captcha {

    private String key;
    private long expiresDate;
    private int attemptCount = 0;
    private String question;
    private String answer;

    public Captcha(String key, long expiresDate, String question, String answer) {
        this.key = key;
        this.expiresDate = expiresDate;
        this.question = question;
        this.answer = answer;
    }

    public String getKey() {
        return key;
    }

    public long getExpiresDate() {
        return expiresDate;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }
}
