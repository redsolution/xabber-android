package com.xabber.android.data.extension.references.mutable.filesharing;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class FileSharingExtension implements ExtensionElement {

    public static final String NAMESPACE = "https://xabber.com/protocol/files";
    public static final String FILE_SHARING_ELEMENT = "file-sharing";

    protected FileInfo fileInfo;
    protected FileSources fileSources;

    public FileSharingExtension(FileInfo fileInfo, FileSources fileSources) {
        this.fileInfo = fileInfo;
        this.fileSources = fileSources;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileSources(FileSources fileSources) {
        this.fileSources = fileSources;
    }

    public FileSources getFileSources() {
        return fileSources;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return FILE_SHARING_ELEMENT;
    }

    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.prelude(FILE_SHARING_ELEMENT, NAMESPACE);
        xml.rightAngleBracket();
        if (fileInfo != null) {
            xml.append(fileInfo.toXML());
        }
        if (fileSources != null) {
            xml.append(fileSources.toXML());
        }
        xml.closeElement(FILE_SHARING_ELEMENT);
        return xml;
    }

}
