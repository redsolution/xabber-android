package com.xabber.android.data.extension.references.mutable.filesharing;

import com.xabber.android.data.extension.references.mutable.Mutable;

import org.jivesoftware.smack.util.XmlStringBuilder;

import java.util.Collections;
import java.util.List;

public class FileReference extends Mutable {

    private List<FileSharingExtension> fileSharingExtensions;

    public FileReference(int begin, int end, FileSharingExtension fileSharingExtension) {
        super(begin, end);
        this.fileSharingExtensions = Collections.singletonList(fileSharingExtension);
    }

    public FileReference(int begin, int end, List<FileSharingExtension> fileSharingExtensions) {
        super(begin, end);
        this.fileSharingExtensions = fileSharingExtensions;
    }

    public void addFileInfo(FileSharingExtension fileSharingExtension) {
        if (fileSharingExtension != null) {
            fileSharingExtensions.add(fileSharingExtension);
        }
    }

    public List<FileSharingExtension> getFileSharingExtensions() {
        return fileSharingExtensions;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) {
        if (fileSharingExtensions != null && !fileSharingExtensions.isEmpty()) {
            for (FileSharingExtension file : fileSharingExtensions) {
                xml.append(file.toXML());
            }
        }
    }
}
