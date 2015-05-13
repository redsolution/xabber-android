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

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.GroupEditorAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class GroupEditorFragment extends ListFragment implements TextWatcher, View.OnClickListener {

    protected static final String ARG_ACCOUNT = "com.xabber.android.ui.GroupEditorFragment.ARG_ACCOUNT";
    protected static final String ARG_USER = "com.xabber.android.ui.GroupEditorFragment.ARG_USER";

    private static final String SAVED_GROUPS = "com.xabber.android.ui.GroupEditorFragment.SAVED_GROUPS";
    private static final String SAVED_SELECTED = "com.xabber.android.ui.GroupEditorFragment.SAVED_SELECTED";
    private static final String SAVED_ADD_GROUP_NAME = "com.xabber.android.ui.GroupEditorFragment.SAVED_ADD_GROUP_NAME";

    private String account;
    private String user;

    private GroupEditorAdapter groupEditorAdapter;

    private Collection<String> groups;
    private Collection<String> selected = new HashSet<>();

    private EditText groupAddInput;
    private CheckBox groupAddCheckBox;
    private View footerView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public GroupEditorFragment() {
    }

    public static GroupEditorFragment newInstance(String account, String user) {
        GroupEditorFragment fragment = new GroupEditorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACCOUNT, account);
        args.putString(ARG_USER, user);
        fragment.setArguments(args);
        return fragment;
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
            groupAddInput.setText(savedInstanceState.getString(SAVED_ADD_GROUP_NAME));
        } else {
            setAccountGroups();
            if (user != null) {
                selected = RosterManager.getInstance().getGroups(account, user);
            }
        }

    }

    protected void setAccountGroups() {
        groups = RosterManager.getInstance().getGroups(account);
    }

    private void setUpFooter() {
        footerView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.group_add_footer, null, false);
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

        updateGroups();
    }

    protected void updateGroups() {
        ArrayList<String> list = new ArrayList<>(groups);
        Collections.sort(list);
        groupEditorAdapter.clear();

        for (int position = 0; position < list.size(); position++) {
            String groupName = list.get(position);

            GroupEditorAdapter.Group group = new GroupEditorAdapter.Group(groupName, selected.contains(groupName));

            groupEditorAdapter.add(group);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);


        selected = getSelected();

        outState.putStringArrayList(SAVED_GROUPS, getGroups());
        outState.putStringArrayList(SAVED_SELECTED, new ArrayList<>(selected));
        outState.putString(SAVED_ADD_GROUP_NAME, groupAddInput.getText().toString());
    }

    @Override
    public void onPause() {
        super.onPause();

        selected = getSelected();

        if (account != null && !"".equals(user)) {
            try {
                RosterManager.getInstance().setGroups(account, user, selected);
            } catch (NetworkException e) {
                Application.getInstance().onError(e);
            }
        }

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        CheckBox checkBox = (CheckBox) v.findViewById(R.id.group_item_selected_checkbox);
        checkBox.toggle();

        GroupEditorAdapter.Group group = groupEditorAdapter.getItem(position - getListView().getHeaderViewsCount());

        group.setIsSelected(checkBox.isChecked());
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

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    protected String getAccount() {
        return account;
    }

    protected void setAccount(String account) {
        this.account = account;
    }

    protected String getUser() {
        return user;
    }

    protected void setUser(String user) {
        this.user = user;
    }

    protected View getFooterView() {
        return footerView;
    }
}
