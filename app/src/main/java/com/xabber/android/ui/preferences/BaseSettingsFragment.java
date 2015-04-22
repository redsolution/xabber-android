package com.xabber.android.ui.preferences;


import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.ui.helper.PreferenceSummaryHelper;
import com.xabber.android.ui.widget.RingtonePreference;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        onInflate(savedInstanceState);

        PreferenceSummaryHelper.updateSummary(getPreferenceScreen());
        if (savedInstanceState == null)
            operation(Operation.read);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        for (int index = 0; index < preferenceScreen.getPreferenceCount(); index++) {
            Preference preference = preferenceScreen.getPreference(index);
            preference.setOnPreferenceChangeListener(this);
            if (preference instanceof EditTextPreference)
                onPreferenceChange(preference,
                        ((EditTextPreference) preference).getText());
            else if (preference instanceof CheckBoxPreference)
                onPreferenceChange(preference,
                        ((CheckBoxPreference) preference).isChecked());
            else if (preference instanceof ListPreference)
                onPreferenceChange(preference,
                        ((ListPreference) preference).getValue());
        }

        return view;
    }

    /**
     * Inflates layout.
     *
     * @param savedInstanceState
     */
    protected abstract void onInflate(Bundle savedInstanceState);

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof EditTextPreference)
            preference.setSummary((String) newValue);
        return true;
    }

    /**
     * Possible operations.
     */
    private static enum Operation {
        save, discard, read
    }

    protected void putValue(Map<String, Object> map, int resoureId, Object value) {
        map.put(getString(resoureId), value);
    }

    protected String getString(Map<String, Object> map, int resoureId) {
        return (String) map.get(getString(resoureId));
    }

    protected int getInt(Map<String, Object> map, int resoureId) {
        return (Integer) map.get(getString(resoureId));
    }

    protected boolean getBoolean(Map<String, Object> map, int resoureId) {
        return (Boolean) map.get(getString(resoureId));
    }

    protected Uri getUri(Map<String, Object> map, int resoureId) {
        return (Uri) map.get(getString(resoureId));
    }

    /**
     * @param intent
     * @return Whether an operation succeed.
     */
    private boolean operation(Operation selected) {
        Map<String, Object> source = getValues();
        if (selected == Operation.read)
            setPreferences(source);
        else {
            Map<String, Object> result = getPreferences(source);
            for (Map.Entry<String, Object> entry : source.entrySet())
                if (!result.containsKey(entry.getKey()))
                    result.put(entry.getKey(), entry.getValue());
            if (selected == Operation.save)
                return setValues(source, result);
            else if (selected == Operation.discard) {
                for (String key : source.keySet())
                    if (hasChanges(source, result, key))
                        return false;
            } else
                throw new IllegalStateException();
        }
        return true;
    }

    /**
     * @param source
     * @param result
     * @param key
     * @return Whether value has been changed.
     */
    protected boolean hasChanges(Map<String, Object> source,
                                 Map<String, Object> result, String key) {
        Object sourceValue = source.get(key);
        Object targetValue = result.get(key);
        return (sourceValue == null && targetValue != null)
                || (sourceValue != null && !sourceValue.equals(targetValue));
    }

    /**
     * @param source
     * @param result
     * @param resourceId
     * @return Whether value has been changed.
     */
    protected boolean hasChanges(Map<String, Object> source,
                                 Map<String, Object> result, int resourceId) {
        return hasChanges(source, result, getString(resourceId));
    }

    /**
     * @return Map with source values.
     */
    protected abstract Map<String, Object> getValues();

    /**
     * Set values to the UI elements.
     *
     * @param source
     */
    protected void setPreferences(Map<String, Object> source) {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        for (int index = 0; index < preferenceScreen.getPreferenceCount(); index++) {
            Preference preference = preferenceScreen.getPreference(index);
            Object value = source.get(preference.getKey());
            setPreference(preference, value);
        }
    }

    /**
     * Set value to the UI element.
     *
     * @param preference
     * @param value
     */
    protected void setPreference(Preference preference, Object value) {
        if (preference instanceof EditTextPreference)
            ((EditTextPreference) preference)
                    .setText(value instanceof Integer ? String.valueOf(value)
                            : (String) value);
        else if (preference instanceof CheckBoxPreference)
            ((CheckBoxPreference) preference).setChecked((Boolean) value);
        else if (preference instanceof ListPreference)
            ((ListPreference) preference).setValueIndex((Integer) value);
        else if (preference instanceof RingtonePreference)
            ((RingtonePreference) preference).setUri((Uri) value);
    }

    /**
     * Get values from the UI elements.
     *
     * @param source
     * @return
     */
    protected Map<String, Object> getPreferences(Map<String, Object> source) {
        Map<String, Object> result = new HashMap<>();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        for (int index = 0; index < preferenceScreen.getPreferenceCount(); index++) {
            Preference preference = preferenceScreen.getPreference(index);
            result.put(preference.getKey(), getPreference(preference, source));
        }
        return result;
    }

    /**
     * Get value from the UI element.
     *
     * @param preference
     * @param source
     * @return
     */
    protected Object getPreference(Preference preference, Map<String, Object> source) {
        if (preference instanceof PreferenceScreen) {
            return null;
        } else if (preference instanceof EditTextPreference) {
            String value = ((EditTextPreference) preference).getText();
            if (source.get(preference.getKey()) instanceof Integer)
                try {
                    return Integer.parseInt(value);
                } catch (Exception NumberFormatException) {
                    return null;
                }
            else
                return value;
        } else if (preference instanceof CheckBoxPreference)
            return ((CheckBoxPreference) preference).isChecked();
        else if (preference instanceof ListPreference)
            return ((ListPreference) preference)
                    .findIndexOfValue(((ListPreference) preference).getValue());
        else if (preference instanceof RingtonePreference)
            return ((RingtonePreference) preference).getUri();
        throw new IllegalStateException();
    }

    /**
     * Apply result values.
     *
     * @param source
     * @param result
     * @return Whether operation succeed.
     */
    protected abstract boolean setValues(Map<String, Object> source, Map<String, Object> result);


    protected boolean saveChanges() {
        return operation(Operation.save);
    }

}
