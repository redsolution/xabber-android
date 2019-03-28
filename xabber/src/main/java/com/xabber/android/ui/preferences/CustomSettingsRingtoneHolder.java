package com.xabber.android.ui.preferences;

public class CustomSettingsRingtoneHolder extends BaseSoundPrefFragment.RingtoneHolder {

    String vibro;
    boolean showPreview;

    public CustomSettingsRingtoneHolder(String uri, String vibro, boolean showPreview) {
        super(uri);
        this.vibro = vibro;
        this.showPreview = showPreview;
    }
}
