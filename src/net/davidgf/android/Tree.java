
/*
 * Java WhatsApp API implementation.
 * Written by David Guillen Fandos (david@davidgf.net) based 
 * on the sources of WhatsAPI PHP implementation and whatsapp
 * for libpurple.
 *
 * Share and enjoy!
 *
 */

package net.davidgf.android;

import java.util.*;

public class Tree {
	private Map < String, String > attributes;
	private Vector < Tree > children;
	private String tag;
	private byte [] data;
	private boolean forcedata;

	public Tree(String tag) {
		this.tag = tag;
		this.forcedata=false;
		this.data = new byte[0];
		children = new Vector < Tree > ();
		attributes = new HashMap < String, String > ();
	}
	public Tree(String tag, Map < String,String > attributes) {
		this.tag = tag;
		this.attributes = attributes;
		this.forcedata = false;
		this.data = new byte[0];
		children = new Vector < Tree > ();
	}
	public void forceDataWrite() {
		forcedata=true;
	}
	public boolean forcedData() {
		return forcedata;
	}
	public void addChild(Tree t) {
		children.add(t);
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public void setAttributes(Map < String, String > attributes) {
		this.attributes = attributes;
	}
	public void readAttributes(DataBuffer data, int size) {
		int count = (size - 2 + (size % 2)) / 2;
		while (count-- > 0) {
			String key = data.readString();
			String value = data.readString();
			attributes.put(key,value);
		}
	}
	public void writeAttributes(DataBuffer data) {
		for (String key: attributes.keySet()) {
			data.putString(key);
			data.putString(attributes.get(key));
		}
	}
	public void setData(byte [] d) {
		data = new byte[d.length];
		for (int c = 0; c < d.length; c++)
			data[c] = d[c];
	}
	public byte [] getData() {
		byte [] b = new byte[data.length];
		for (int c = 0; c < data.length; c++)
			b[c] = data[c];
		return b;
	}
	public String getTag() {
		return tag;
	}
	public void setChildren(Vector < Tree > c) {
		children = c;
	}
	public Vector < Tree > getChildren() {
		return children;
	}
	public Map < String, String > getAttributes() {
		return attributes;
	}
	public boolean hasAttributeValue(String at, String val) {
		if (hasAttribute(at)) {
			return (attributes.get(at).equals(val));
		}
		return false;
	}
	public boolean hasAttribute(String at) {
		return (attributes.containsKey(at));
	}
	public String getAttribute(String at) {
		if (attributes.containsKey(at))
			return (attributes.get(at));
		return "";
	}
	
	public Tree getChild(String tag) {
		for (int i = 0; i < children.size(); i++) {
			if (children.get(i).getTag().equals(tag))
				return children.get(i);
			Tree t = children.get(i).getChild(tag);
			if (t != null)
				return t;
		}
		return null;
	}
	public boolean hasChild(String tag) {
		for (int i = 0; i < children.size(); i++) {
			if (children.get(i).getTag().equals(tag))
				return true;
			if (children.get(i).hasChild(tag))
				return true;
		}
		return false;
	}
	
	String toString(int sp) {
		String ret = "";
		String spacing = "";
		for (int i = 0; i < sp*3; i++)
			spacing += " ";
		ret += spacing+"Tag: "+tag+"\n";
		for (String key: attributes.keySet()) {
			ret += spacing+"at["+key+"]="+attributes.get(key)+"\n";
		}
		ret += spacing+"Data: "+MiscUtil.bytesToUTF8(data)+"\n";
		
		for (int i = 0; i < children.size(); i++) {
			ret += children.get(i).toString(sp+1);
		}
		return ret;
	}
};




