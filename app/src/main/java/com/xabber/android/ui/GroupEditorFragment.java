package com.xabber.android.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.GroupEditorAdapter;
import com.xabber.androiddev.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class GroupEditorFragment extends ListFragment implements TextWatcher, View.OnClickListener {

    private static final String ARG_ACCOUNT = "com.xabber.android.ui.GroupEditorFragment.ARG_ACCOUNT";
    private static final String ARG_USER = "com.xabber.android.ui.GroupEditorFragment.ARG_USER";

    private static final String SAVED_GROUPS = "com.xabber.android.ui.GroupEditorFragment.SAVED_GROUPS";
    private static final String SAVED_SELECTED = "com.xabber.android.ui.GroupEditorFragment.SAVED_SELECTED";

    private String account;
    private String user;

    private GroupEditorAdapter groupEditorAdapter;

    private Collection<String> groups;
    private Collection<String> selected;

    private EditText groupAddInput;
    private CheckBox groupAddCheckBox;

    public static GroupEditorFragment newInstance(String account, String user) {
        GroupEditorFragment fragment = new GroupEditorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACCOUNT, account);
        args.putString(ARG_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public GroupEditorFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            account = getArguments().getString(ARG_ACCOUNT);
            user = getArguments().getString(ARG_USER);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        setUpFooter();

        groupEditorAdapter = new GroupEditorAdapter(getActivity(),
                R.layout.group_list_item, new ArrayList<GroupEditorAdapter.Group>());

        setListAdapter(groupEditorAdapter);

        if (savedInstanceState != null) {
            groups = savedInstanceState.getStringArrayList(SAVED_GROUPS);
            selected = savedInstanceState.getStringArrayList(SAVED_SELECTED);
        } else {
            groups = RosterManager.getInstance().getGroups(account);
            selected = RosterManager.getInstance().getGroups(account, user);
        }

    }

    private void setUpFooter() {
        View footerView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.group_add_footer, null, false);
        getListView().addFooterView(footerView);

        groupAddInput = (EditText) footerView.findViewById(R.id.group_add_input);
        groupAddInput.addTextChangedListener(this);

        groupAddCheckBox = (CheckBox) footerView.findViewById(R.id.group_add_checkbox);
        groupAddCheckBox.setVisibility(View.INVISIBLE);
        groupAddCheckBox.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        setGroups(groups, selected);
    }

    void setGroups(Collection<String> groups, Collection<String> selected) {
        ArrayList<String> list = new ArrayList<>(groups);
        Collections.sort(list);
        groupEditorAdapter.clear();

        for (int position = 0; position < list.size(); position++) {
            String groupName = list.get(position);

            GroupEditorAdapter.Group group = new GroupEditorAdapter.Group(groupName, selected.contains(groupName));

            groupEditorAdapter.add(group);
            getListView().setItemChecked(position + getListView().getHeaderViewsCount(),
                    selected.contains(groupName));
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);


        selected = getSelected();

        outState.putStringArrayList(SAVED_GROUPS, getGroups());
        outState.putStringArrayList(SAVED_SELECTED, new ArrayList<>(selected));
    }

    @Override
    public void onPause() {
        super.onPause();

        selected = getSelected();

        try {
            RosterManager.getInstance().setGroups(account, user, selected);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        CheckBox checkBox = (CheckBox) v.findViewById(R.id.group_add_checkbox);
        checkBox.toggle();
        groupEditorAdapter.getItem(position).setIsSelected(checkBox.isChecked());
    }

    protected ArrayList<String> getGroups() {
        ArrayList<String> groups = new ArrayList<>();
        for (int position = 0; position < groupEditorAdapter.getCount(); position++)
            groups.add(groupEditorAdapter.getItem(position).getGroupName());
        return groups;
    }

    public ArrayList<String> getSelected() {
        ArrayList<String> selectedGroups = new ArrayList<>();
        for (int position = 0; position < groupEditorAdapter.getCount(); position++) {

            GroupEditorAdapter.Group item = groupEditorAdapter.getItem(position);

            if (item.isSelected()) {
                selectedGroups.add(item.getGroupName());
            }
        }
        return selectedGroups;
    }

    @Override
    public void afterTextChanged(Editable s) {
        String groupName = groupAddInput.getText().toString().trim();
        if (groupName.isEmpty() || getGroups().contains(groupName)) {
            groupAddCheckBox.setVisibility(View.INVISIBLE);
        } else {
            groupAddCheckBox.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.group_add_checkbox) {
            String groupName = groupAddInput.getText().toString().trim();
            groupEditorAdapter.add(new GroupEditorAdapter.Group(groupName, true));

            groupAddInput.getText().clear();
            groupAddInput.clearFocus();
            hideKeyboard(getActivity());
            groupAddCheckBox.setChecked(false);
        }
    }

    public static void hideKeyboard(Activity activity) {
        // Check if no view has focus:
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }
}
