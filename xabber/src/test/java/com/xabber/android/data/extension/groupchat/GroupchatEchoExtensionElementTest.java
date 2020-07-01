package com.xabber.android.data.extension.groupchat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GroupchatEchoExtensionElementTest {
    @Test
    public void testGroupchatEchoExtensionElementParsing(){

    }

    @Test
    public void testGroupchatEchoExtensionElementBuilding(){
        GroupchatEchoExtensionElement ee = new GroupchatEchoExtensionElement();
        String reference = "<x xmlns=\"http://xabber.com/protocol/groupchat#system-message\" type=\"echo\"></x>";
        assertEquals(reference, ee.toXML().toString());
    }
}
