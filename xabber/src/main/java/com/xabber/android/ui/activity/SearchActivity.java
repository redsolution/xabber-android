package com.xabber.android.ui.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.xabber.android.R;

public class SearchActivity extends ManagedActivity implements View.OnClickListener {

    /*
    Toolbar variables
     */
    private RelativeLayout toolbarLayout;           //Toolbar layout
    private ImageView toolbarBackIv;                //Back arrow always active
    private RelativeLayout toolbarGreetingsLayout;  //Contains toolbarGreetingsSearchIv and "Choose recipient" TextView
    private ImageView toolbarGreetingsSearchIv;              //Belongs to toolbarGreetingLayout
    private RelativeLayout toolbarSearchlayout;     //Contains toolbar toolbarSearchEt, toolbarSearchClearIv
    private EditText toolbarSearchEt;               //Belongs to toolbarSearchLayout
    private ImageView toolbarSearchClearIv;         //belongs to toolbarSearchLayout
    private CurrentToolbarLayout toolbarCurrentLayout = CurrentToolbarLayout.greetings;

    /*
    InputMehodManager for keyboard management
     */
    private InputMethodManager inputMethodManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        /*
        Initialize InputMethodManager
         */
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        /*
        Initialize toolbar variables
         */
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

        //TODO hord invoke GreetingsLayout to test
        toolbarGreetingsLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.toolbar_search_back_button :
                if (toolbarSearchEt.isFocused()){
                    inputMethodManager.hideSoftInputFromWindow(toolbarSearchEt.getWindowToken(), 0);
                    toolbarSearchlayout.setVisibility(View.GONE);
                    toolbarGreetingsLayout.setVisibility(View.VISIBLE);
                    toolbarSearchEt.setText("");
                    //TODO implement clearing search string
                } else finish();
                break;
            case R.id.search_toolbar_search_button :
                toolbarSearchlayout.setVisibility(View.VISIBLE);
                toolbarGreetingsLayout.setVisibility(View.GONE);
                toolbarSearchEt.requestFocus();
                inputMethodManager.showSoftInput(toolbarSearchEt, InputMethodManager.SHOW_IMPLICIT);
                break;
            case R.id.search_toolbar_clear_button :
                if (toolbarSearchEt.getText().toString().isEmpty() || toolbarSearchEt.getText() == null){
                    inputMethodManager.hideSoftInputFromWindow(toolbarSearchEt.getWindowToken(), 0);
                    toolbarSearchlayout.setVisibility(View.GONE);
                    toolbarGreetingsLayout.setVisibility(View.VISIBLE);
                    //TODO implement clearing search string
                } else {
                    toolbarSearchEt.setText("");
                }
                break;
        }
    }


    private enum CurrentToolbarLayout{
        greetings, search
    }
}
