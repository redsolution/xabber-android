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
package com.xabber.android.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.xabber.android.BuildConfig;
import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.connection.WakeLockManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.attention.AttentionManager;
import com.xabber.android.data.extension.carbons.CarbonManager;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.NotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.service.XabberService;
import com.xabber.android.ui.adapter.ComparatorByName;
import com.xabber.android.ui.adapter.ComparatorByStatus;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.Emoticons;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Manage operations with common settings.
 *
 * @author alexander.ivanov
 */
public class SettingsManager implements OnInitializedListener,
        OnMigrationListener, OnSharedPreferenceChangeListener {

    public static final String NOTIFICATION_PREFERENCES = "notification_preferences";

    private static SettingsManager instance;

    private SettingsManager() {
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    public static SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    private static SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(Application
                .getInstance());
    }

    private static SharedPreferences getNotificationSharedPreferences() {
        return Application.getInstance().getSharedPreferences(
                NOTIFICATION_PREFERENCES, Context.MODE_PRIVATE);
    }

    private static int getInt(int key, int def) {
        String value = getString(key, def);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return Integer.parseInt(Application.getInstance().getString(def));
        }
    }

    private static int getInteger(int key, int def) {
        return getSharedPreferences().getInt(Application.getInstance().getString(key), def);
    }

    private static void setInt(int key, int value) {
        Editor editor = getSharedPreferences().edit();
        editor.putInt(Application.getInstance().getString(key), value);
        editor.commit();
    }

    private static boolean getBoolean(int key, boolean def) {
        return getSharedPreferences().getBoolean(
                Application.getInstance().getString(key), def);
    }

    private static boolean getBoolean(int key, int def) {
        return getBoolean(key, Application.getInstance().getResources()
                .getBoolean(def));
    }

    private static void setBoolean(int key, boolean value) {
        Editor editor = getSharedPreferences().edit();
        editor.putBoolean(Application.getInstance().getString(key), value);
        editor.commit();
    }

    private static boolean getNotifBoolean(int key, boolean def) {
        return getNotificationSharedPreferences().getBoolean(
                Application.getInstance().getString(key), def);
    }

    private static boolean getNotifBoolean(int key, int def) {
        return getNotifBoolean(key, Application.getInstance().getResources()
                .getBoolean(def));
    }

    private static String getString(int key, String def) {
        return getSharedPreferences().getString(
                Application.getInstance().getString(key), def);
    }

    private static String getString(int key, int def) {
        return getString(key, Application.getInstance().getString(def));
    }

    private static void setString(int key, String value) {
        Editor editor = getSharedPreferences().edit();
        editor.putString(Application.getInstance().getString(key), value);
        editor.commit();
    }

    private static String getNotifString(int key, String def) {
        return getNotificationSharedPreferences().getString(
                Application.getInstance().getString(key), def);
    }

    private static String getNotifString(int key, int def) {
        return getNotifString(key, Application.getInstance().getString(def));
    }

    private static void setNotifString(int key, String value) {
        Editor editor = getNotificationSharedPreferences().edit();
        editor.putString(Application.getInstance().getString(key), value);
        editor.apply();
    }

    /**
     * @param key
     * @param defaultUri
     * @return Sound uri. Sets defaulUri if value is default.
     */
    private static Uri getSound(int key, Uri defaultUri, int defaultResource) {
        String defaultValue = Application.getInstance().getString(
                defaultResource);
        String value = getNotifString(key, defaultValue);
        if (TextUtils.isEmpty(value))
            return null;
        if (defaultValue.equals(value)) {
            setNotifString(key, defaultUri.toString());
            return defaultUri;
        }
        return Uri.parse(value);
    }

    public static boolean contactsShowAvatars() {
        return getBoolean(R.string.contacts_show_avatars_key,
                R.bool.contacts_show_avatars_default);
    }

    public static boolean contactsShowMessages() {
        return getBoolean(R.string.contacts_show_messages_key,
                R.bool.contacts_show_messages_default);
    }

    public static boolean contactsShowOffline() {
        return getBoolean(R.string.contacts_show_offline_key,
                R.bool.contacts_show_offline_default);
    }

    public static void setContactsShowOffline(boolean show) {
        setBoolean(R.string.contacts_show_offline_key, show);
    }

    public static boolean contactsShowGroups() {
        return getBoolean(R.string.contacts_show_groups_key,
                R.bool.contacts_show_groups_default);
    }

    public static boolean contactsShowEmptyGroups() {
        return getBoolean(R.string.contacts_show_empty_groups_key,
                R.bool.contacts_show_empty_groups_default);
    }

    public static boolean contactsShowAccounts() {
        return getBoolean(R.string.contacts_show_accounts_key,
                R.bool.contacts_show_accounts_default);
    }

    public static Comparator<AbstractContact> contactsOrder() {
        String value = getString(R.string.contacts_order_key,
                R.string.contacts_order_default);
        if (Application.getInstance()
                .getString(R.string.contacts_order_alphabet_value)
                .equals(value))
            return ComparatorByName.COMPARATOR_BY_NAME;
        else if (Application.getInstance()
                .getString(R.string.contacts_order_status_value).equals(value))
            return ComparatorByStatus.COMPARATOR_BY_STATUS;
        else
            throw new IllegalStateException();
    }

    public static boolean contactsEnableShowAccounts() {
        return getBoolean(R.string.contacts_enable_show_accounts_key,
                R.bool.contacts_enable_show_accounts_default);
    }

    /**
     * DON`T USE THIS METHOD DIRECTLY.
     * <p/>
     * Use {@link AccountManager#getSelectedAccount()} instead.
     *
     * @return
     */
    public static String contactsSelectedAccount() {
        return getString(R.string.contacts_selected_account_key, "");
    }

    public static void setContactsSelectedAccount(AccountJid account) {
        String value;
        if (account == null) {
            value = "";
        } else {
            value = account.toString();
        }
        setString(R.string.contacts_selected_account_key, value);
    }

    public static void enableContactsShowAccount() {
        setBoolean(R.string.contacts_enable_show_accounts_key, false);
        setBoolean(R.string.contacts_show_accounts_key, true);
    }

    /**
     * Gets event sound. It will save DEFAULT_NOTIFICATION_URI if value is
     * default.
     *
     * @return {@link Uri} or <code>null</code>.
     */
    public static Uri eventsSound() {
        return getSound(R.string.events_sound_key,
                Settings.System.DEFAULT_NOTIFICATION_URI,
                R.string.events_sound_default);
    }

    public static Uri eventsSoundMuc() {
        return getSound(R.string.events_sound_muc_key,
                Settings.System.DEFAULT_NOTIFICATION_URI,
                R.string.events_sound_default);
    }

    public static VibroMode eventsVibroChat() {
        String value = getNotifString(R.string.events_vibro_chat_key, R.string.events_vibro_bydefault);
        if (Application.getInstance().getString(R.string.events_vibro_disable).equals(value)) {
            return VibroMode.disabled;
        } else if (Application.getInstance().getString(R.string.events_vibro_bydefault).equals(value)) {
            return VibroMode.defaultvibro;
        } else if (Application.getInstance().getString(R.string.events_vibro_short).equals(value)) {
            return VibroMode.shortvibro;
        } else if (Application.getInstance().getString(R.string.events_vibro_long).equals(value)) {
            return VibroMode.longvibro;
        } else if (Application.getInstance().getString(R.string.events_vibro_if_silent).equals(value)) {
            return VibroMode.onlyifsilent;
        } else {
            throw new IllegalStateException();
        }
    }

    public static VibroMode eventsVibroMuc() {
        String value = getNotifString(R.string.events_vibro_muc_key, R.string.events_vibro_bydefault);
        if (Application.getInstance().getString(R.string.events_vibro_disable).equals(value)) {
            return VibroMode.disabled;
        } else if (Application.getInstance().getString(R.string.events_vibro_bydefault).equals(value)) {
            return VibroMode.defaultvibro;
        } else if (Application.getInstance().getString(R.string.events_vibro_short).equals(value)) {
            return VibroMode.shortvibro;
        } else if (Application.getInstance().getString(R.string.events_vibro_long).equals(value)) {
            return VibroMode.longvibro;
        } else if (Application.getInstance().getString(R.string.events_vibro_if_silent).equals(value)) {
            return VibroMode.onlyifsilent;
        } else {
            throw new IllegalStateException();
        }
    }

    @Deprecated
    public static boolean eventsSuppress100() {
        return getBoolean(R.string.chat_events_suppress_100_key,
                R.bool.chat_events_suppress_100_default);
    }

//    public static boolean eventsIgnoreSystemVibro() {
//        return getBoolean(R.string.events_ignore_system_vibro_key,
//                R.bool.events_ignore_system_vibro_default);
//    }

    public static boolean eventsLightning() {
        return getNotifBoolean(R.string.events_lightning_key,
                R.bool.events_lightning_default);
    }

    public static boolean eventsLightningForMuc() {
        return getNotifBoolean(R.string.events_lightning_muc_key,
                R.bool.events_lightning_default);
    }

    public static boolean eventsPersistent() {
        return getBoolean(R.string.events_persistent_key,
                R.bool.events_persistent_default);
    }

    public static boolean eventsShowText() {
        return getNotifBoolean(R.string.events_show_text_key,
                R.bool.events_show_text_default);
    }

    public static boolean eventsShowTextOnMuc() {
        return getNotifBoolean(R.string.events_show_text_muc_key,
                R.bool.events_show_text_default);
    }

    public static boolean eventsOnChat() {
        return getNotifBoolean(R.string.events_on_chat_key,
                R.bool.events_on_chat_default);
    }

    public static boolean eventsOnMuc() {
        return getNotifBoolean(R.string.events_on_muc_key,
                R.bool.events_on_muc_default);
    }

//    public static boolean eventsInAppSounds() {
//        return getBoolean(R.string.events_in_app_sounds_key,
//                R.bool.events_in_app_sounds_default);
//    }
//
//    public static boolean eventsInAppVibrate() {
//        return getBoolean(R.string.events_in_app_vibrate_key,
//                R.bool.events_in_app_vibrate_default);
//    }
//
//    public static boolean eventsInAppPreview() {
//        return getBoolean(R.string.events_in_app_preview_key,
//                R.bool.events_in_app_preview_default);
//    }

    @Deprecated
    public static boolean eventsInChatSounds() {
        return getBoolean(R.string.events_in_chat_sounds_key,
                R.bool.events_in_chat_sounds_default);
    }

    @Deprecated
    public static boolean eventsVisibleChat() {
        return getBoolean(R.string.events_visible_chat_key,
                R.bool.events_visible_chat_default);
    }

    @Deprecated
    public static boolean eventsFirstOnly() {
        return getBoolean(R.string.events_first_only_key,
                R.bool.events_first_only_default);
    }

    public static boolean chatsShowAvatars() {
        return getBoolean(R.string.chats_show_avatars_key,
                R.bool.chats_show_avatars_default);
    }

    public static boolean chatsShowAvatarsMUC() {
        return getBoolean(R.string.chats_show_avatars_muc_key,
                R.bool.chats_show_avatars_muc_default);
    }

    public static boolean chatsSendByEnter() {
        return getBoolean(R.string.chats_send_by_enter_key,
                R.bool.chats_send_by_enter_default);
    }

    public static ChatsShowStatusChange chatsShowStatusChange() {
        String value = getString(R.string.chats_show_status_change_key,
                R.string.chats_show_status_change_default);
        if (Application.getInstance()
                .getString(R.string.chats_show_status_change_always_value)
                .equals(value))
            return ChatsShowStatusChange.always;
        else if (Application.getInstance()
                .getString(R.string.chats_show_status_change_muc_value)
                .equals(value))
            return ChatsShowStatusChange.muc;
        else if (Application.getInstance()
                .getString(R.string.chats_show_status_change_never_value)
                .equals(value))
            return ChatsShowStatusChange.never;
        else
            throw new IllegalStateException();

    }

    public static ChatsHideKeyboard chatsHideKeyboard() {
        String value = getString(R.string.chats_hide_keyboard_key,
                R.string.chats_hide_keyboard_default);
        if (Application.getInstance()
                .getString(R.string.chats_hide_keyboard_always_value)
                .equals(value))
            return ChatsHideKeyboard.always;
        else if (Application.getInstance()
                .getString(R.string.chats_hide_keyboard_landscape_value)
                .equals(value))
            return ChatsHideKeyboard.landscape;
        else if (Application.getInstance()
                .getString(R.string.chats_hide_keyboard_never_value)
                .equals(value))
            return ChatsHideKeyboard.never;
        else
            throw new IllegalStateException();
    }

    public static boolean chatsShowBackground() {
        return getBoolean(R.string.chats_show_background_key, R.bool.chats_show_background_default);
    }

    public static int chatsAppearanceStyle() {
        String value = getString(R.string.chats_font_size_key,
                R.string.chats_font_size_default);
        if (Application.getInstance()
                .getString(R.string.chats_font_size_small_value).equals(value))
            return R.style.ChatText_Small;
        else if (Application.getInstance()
                .getString(R.string.chats_font_size_normal_value).equals(value))
            return R.style.ChatText_Normal;
        else if (Application.getInstance()
                .getString(R.string.chats_font_size_large_value).equals(value))
            return R.style.ChatText_Large;
        else if (Application.getInstance()
                .getString(R.string.chats_font_size_xlarge_value).equals(value))
            return R.style.ChatText_XLarge;
        else
            throw new IllegalStateException();
    }

    public static boolean chatsStateNotification() {
        return getBoolean(R.string.chats_state_notification_key,
                R.bool.chats_state_notification_default);
    }

    public static boolean chatsAttention() {
        return getNotifBoolean(R.string.chats_attention_key,
                R.bool.chats_attention_default);
    }

    /**
     * Gets event sound. It will save DEFAULT_NOTIFICATION_URI if value is
     * default.
     *
     * @return {@link Uri} or <code>null</code>.
     */
    public static Uri chatsAttentionSound() {
        return getSound(R.string.chats_attention_sound_key,
                Settings.System.DEFAULT_RINGTONE_URI,
                R.string.chats_attention_sound_default);
    }

    public static int connectionGoAway() {
        return getInt(R.string.connection_go_away_key,
                R.string.connection_go_away_default);
    }

    public static int connectionGoXa() {
        return getInt(R.string.connection_go_xa_key,
                R.string.connection_go_xa_default);
    }

    public static boolean connectionWifiLock() {
        return getBoolean(R.string.connection_wifi_lock_key,
                R.bool.connection_wifi_lock_default);
    }

    public static boolean connectionWakeLock() {
        return getBoolean(R.string.connection_wake_lock_key,
                R.bool.connection_wake_lock_default);
    }

    public static boolean connectionStartAtBoot() {
        return getBoolean(R.string.connection_start_at_boot_key,
                R.bool.connection_start_at_boot_default);
    }

    public static void setConnectionStartAtBoot(boolean value) {
        setBoolean(R.string.connection_start_at_boot_key, value);
    }

    public static boolean connectionLoadVCard() {
        return getBoolean(R.string.connection_load_vcard_key,
                R.bool.connection_load_vcard_default);
    }

    public static boolean connectionLoadImages() {
        return getBoolean(R.string.connection_load_images_key,
                R.bool.connection_load_images_default);
    }

    public static boolean connectionUseCarbons() {
        return getBoolean(R.string.connection_use_carbons_key,
                R.bool.connection_use_carbons_default);
    }

    public static DnsResolverType connectionDnsResolver() {
        String value = getString(R.string.connection_dns_resolver_type_key,
                R.string.connection_dns_resolver_type_default);
        return getDnsResolverType(value);
    }

    @NonNull
    public static DnsResolverType getDnsResolverType(String value) {
        if (Application.getInstance().getString(R.string.connection_dns_resolver_type_dns_java_resolver_value).equals(value)) {
            return DnsResolverType.dnsJavaResolver;
        } else if (Application.getInstance().getString(R.string.connection_dns_resolver_type_mini_dns_resolver_value).equals(value)) {
            return DnsResolverType.miniDnsResolver;
        } else {
            throw new IllegalStateException("Unknown preference value for DNS resolver type");
        }
    }

    public static boolean connectionUsePlainTextAuth() {
        return getBoolean(R.string.connection_use_plain_text_auth_key,
                R.bool.connection_use_plain_text_auth_default);
    }

    public static boolean connectionAdjustPriority() {
        return getBoolean(R.string.connection_adjust_priority_key,
                R.bool.connection_adjust_priority_default);
    }

    public static int connectionPriorityAvailable() {
        return getInt(R.string.connection_priority_available_key,
                R.string.connection_priority_available_default);
    }

    public static int connectionPriorityAway() {
        return getInt(R.string.connection_priority_away_key,
                R.string.connection_priority_away_default);
    }

    public static int connectionPriorityChat() {
        return getInt(R.string.connection_priority_chat_key,
                R.string.connection_priority_chat_default);
    }

    public static int connectionPriorityDnd() {
        return getInt(R.string.connection_priority_dnd_key,
                R.string.connection_priority_dnd_default);
    }

    public static int connectionPriorityXa() {
        return getInt(R.string.connection_priority_xa_key,
                R.string.connection_priority_xa_default);
    }

    public static boolean debugLog() {
        return getBoolean(R.string.debug_log_key, R.bool.debug_log_default);
    }

    public static boolean fileLog() {
        return getBoolean(R.string.debug_file_log_key, R.bool.debug_file_log_default);
    }

    public static boolean showConnectionErrors() {
        return getBoolean(R.string.debug_connection_errors_key, R.bool.debug_connection_errors_default);
    }

    public static boolean sendCrashReports() {
        return getBoolean(R.string.debug_crash_reports_key, R.bool.debug_crash_reports_default);
    }

    public static boolean useDevelopAPI() {
        return getBoolean(R.string.debug_use_develop_api_key, R.bool.debug_use_develop_api_default);
    }

    public static boolean isCrashReportsSupported() {
        return BuildConfig.FLAVOR.equals("beta")
                || BuildConfig.FLAVOR.equals("vip")
                || BuildConfig.FLAVOR.equals("prod");

    }

    public static boolean isCrashReportsDialogShown() {
        return getBoolean(R.string.debug_crash_reports_dialog_key, false);
    }

    public static void setCrashReportsDialogShown() {
        setBoolean(R.string.debug_crash_reports_dialog_key, true);
    }

    public static InterfaceTheme interfaceTheme() {
        String value = getString(R.string.interface_theme_key, R.string.interface_theme_default);
        if (Application.getInstance().getString(R.string.interface_theme_dark_value).equals(value)) {
            return InterfaceTheme.dark;
        } else if (Application.getInstance().getString(R.string.interface_theme_light_value).equals(value)) {
            return InterfaceTheme.light;
        } else if (Application.getInstance().getString(R.string.interface_theme_normal_value).equals(value)) {
            return InterfaceTheme.light;
        } else {
            throw new IllegalStateException();
        }
    }

    public static Map<Pattern, Integer> interfaceSmiles() {
        String value = getString(R.string.interface_smiles_key, R.string.interface_smiles_default);
        if (Application.getInstance().getString(R.string.interface_smiles_none_value).equals(value)) {
            return Collections.unmodifiableMap(Emoticons.NONE_EMOTICONS);
        } else if (Application.getInstance().getString(R.string.interface_smiles_android_value).equals(value)) {
            return Collections.unmodifiableMap(Emoticons.ANDROID_EMOTICONS);
        } else {
            throw new IllegalStateException();
        }
    }

    public static boolean securityCheckCertificate() {
        return getBoolean(R.string.security_check_certificate_key,
                R.bool.security_check_certificate_default);
    }

    public static SecurityOtrMode securityOtrMode() {
        String value = getString(R.string.security_otr_mode_key,
                R.string.security_otr_mode_default);
        if (Application.getInstance()
                .getString(R.string.security_otr_mode_disabled_value)
                .equals(value))
            return SecurityOtrMode.disabled;
        else if (Application.getInstance()
                .getString(R.string.security_otr_mode_manual_value)
                .equals(value))
            return SecurityOtrMode.manual;
        else if (Application.getInstance()
                .getString(R.string.security_otr_mode_auto_value).equals(value))
            return SecurityOtrMode.auto;
        else if (Application.getInstance()
                .getString(R.string.security_otr_mode_required_value)
                .equals(value))
            return SecurityOtrMode.required;
        else
            throw new IllegalStateException();
    }

    public static SpamFilterMode spamFilterMode() {
        String value = getString(R.string.spam_filter_key, R.string.spam_filter_default);

        if (Application.getInstance().getString(R.string.spam_filter_1_value).equals(value))
            return SpamFilterMode.disabled;

        else if (Application.getInstance().getString(R.string.spam_filter_2_value).equals(value))
            return SpamFilterMode.onlyRoster;

        else if (Application.getInstance().getString(R.string.spam_filter_3_value).equals(value))
            return SpamFilterMode.authCaptcha;

        else if (Application.getInstance().getString(R.string.spam_filter_4_value).equals(value))
            return SpamFilterMode.noAuth;

        else throw new IllegalStateException();
    }

    public static boolean securityOtrHistory() {
        return getBoolean(R.string.security_otr_history_key,
                R.bool.security_otr_history_default);
    }

    public static int bootCount() {
        return getSharedPreferences()
                .getInt(Application.getInstance().getString(
                        R.string.boot_count_key), 0);
    }

    public static void incrementBootCount() {
        Editor editor = getSharedPreferences().edit();
        editor.putInt(
                Application.getInstance().getString(R.string.boot_count_key),
                bootCount() + 1);
        editor.commit();
    }

    public static boolean startAtBootSuggested() {
        return getBoolean(R.string.start_at_boot_suggested_key, false);
    }

    public static void setStartAtBootSuggested() {
        setBoolean(R.string.start_at_boot_suggested_key, true);
    }

    public static boolean chatShowcaseSuggested() {
        return getBoolean(R.string.chat_showcase_suggested_key, false);
    }

    public static void setChatShowcaseSuggested() {
        setBoolean(R.string.chat_showcase_suggested_key, true);
    }

    public static boolean contactShowcaseSuggested() {
        return getBoolean(R.string.contact_showcase_suggested_key, false);
    }

    public static void setContactShowcaseSuggested() {
        setBoolean(R.string.contact_showcase_suggested_key, true);
    }

    public static boolean contactIntegrationSuggested() {
        return getBoolean(R.string.contact_integration_suggested_key, false);
    }

    public static void setContactIntegrationSuggested() {
        setBoolean(R.string.contact_integration_suggested_key, true);
    }

    public static boolean isTranslationSuggested() {
        return getBoolean(R.string.translation_suggested_key, false);
    }

    public static void setTranslationSuggested() {
        setBoolean(R.string.translation_suggested_key, true);
    }

    public static boolean isDarkThemeSuggested() {
        return getBoolean(R.string.dark_theme_suggested_key, false);
    }

    public static void setDarkThemeSuggested() {
        setBoolean(R.string.dark_theme_suggested_key, true);
    }

    public static void setDarkTheme() {
        setString(R.string.interface_theme_key, Application.getInstance().getString(
                R.string.interface_theme_dark_value));
    }

    public static boolean isBatteryOptimizationDisableSuggested() {
        return getBoolean(R.string.battery_optimization_disable_suggested_key, false);
    }

    public static void setBatteryOptimizationDisableSuggested() {
        setBoolean(R.string.battery_optimization_disable_suggested_key, true);
    }

    /**
     * @return Common status mode for all accounts or
     * {@link StatusMode#available} if mode was not set.
     */
    public static StatusMode statusMode() {
        return StatusMode.valueOf(getString(R.string.status_mode_key,
                StatusMode.available.name()));
    }

    public static void setStatusMode(StatusMode statusMode) {
        setString(R.string.status_mode_key, statusMode.name());
    }

    /**
     * @return Common status text for all accounts.
     */
    public static String statusText() {
        return getString(R.string.status_text_key, "");
    }

    public static void setStatusText(String statusText) {
        setString(R.string.status_text_key, statusText);
    }

    public static void setLastSyncDate(String lastSyncDate) {
        setString(R.string.last_sync_date_key, lastSyncDate);
    }

    public static String getLastSyncDate() {
        return getString(R.string.last_sync_date_key, R.string.last_sync_date_default);
    }

    public static void setSyncAllAccounts(boolean syncAll) {
        setBoolean(R.string.sync_all_key, syncAll);
        if (syncAll) XabberAccountManager.getInstance().setAllExistingAccountSync(true);
    }

    public static boolean isSyncAllAccounts() {
        if (AccountManager.getInstance().haveNotAllowedSyncAccounts()) return false;
        return getBoolean(R.string.sync_all_key, true);
    }

    public static void setLastOrderChangeTimestamp(int lastOrderChange) {
        setInt(R.string.order_last_timestamp_key, lastOrderChange);
    }

    public static int getLastOrderChangeTimestamp() {
        return getInteger(R.string.order_last_timestamp_key, 1);
    }

    public static void setLastPatreonLoadTimestamp(int timestamp) {
        setInt(R.string.patreon_last_load_timestamp_key, timestamp);
    }

    public static int getLastPatreonLoadTimestamp() {
        return getInteger(R.string.patreon_last_load_timestamp_key, 1);
    }

    public static void setLastCrowdfundingLoadTimestamp(int timestamp) {
        setInt(R.string.crowdfunding_last_load_timestamp_key, timestamp);
    }

    public static int getLastCrowdfundingLoadTimestamp() {
        return getInteger(R.string.crowdfunding_last_load_timestamp_key, 1);
    }

    public static void setLastLeaderCrowdfundingLoadTimestamp(int timestamp) {
        setInt(R.string.crowdfunding_leader_last_load_timestamp_key, timestamp);
    }

    public static int getLastLeaderCrowdfundingLoadTimestamp() {
        return getInteger(R.string.crowdfunding_leader_last_load_timestamp_key, 1);
    }

    public static void setFirstAppRunTimestamp(int timestamp) {
        setInt(R.string.first_app_run_timestamp_key, timestamp);
    }

    public static int getFirstAppRunTimestamp() {
        return getInteger(R.string.first_app_run_timestamp_key, 0);
    }

    public static void setLastCrowdfundingPosition(int position) {
        setInt(R.string.crowdfunding_last_position_key, position);
    }

    public static int getLastCrowdfundingPosition() {
        return getInteger(R.string.crowdfunding_last_position_key, 0);
    }

    public static void resetPreferences(Context context, String preferencesName) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear().apply();
    }

    @Override
    public void onInitialized() {
        incrementBootCount();
    }

    @Override
    public void onMigrate(int toVersion) {
        switch (toVersion) {
            case 32:
                setBoolean(R.string.chats_show_status_change_key, false);
                break;
            case 40:
                String value;
                try {
                    if (getBoolean(R.string.chats_show_status_change_key, false))
                        value = Application.getInstance().getString(
                                R.string.chats_show_status_change_always_value);
                    else
                        value = Application.getInstance().getString(
                                R.string.chats_show_status_change_muc_value);
                } catch (ClassCastException e) {
                    value = Application.getInstance().getString(
                            R.string.chats_show_status_change_default);
                }
                setString(R.string.chats_show_status_change_key, value);
                break;
            case 45:
                setBoolean(R.string.chats_show_avatars_key,
                        "message".equals(getString(R.string.chats_show_avatars_key,
                                "")));
                break;
            case 65:
                SharedPreferences settings = Application.getInstance()
                        .getSharedPreferences("accounts", Context.MODE_PRIVATE);
                int statusModeIndex = settings.getInt("status_mode",
                        StatusMode.available.ordinal());
                StatusMode statusMode = StatusMode.values()[statusModeIndex];
                setString(R.string.status_mode_key, statusMode.name());
                String statusText = settings.getString("status_text", "");
                setString(R.string.status_text_key, statusText);
                break;
            default:
                break;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals(Application.getInstance().getString(
                R.string.chats_show_status_change_key))) {
            MessageManager.getInstance().onSettingsChanged();
        } else if (key.equals(Application.getInstance().getString(
                R.string.events_persistent_key))) {
            NotificationManager.getInstance().onMessageNotification();
            XabberService.getInstance().changeForeground();
        } else if (key.equals(Application.getInstance().getString(
                R.string.connection_wake_lock_key))) {
            WakeLockManager.onWakeLockSettingsChanged();
        } else if (key.equals(Application.getInstance().getString(
                R.string.connection_wifi_lock_key))) {
            WakeLockManager.onWifiLockSettingsChanged();
        } else if (key.equals(Application.getInstance().getString(
                R.string.connection_use_carbons_key))) {
            CarbonManager.getInstance().onUseCarbonsSettingsChanged();
        } else if (key.equals(Application.getInstance().getString(
                R.string.events_show_text_key))) {
            NotificationManager.getInstance().onMessageNotification();
        } else if (key.equals(Application.getInstance().getString(
                R.string.chats_attention_key))) {
            AttentionManager.getInstance().onSettingsChanged();
        } else if (key.equals(Application.getInstance().getString(
                R.string.security_otr_mode_key))) {
            OTRManager.getInstance().onSettingsChanged();
        } else if (key.equals(Application.getInstance().getString(
                R.string.interface_theme_key))) {
            ColorManager.getInstance().onSettingsChanged();
            if (SettingsManager.interfaceTheme() == InterfaceTheme.dark) {
                SettingsManager.setDarkThemeSuggested();
            }
        } else if(key.equals(Application.getInstance().getString(
                R.string.debug_file_log_key))) {
            LogManager.getInstance().onSettingsChanged();
        }
    }

    public enum ChatsHistory {

        /**
         * Don't store chat messages.
         */
        none,

        /**
         * Store only unread messages.
         */
        unread,

        /**
         * Store all messages.
         */
        all

    }

    public enum InterfaceTheme {

        /**
         * All windows will be dark.
         */
        dark,

        /**
         * All windows will be light.
         */
        light,

        /**
         * Chat will be light, other windows will be dark.
         */
        normal

    }

    public enum DnsResolverType {
        /**
         * Use DNS resolver based on dnsjava
         * http://dnsjava.org/
         */
        dnsJavaResolver,

        /**
         * Use DNS resolver based on MiniDNS - experimental
         * https://github.com/rtreffer/minidns
         */
        miniDnsResolver
    }

    public enum EventsMessage {

        /**
         * Never notify.
         */
        none,

        /**
         * Notify in chat only.
         */
        chat,

        /**
         * Notify in chat and muc.
         */
        chatAndMuc

    }

    public enum ChatsShowStatusChange {

        /**
         * Always show status change.
         */
        always,

        /**
         * Show status change only in MUC.
         */
        muc,

        /**
         * Never show status change.
         */
        never

    }

    public enum ChatsHideKeyboard {

        /**
         * Always hide keyboard.
         */
        always,

        /**
         * Hide keyboard only in landscape mode.
         */
        landscape,

        /**
         * Never hide keyboard.
         */
        never,
    }

    public enum SecurityOtrMode {

        /**
         * OTR is disabled.
         */
        disabled,

        /**
         * Manually send request and confirm requests.
         */
        manual,

        /**
         * Automatically try to use OTR.
         */
        auto,

        /**
         * Require to use OTR.
         */
        required

    }

    public enum SpamFilterMode {
        /**
         * Spam filter is disabled.
         */
        disabled,

        /**
         * Receiving messages only from roster.
         */
        onlyRoster,

        /**
         * Receiving messages only from roster.
         * Auth requests only with captcha.
         */
        authCaptcha,

        /**
         * Receiving messages only from roster.
         * No auth requests.
         */
        noAuth
    }

    public enum VibroMode {
        /**
         * Vibrate is disabled.
         */
        disabled,

        /**
         * Default vibration
         */
        defaultvibro,

        /**
         * Short vibration
         */
        shortvibro,

        /**
         * Long vibration
         */
        longvibro,

        /**
         * Vibration only in silent mode
         */
        onlyifsilent
    }

}