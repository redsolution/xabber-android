package com.xabber.android.data.extension.references;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RefMediaTest {
    String xml;
    RefFile.Builder builder;
    RefMedia media;

    @Before
    public void setUp() throws Exception {
        builder = RefFile.newBuilder();
        builder.setMediaType("image/jpeg");
        builder.setName("summit.jpg");
        builder.setHeight(150);
        builder.setWidth(160);
        builder.setSize(3032449);
        builder.setDesc("Photo from the summit.");

        media = new RefMedia(builder.build(), "https://upload02.xabber.org/5f7IyZ/guide.txt");

        xml = "<media><file><media-type>image/jpeg</media-type><name>summit.jpg</name><height>150</height>" +
                "<width>160</width><size>3032449</size><desc>Photo from the summit.</desc></file>" +
                "<uri>https://upload02.xabber.org/5f7IyZ/guide.txt</uri></media>";
    }

    @Test
    public void toXML() {
        assertEquals(xml, media.toXML().toString());
    }
}