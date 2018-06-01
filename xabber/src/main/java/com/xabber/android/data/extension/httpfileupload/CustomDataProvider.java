package com.xabber.android.data.extension.httpfileupload;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.roster.provider.RosterPacketProvider;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout;
import org.jivesoftware.smackx.xdatalayout.provider.DataLayoutProvider;
import org.jivesoftware.smackx.xdatavalidation.packet.ValidateElement;
import org.jivesoftware.smackx.xdatavalidation.provider.DataValidationProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CustomDataProvider extends ExtensionElementProvider<DataForm> {

    public static final CustomDataProvider INSTANCE = new CustomDataProvider();

    @Override
    public DataForm parse(XmlPullParser parser, int initialDepth) throws
            Exception {
        DataForm.Type dataFormType = DataForm.Type.fromString(parser.getAttributeValue("", "type"));
        DataForm dataForm = new DataForm(dataFormType);
        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    String namespace = parser.getNamespace();
                    switch (name) {
                        case "instructions":
                            dataForm.addInstruction(parser.nextText());
                            break;
                        case "title":
                            dataForm.setTitle(parser.nextText());
                            break;
                        case "field":
                            dataForm.addField(parseField(parser));
                            break;
                        case "item":
                            dataForm.addItem(parseItem(parser));
                            break;
                        case "reported":
                            dataForm.setReportedData(parseReported(parser));
                            break;
                        // See XEP-133 Example 32 for a corner case where the data form contains this extension.
                        case RosterPacket.ELEMENT:
                            if (namespace.equals(RosterPacket.NAMESPACE)) {
                                dataForm.addExtensionElement(RosterPacketProvider.INSTANCE.parse(parser));
                            }
                            break;
                        // See XEP-141 Data Forms Layout
                        case DataLayout.ELEMENT:
                            if (namespace.equals(DataLayout.NAMESPACE)) {
                                dataForm.addExtensionElement(DataLayoutProvider.parse(parser));
                            }
                            break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
            }
        }
        return dataForm;
    }

    private static ExtendedFormField parseField(XmlPullParser parser) throws XmlPullParserException, IOException {
        final int initialDepth = parser.getDepth();
        final String var = parser.getAttributeValue("", "var");

        FormField.Type type;
        try {
            type = FormField.Type.fromString(parser.getAttributeValue("", "type"));
        } catch (IllegalArgumentException e) {
            type = FormField.Type.fixed;
        }

        final ExtendedFormField formField;
        if (type == FormField.Type.fixed) {
            formField = new ExtendedFormField();
        } else {
            formField = new ExtendedFormField(var);
            formField.setType(type);
        }
        formField.setLabel(parser.getAttributeValue("", "label"));

        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    String namespace = parser.getNamespace();
                    switch (name) {
                        case "desc":
                            formField.setDescription(parser.nextText());
                            break;
                        case "value":
                            formField.addValue(parser.nextText());
                            break;
                        case "required":
                            formField.setRequired(true);
                            break;
                        case "option":
                            formField.addOption(parseOption(parser));
                            break;
                        // See XEP-0221 Data Forms Media Elements
                        case ExtendedFormField.Media.ELEMENT:
                            if (namespace.equals(ExtendedFormField.Media.NAMESPACE))
                                formField.setMedia(parseMedia(parser));
                            break;
                        // See XEP-122 Data Forms Validation
                        case ValidateElement.ELEMENT:
                            if (namespace.equals(ValidateElement.NAMESPACE)) {
                                formField.setValidateElement(DataValidationProvider.parse(parser));
                            }
                            break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
            }
        }
        return formField;
    }

    private static DataForm.Item parseItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        final int initialDepth = parser.getDepth();
        List<FormField> fields = new ArrayList<FormField>();
        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    switch (name) {
                        case "field":
                            fields.add(parseField(parser));
                            break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
            }
        }
        return new DataForm.Item(fields);
    }

    private static DataForm.ReportedData parseReported(XmlPullParser parser) throws XmlPullParserException, IOException {
        final int initialDepth = parser.getDepth();
        List<FormField> fields = new ArrayList<FormField>();
        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    switch (name) {
                        case "field":
                            fields.add(parseField(parser));
                            break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
            }
        }
        return new DataForm.ReportedData(fields);
    }

    private static FormField.Option parseOption(XmlPullParser parser) throws XmlPullParserException, IOException {
        final int initialDepth = parser.getDepth();
        FormField.Option option = null;
        String label = parser.getAttributeValue("", "label");
        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    switch (name) {
                        case "value":
                            option = new FormField.Option(label, parser.nextText());
                            break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
            }
        }
        return option;
    }

    protected static ExtendedFormField.Media parseMedia(XmlPullParser parser) throws XmlPullParserException, IOException {
        final String height = parser.getAttributeValue("", "height");
        final String width = parser.getAttributeValue("", "width");
        ExtendedFormField.Uri uri = null;

        // parse only first uri
        parser.next();
        if (parser.getName().equals("uri")) {
            String type = parser.getAttributeValue("", "type");
            String size = parser.getAttributeValue("", "size");
            String duration = parser.getAttributeValue("", "duration");
            String url = parser.nextText();
            uri = new ExtendedFormField.Uri(type, url);
            if (size != null && !size.isEmpty()) uri.setSize(Long.valueOf(size));
            if (duration != null && !duration.isEmpty()) uri.setDuration(Long.valueOf(duration));
        }

        return new ExtendedFormField.Media(height, width, uri);
    }
}
