package com.xabber.android.ui.activity;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;

import java.util.ArrayList;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class SearchActivity extends ManagedActivity implements View.OnClickListener {

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

//    /* Main list variables */
//    private RecyclerView recyclerView;
//    private FlexibleAdapter<IFlexible> adapter;
//    private ArrayList<IFlexible> items;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

//        /* Initialize and setup main list */
//        recyclerView = findViewById(R.id.search_recyclerview);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));


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

        //TODO delete this hord invoke GreetingsLayout to test
        toolbarGreetingsLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.toolbar_search_back_button :
                finish();
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
            case R.id.search_activity_container :
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

    //TODO decise about necessity
    private enum CurrentToolbarLayout{
        greetings, search
    }
}
