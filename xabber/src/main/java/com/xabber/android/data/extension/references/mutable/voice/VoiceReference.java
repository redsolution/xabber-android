package com.xabber.android.data.extension.references.mutable.voice;

import com.xabber.android.data.extension.references.mutable.Mutable;

import org.jivesoftware.smack.util.XmlStringBuilder;

import java.util.Collections;
import java.util.List;

public class VoiceReference extends Mutable {

    private List<VoiceMessageExtension> voiceMessageExtensions;

    public VoiceReference(int begin, int end, VoiceMessageExtension voiceFile) {
        super(begin, end);
        voiceMessageExtensions = Collections.singletonList(voiceFile);
    }

    public VoiceReference(int begin, int end, List<VoiceMessageExtension> voiceFiles) {
        super(begin, end);
        voiceMessageExtensions = voiceFiles;
    }

    public List<VoiceMessageExtension> getVoiceMessageExtensions() {
        return voiceMessageExtensions;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) {
        if (voiceMessageExtensions != null && !voiceMessageExtensions.isEmpty()) {
            for (VoiceMessageExtension file : voiceMessageExtensions) {
                xml.append(file.toXML());
            }
        }
    }
}
