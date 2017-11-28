package com.xabber.android.ui.preferences;


import android.app.Activity;
import android.os.Bundle;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.ShowMessageTextInNotification;

import java.util.HashMap;
import java.util.Map;

public class ChatContactSettingsFragment extends BaseSettingsFragment {

    private ChatEditorFragmentInteractionListener mListener;

    @Override
    protected void onInflate(Bundle savedInstanceState) {
        addPreferencesFromResource(R.xml.preference_chat_contact);
        getPreferenceScreen().removePreference(getPreferenceScreen()
                .findPreference(getString(R.string.chat_save_history_key)));
    }

    @Override
    public void onPause() {
        super.onPause();
        saveChanges();
    }

    @Override
    protected Map<String, Object> getValues() {
        Map<String, Object> map = new HashMap<>();
        AccountJid account = mListener.getAccount();
        UserJid user = mListener.getUser();

        boolean isMUC = false;
        if (MUCManager.getInstance().hasRoom(account, user.getJid().asEntityBareJidIfPossible())) {
            isMUC = true;
        }

        // custom notification
        AbstractChat chat = MessageManager.getInstance().getChat(account, user);
        if (chat != null) {
            putValue(map, R.string.chat_notification_settings_key, chat.notifyAboutMessage());
        }

        putValue(map, R.string.chat_save_history_key, ChatManager.getInstance()
                .isSaveMessages(account, user));
        putValue(map, R.string.chat_events_visible_chat_key, ChatManager
                .getInstance().isNotifyVisible(account, user));
        putValue(map, R.string.chat_events_show_text_key, ChatManager
                .getInstance().getShowText(account, user).ordinal());

        putValue(map, R.string.chat_events_vibro_key, ChatManager.getInstance()
                .isMakeVibro(account, user));
        putValue(map, R.string.chat_events_sound_key, ChatManager.getInstance()
                .getSound(account, user, isMUC));
        putValue(map, R.string.chat_events_suppress_100_key, ChatManager.getInstance()
                .isSuppress100(account, user));
        return map;
    }

    @Override
    protected boolean setValues(Map<String, Object> source, Map<String, Object> result) {
        AccountJid account = mListener.getAccount();
        UserJid user = mListener.getUser();

        // custom notification
        if (hasChanges(source, result, R.string.chat_notification_settings_key)) {
            AbstractChat chat = MessageManager.getInstance().getChat(account, user);
            if (chat != null) {
                boolean newValue = getBoolean(result, R.string.chat_notification_settings_key);
                if (chat.getNotificationState().getMode().equals(NotificationState.NotificationMode.bydefault)) {

                    NotificationState.NotificationMode mode = newValue
                            ? NotificationState.NotificationMode.enabled
                            : NotificationState.NotificationMode.disabled;

                    chat.setNotificationState(new NotificationState(mode, 0), true);

                } else {

                    boolean defValue;
                    if (MUCManager.getInstance().hasRoom(account, user.getJid().asEntityBareJidIfPossible()))
                        defValue = SettingsManager.eventsOnMuc();
                    else defValue = SettingsManager.eventsOnChat();

                    if (!defValue && chat.getNotificationState().getMode()
                            .equals(NotificationState.NotificationMode.disabled)) {
                        chat.setNotificationState(new NotificationState(NotificationState.NotificationMode.enabled,
                                0), true);
                    } else if (defValue && chat.getNotificationState().getMode()
                            .equals(NotificationState.NotificationMode.enabled)) {
                        chat.setNotificationState(new NotificationState(NotificationState.NotificationMode.disabled,
                                0), true);
                    } else chat.setNotificationState(new NotificationState(NotificationState.NotificationMode.bydefault,
                            0), true);
                }
            }
        }

        if (hasChanges(source, result, R.string.chat_save_history_key))
            ChatManager.getInstance().setSaveMessages(account, user,
                    getBoolean(result, R.string.chat_save_history_key));

        if (hasChanges(source, result, R.string.chat_events_visible_chat_key))
            ChatManager.getInstance().setNotifyVisible(account, user,
                    getBoolean(result, R.string.chat_events_visible_chat_key));

        if (hasChanges(source, result, R.string.chat_events_show_text_key)) {
            ChatManager.getInstance().setShowText(account, user,
                    ShowMessageTextInNotification.fromInteger(getInt(result, R.string.chat_events_show_text_key)));
        }

        if (hasChanges(source, result, R.string.chat_events_vibro_key))
            ChatManager.getInstance().setMakeVibro(account, user,
                    getBoolean(result, R.string.chat_events_vibro_key));

        if (hasChanges(source, result, R.string.chat_events_sound_key))
            ChatManager.getInstance().setSound(account, user,
                    getUri(result, R.string.chat_events_sound_key));

        if (hasChanges(source, result, R.string.chat_events_suppress_100_key))
            ChatManager.getInstance().setSuppress100(account, user,
                    getBoolean(result, R.string.chat_events_suppress_100_key));
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
        AccountJid getAccount();
        AccountItem getAccountItem();
        UserJid getUser();
    }
}
