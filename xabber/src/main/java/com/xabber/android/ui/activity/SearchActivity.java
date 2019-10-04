package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.fragment.app.FragmentTransaction;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.presentation.ui.contactlist.ChatListFragment;
import com.xabber.android.ui.color.AccountPainter;
import com.xabber.android.ui.color.ColorManager;

public class SearchActivity extends ManagedActivity implements View.OnClickListener, ChatListFragment.ChatListFragmentListener {

    /* Constants for Chat List Fragment */
    private static final String CHAT_LIST_TAG = "CHAT_LIST";

    /* Constants for savind state budle*/
    private static final String SAVED_ACTION = "com.xabber.android.ui.activity.SearchActivity.SAVED_ACTION";
    private static final String SAVED_SEND_TEXT = "com.xabber.android.ui.activity.SearchActivity.SAVED_SEND_TEXT";

    /* Toolbar variables */
    private RelativeLayout toolbarLayout;           //Toolbar layout
    private ImageView toolbarBackIv;                //Back arrow always active
    private RelativeLayout toolbarGreetingsLayout;  //Contains toolbarGreetingsSearchIv and "Choose recipient" TextView
    private ImageView toolbarGreetingsSearchIv;     //Belongs to toolbarGreetingLayout
    private RelativeLayout toolbarSearchlayout;     //Contains toolbar toolbarSearchEt, toolbarSearchClearIv
    private EditText toolbarSearchEt;               //Belongs to toolbarSearchLayout
    private ImageView toolbarSearchClearIv;         //belongs to toolbarSearchLayout
    private CurrentToolbarLayout toolbarCurrentLayout = CurrentToolbarLayout.greetings;

    /* InputMethodManager for keyboard management variable */
    private InputMethodManager inputMethodManager;

    /* Variables for intents */
    private String action;
    private String sendText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        /* Check Saved State */
        if (savedInstanceState != null) {
            sendText = savedInstanceState.getString(SAVED_SEND_TEXT);
            action = savedInstanceState.getString(SAVED_ACTION);
        } else {
            sendText = null;
            action = getIntent().getAction();
        }

        /* Initialize InputMethodManager */
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        /* Initialize and setup toolbar */
        toolbarLayout = findViewById(R.id.search_toolbar_layout);
        toolbarBackIv = findViewById(R.id.toolbar_search_back_button);
        toolbarGreetingsLayout = findViewById(R.id.search_toolbar_greetings_view);
        toolbarGreetingsSearchIv = findViewById(R.id.search_toolbar_search_button);
        toolbarSearchlayout = findViewById(R.id.search_toolbar_search_view);
        toolbarSearchEt = findViewById(R.id.search_toolbar_edittext);
        toolbarSearchClearIv = findViewById(R.id.search_toolbar_clear_button);
        toolbarSearchClearIv.setOnClickListener(this);
        toolbarSearchEt.setOnClickListener(this);
        toolbarGreetingsSearchIv.setOnClickListener(this);
        toolbarBackIv.setOnClickListener(this);
        findViewById(R.id.search_activity_container).setOnClickListener(this);
        toolbarSearchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() != 0) toolbarSearchClearIv.setVisibility(View.VISIBLE);
                else toolbarSearchClearIv.setVisibility(View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        showChatListFragment();

        //TODO delete this hord invoke GreetingsLayout to test
        toolbarGreetingsLayout.setVisibility(View.VISIBLE);

        /*
        Update background color via current main user;
         */
        TypedValue typedValue = new TypedValue();
        TypedArray a = this.obtainStyledAttributes(typedValue.data, new int[] {R.attr.contact_list_account_group_background});
        final int accountGroupColorsResourceId = a.getResourceId(0, 0);
        a.recycle();
        final int[] accountGroupColors = this.getResources().getIntArray(accountGroupColorsResourceId);
        final int level = AccountManager.getInstance().getColorLevel(AccountPainter.getFirstAccount());
        toolbarLayout.setBackgroundColor(accountGroupColors[level]);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* Setup StatusBarColor */   //TODO Doesn't working think about it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(SAVED_ACTION, action);
        outState.putString(SAVED_SEND_TEXT, sendText);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.toolbar_search_back_button:
                finish();
                break;
            case R.id.search_toolbar_search_button:
                toolbarSearchlayout.setVisibility(View.VISIBLE);
                toolbarGreetingsLayout.setVisibility(View.GONE);
                toolbarSearchEt.requestFocus();
                inputMethodManager.showSoftInput(toolbarSearchEt, InputMethodManager.SHOW_IMPLICIT);
                break;
            case R.id.search_toolbar_clear_button:
                if (toolbarSearchEt.getText().toString().isEmpty() || toolbarSearchEt.getText() == null){
                    inputMethodManager.hideSoftInputFromWindow(toolbarSearchEt.getWindowToken(), 0);
                    toolbarSearchlayout.setVisibility(View.GONE);
                    toolbarGreetingsLayout.setVisibility(View.VISIBLE);
                    //TODO implement clearing search string
                } else {
                    toolbarSearchEt.setText("");
                }
                break;
            default:
                inputMethodManager.hideSoftInputFromWindow(toolbarSearchEt.getWindowToken(), 0);
                if (toolbarSearchEt.getText().toString().isEmpty() || toolbarSearchEt.getText() == null){
                    inputMethodManager.hideSoftInputFromWindow(toolbarSearchEt.getWindowToken(), 0);
                    toolbarSearchlayout.setVisibility(View.GONE);
                    toolbarGreetingsLayout.setVisibility(View.VISIBLE);
                    //TODO implement clearing search string and probably change view id
                }
                break;
        }
    }

    //TODO saving state instance

    //TODO Intent-actions

    /**
     * Shows existing or make new ChatListFragment
     */
    private void showChatListFragment() {
        if (!isFinishing()) {
            FragmentTransaction fTrans = getSupportFragmentManager().beginTransaction();
            if (getSupportFragmentManager().findFragmentByTag(CHAT_LIST_TAG) != null)
            fTrans.replace(R.id.search_activity_container, getSupportFragmentManager().findFragmentByTag(CHAT_LIST_TAG), CHAT_LIST_TAG);
            else fTrans.replace(R.id.search_activity_container, ChatListFragment.newInstance(null), CHAT_LIST_TAG);
            fTrans.commit();
        }
    }

    @Override
    public void onChatClick(AbstractContact contact) {
        Toast.makeText(this, "Chat was clicked", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onChatListStateChanged(ChatListFragment.ChatListState chatListState) {
        Toast.makeText(this, "ChatList state was changed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUnreadChanged(int unread) {
        Toast.makeText(this, "Unread count was changed", Toast.LENGTH_SHORT).show();
    }

    //TODO decise about necessity
    private enum CurrentToolbarLayout{
        greetings, search
    }
}
