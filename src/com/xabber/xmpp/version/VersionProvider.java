package com.xabber.xmpp.version;

import org.xmlpull.v1.XmlPullParser;

import com.xabber.xmpp.AbstractIQProvider;
import com.xabber.xmpp.ProviderUtils;

/**
 * Provider for parsing XEP-0092 packets.
 * 
 * http://xmpp.org/extensions/xep-0092.html
 * 
 * @author Wolfgang Wermund
 *
 */
public class VersionProvider extends AbstractIQProvider<Version> {

	@Override
	protected boolean parseInner(XmlPullParser parser, Version instance)
			throws Exception {
		if (super.parseInner(parser, instance))
			return true;
		String name = parser.getName();
		if (Version.NAME_NAME.equals(name)) {
			String software_name = ProviderUtils.parseText(parser);
			instance.setName(software_name);
		} else if (Version.VERSION_NAME.equals(name)) {
			String version_name = ProviderUtils.parseText(parser);
			instance.setVersion(version_name);
		} else if (Version.OS_NAME.equals(name)) {
			String os_name = ProviderUtils.parseText(parser);
			instance.setOs(os_name);
		} else return false;
		return true;
	}

	@Override
	protected Version createInstance(XmlPullParser parser) {
		return new Version();
	}

}
