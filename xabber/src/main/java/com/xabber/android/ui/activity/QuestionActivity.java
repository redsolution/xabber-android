/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.helper.ContactTitleActionBarInflater;

import org.jxmpp.jid.BareJid;

import java.util.Collection;

/**
 * Represents OTR question.
 *
 * @author alexander.ivanov
 */
public class QuestionActivity extends ManagedActivity implements
        OnAccountChangedListener, OnContactChangedListener, OnClickListener {

    public static final String EXTRA_FIELD_SHOW_QUESTION = "com.xabber.android.data.ui.QuestionViewer.SHOW_QUESTION";
    public static final String EXTRA_FIELD_ANSWER_REQUEST = "com.xabber.android.data.ui.QuestionViewer.ANSWER_REQUEST";
    public static final String EXTRA_FIELD_CANCEL = "com.xabber.android.data.ui.QuestionViewer.CANCEL";
    ContactTitleActionBarInflater contactTitleActionBarInflater;
    private AccountJid account;
    private ContactJid user;
    private boolean showQuestion;
    private boolean answerRequest;
    private EditText questionView;

    /**
     * @param context
     * @param account
     * @param user
     * @return Intent to cancel negotiation.
     */
    public static Intent createCancelIntent(Context context, AccountJid account, ContactJid user) {
        Intent intent = new EntityIntentBuilder(context, QuestionActivity.class)
                .setAccount(account).setUser(user).build();
        intent.putExtra(EXTRA_FIELD_CANCEL, true);
        return intent;
    }

    /**
     * @param context
     * @param account
     * @param user
     * @param showQuestion  <code>false</code> is used for shared secret.
     * @param answerRequest <code>false</code> is used to ask a question.
     * @param question      must be not <code>null</code> if showQuestion and
     *                      answerRequest are <code>true</code>.
     * @return
     */
    public static Intent createIntent(Context context, AccountJid account, ContactJid user,
                                      boolean showQuestion, boolean answerRequest, String question) {
        Intent intent = new EntityIntentBuilder(context, QuestionActivity.class)
                .setAccount(account).setUser(user).build();
        intent.putExtra(EXTRA_FIELD_SHOW_QUESTION, showQuestion);
        intent.putExtra(EXTRA_FIELD_ANSWER_REQUEST, answerRequest);
        intent.putExtra(Intent.EXTRA_TEXT, question);
        return intent;
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    private static ContactJid getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }

        Intent intent = getIntent();
        account = QuestionActivity.getAccount(intent);
        user = QuestionActivity.getUser(intent);
        if (AccountManager.getInstance().getAccount(account) == null || user == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
            return;
        }
        if (intent.getBooleanExtra(EXTRA_FIELD_CANCEL, false)) {
            try {
                OTRManager.getInstance().abortSmp(account, user);
            } catch (NetworkException e) {
                Application.getInstance().onError(e);
            }
            finish();
            return;
        }
        showQuestion = intent.getBooleanExtra(EXTRA_FIELD_SHOW_QUESTION, true);
        answerRequest = intent.getBooleanExtra(EXTRA_FIELD_ANSWER_REQUEST, false);
        if (showQuestion) {
            setContentView(R.layout.activity_question);
            questionView = (EditText) findViewById(R.id.question);
            questionView.setEnabled(!answerRequest);
            if (answerRequest) {
                questionView.setText(intent.getStringExtra(Intent.EXTRA_TEXT));
            } else {
                findViewById(R.id.cancel).setVisibility(View.GONE);
            }
        } else {
            setContentView(R.layout.secret_viewer);

        }
        findViewById(R.id.cancel).setOnClickListener(this);
        findViewById(R.id.send).setOnClickListener(this);

        contactTitleActionBarInflater = new ContactTitleActionBarInflater(this);
        contactTitleActionBarInflater.setUpActionBarView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        update();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        BareJid thisBareAddress = user.getBareJid();
        for (BaseEntity entity : entities) {
            if (entity.equals(account, thisBareAddress)) {
                update();
                break;
            }
        }
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        if (accounts.contains(account)) {
            update();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.send:
                String question = showQuestion ? questionView.getText().toString() : null;
                String answer = ((TextView) findViewById(R.id.answer)).getText().toString();
                try {
                    if (answerRequest) {
                        OTRManager.getInstance().respondSmp(account, user, question, answer);
                    } else {
                        OTRManager.getInstance().initSmp(account, user, question, answer);
                    }
                } catch (NetworkException e) {
                    Application.getInstance().onError(e);
                }
                finish();
                break;
            case R.id.cancel:
                try {
                    OTRManager.getInstance().abortSmp(account, user);
                } catch (NetworkException e) {
                    Application.getInstance().onError(e);
                }
                finish();
            default:
                break;
        }
    }

    private void update() {
        AbstractContact abstractContact = RosterManager.getInstance().getBestContact(account, user);
        contactTitleActionBarInflater.update(abstractContact);
    }

}
