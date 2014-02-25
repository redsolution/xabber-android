package com.xabber.xmpp.version;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.IQ;
import com.xabber.xmpp.SerializerUtils;

/**
 * Software version packet.
 * 
 * http://xmpp.org/extensions/xep-0092.html
 * 
 * @author Wolfgang Wermund
 *
 */
public class Version extends IQ {

	public static final String ELEMENT_NAME = "query";
	public static final String NAMESPACE = "jabber:iq:version";
	
	public static final String NAME_NAME = "name";
	public static final String VERSION_NAME = "version";
	public static final String OS_NAME = "os";
	
	private String name;
	private String version;
	private String os;
	
	@Override
	public String getElementName() {
		return ELEMENT_NAME;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public void serializeContent(XmlSerializer serializer) throws IOException {
		if(name != null) {
			SerializerUtils.addTextTag(serializer, NAME_NAME, name);
		}
		if(version != null) {
			SerializerUtils.addTextTag(serializer, VERSION_NAME, version);
		}
		if(os != null) {
			SerializerUtils.addTextTag(serializer, OS_NAME, os);
		}
	}

	@Override
	public boolean isValid() {
		return name != null && version != null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getOs() {
		return os;
	}

	public void setOs(String os) {
		this.os = os;
	}

}
