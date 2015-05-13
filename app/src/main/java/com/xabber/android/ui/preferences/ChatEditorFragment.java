package com.xabber.android.ui.preferences;


import android.app.Activity;
import android.os.Bundle;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.ArchiveMode;
import com.xabber.android.data.message.chat.ChatManager;

import java.util.HashMap;
import java.util.Map;

public class ChatEditorFragment extends BaseSettingsFragment {

    private ChatEditorFragmentInteractionListener mListener;

    @Override
    protected void onInflate(Bundle savedInstanceState) {
        addPreferencesFromResource(R.xml.chat_editor);

        AccountItem accountItem = mListener.getAccountItem();

        if (accountItem.getArchiveMode() == ArchiveMode.server
                || accountItem.getArchiveMode() == ArchiveMode.dontStore) {
            getPreferenceScreen().removePreference(getPreferenceScreen()
                    .findPreference(getString(R.string.chat_save_history_key)));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        saveChanges();
    }

    @Override
    protected Map<String, Object> getValues() {
        Map<String, Object> map = new HashMap<>();
        String account = mListener.getAccount();
        String user = mListener.getUser();

        putValue(map, R.string.chat_save_history_key, ChatManager.getInstance()
                .isSaveMessages(account, user));
        putValue(map, R.string.chat_events_visible_chat_key, ChatManager
                .getInstance().isNotifyVisible(account, user));
        putValue(map, R.string.chat_events_show_text_key, ChatManager
                .getInstance().isShowText(account, user));
        putValue(map, R.string.chat_events_vibro_key, ChatManager.getInstance()
                .isMakeVibro(account, user));
        putValue(map, R.string.chat_events_sound_key, ChatManager.getInstance()
                .getSound(account, user));
        return map;
    }

    @Override
    protected boolean setValues(Map<String, Object> source, Map<String, Object> result) {
        String account = mListener.getAccount();
        String user = mListener.getUser();

        if (hasChanges(source, result, R.string.chat_save_history_key))
            ChatManager.getInstance().setSaveMessages(account, user,
                    getBoolean(result, R.string.chat_save_history_key));

        if (hasChanges(source, result, R.string.chat_events_visible_chat_key))
            ChatManager.getInstance().setNotifyVisible(account, user,
                    getBoolean(result, R.string.chat_events_visible_chat_key));

        if (hasChanges(source, result, R.string.chat_events_show_text_key))
            ChatManager.getInstance().setShowText(account, user,
                    getBoolean(result, R.string.chat_events_show_text_key));

        if (hasChanges(source, result, R.string.chat_events_vibro_key))
            ChatManager.getInstance().setMakeVibro(account, user,
                    getBoolean(result, R.string.chat_events_vibro_key));

        if (hasChanges(source, result, R.string.chat_events_sound_key))
            ChatManager.getInstance().setSound(account, user,
                    getUri(result, R.string.chat_events_sound_key));

        return true;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ChatEditorFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ChatEditorFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface ChatEditorFragmentInteractionListener {
        String getAccount();

        AccountItem getAccountItem();

        String getUser();
    }
}
