package com.xabber.android.data.extension.references.mutable.voice;

import com.xabber.android.data.extension.references.mutable.filesharing.FileSharingExtension;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class VoiceMessageExtension implements ExtensionElement {

    public static final String VOICE_ELEMENT = "voice-message";
    public static final String VOICE_NAMESPACE = "https://xabber.com/protocol/voice-message";

    private FileSharingExtension voiceFile;

    public VoiceMessageExtension() {}

    public VoiceMessageExtension(FileSharingExtension voiceFile) {
        this.voiceFile = voiceFile;
    }

    public FileSharingExtension getVoiceFile() {
        return voiceFile;
    }

    @Override
    public String getNamespace() {
        return VOICE_NAMESPACE;
    }

    @Override
    public String getElementName() {
        return VOICE_ELEMENT;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        if (voiceFile != null) {
            xml.prelude(VOICE_ELEMENT, VOICE_NAMESPACE);
            xml.rightAngleBracket();
            xml.append(voiceFile.toXML());
            xml.closeElement(VOICE_ELEMENT);
        }
        return xml;
    }
}
