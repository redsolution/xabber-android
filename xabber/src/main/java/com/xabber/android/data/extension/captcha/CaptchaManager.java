package com.xabber.android.data.extension.captcha;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Created by valery.miller on 11.07.17.
 */

public class CaptchaManager {

    private static final int MAX_SAVED_CAPTCHAS = 15;
    private static final int CAPTCHA_LIFE_TIME_MILLIS = 60000 * 15;
    public static final int CAPTCHA_MAX_ATTEMPT_COUNT = 5;

    private static CaptchaManager instance;
    private List<Captcha> currentCaptchas;

    public static CaptchaManager getInstance() {
        if (instance == null)
            instance = new CaptchaManager();
        return instance;
    }

    public CaptchaManager() {
        currentCaptchas = new ArrayList<>();
    }

    public String generateAndSaveCaptcha(AccountJid account, UserJid user) {
        // remove old captcha for this user
        removeCaptcha(account, user);

        // clean captchas if list too large
        if (currentCaptchas.size() > MAX_SAVED_CAPTCHAS)
            currentCaptchas.remove(0);

        // generate new captcha
        Captcha captcha = generateRandomCaptcha(account, user);

        // save captcha-answer to ram
        currentCaptchas.add(captcha);

        // return captcha-question
        return captcha.getQuestion();
    }

    // returns captcha for this account and user if captcha exist else returns null
    public Captcha getCaptcha(AccountJid account, UserJid user) {
        String key = account.toString() + user.toString();

        for (int i = 0; i < currentCaptchas.size(); i++) {
            if (currentCaptchas.get(i).getKey().equals(key))
            return currentCaptchas.get(i);
        }

        return null;
    }

    public void removeCaptcha(AccountJid account, UserJid user) {
        String key = account.toString() + user.toString();
        for (Iterator<Captcha> iterator = currentCaptchas.iterator(); iterator.hasNext();) {
            if (iterator.next().getKey().equals(key))
                iterator.remove();
        }
    }

    public Captcha generateRandomCaptcha(AccountJid account, UserJid user) {
        Random random = new Random(System.currentTimeMillis());
        int firstNumber = random.nextInt(9) + 1;
        int secondNumber = random.nextInt(9) + 1;
        int action = random.nextInt(3);
        String answer = "";
        String question = "";

        if (firstNumber < secondNumber) {
            int temp = firstNumber;
            firstNumber = secondNumber;
            secondNumber = temp;
        }

        switch (action) {
            case 0:
                answer = "" + (firstNumber + secondNumber);
                question = firstNumber + " + " + secondNumber + " = ?";
                break;
            case 1:
                answer = "" + (firstNumber - secondNumber);
                question = firstNumber + " - " + secondNumber + " = ?";
                break;
            case 2:
                answer = "" + (firstNumber * secondNumber);
                question = firstNumber + " * " + secondNumber + " = ?";
                break;
        }

        return new Captcha(
                account.toString() + user.toString(),
                System.currentTimeMillis() + CAPTCHA_LIFE_TIME_MILLIS,
                question,
                answer);
    }
}
