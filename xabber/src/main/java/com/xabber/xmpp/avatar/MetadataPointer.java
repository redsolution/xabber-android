package com.xabber.xmpp.avatar;

import org.jivesoftware.smack.util.StringUtils;

import java.util.Map;

public class MetadataPointer {

    private final String namespace;
    private final Map<String, Object> fields;

    /**
     * Metadata Pointer constructor.
     *
     * @param namespace namespace of the child element of the metadata pointer.
     * @param fields fields of the child element as key, value pairs.
     */
    public MetadataPointer(String namespace, Map<String, Object> fields) {
        this.namespace = StringUtils.requireNotNullOrEmpty(namespace, "Namespace MUST NOT be null, nor empty.");
        this.fields = fields;
    }

    /**
     * Get the namespace of the pointers child element.
     *
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Get the fields of the pointers child element.
     *
     * @return the fields
     */
    public Map<String, Object> getFields() {
        return fields;
    }

}