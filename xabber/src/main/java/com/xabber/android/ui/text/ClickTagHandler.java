package com.xabber.android.ui.text;

import android.content.Context;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import com.xabber.android.R;

import org.xml.sax.XMLReader;

import java.lang.reflect.Field;

public class ClickTagHandler implements Html.TagHandler {

    private final static String FIELD_NEW_ELEMENT = "theNewElement";
    private final static String FIELD_ATTS = "theAtts";
    private final static String FIELD_DATA = "data";
    private final static String FIELD_LENGTH = "length";
    private final static String ATTRIBUTE_URI = "uri";
    private final static String ATTRIBUTE_TYPE = "type";
    private final static String TAG = "click";

    private Context context;
    private int backgroundColor;

    public ClickTagHandler(Context context, int backgroundColor) {
        this.context = context;
        this.backgroundColor = backgroundColor;
    }

    @Override
    public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
        if (tag.equalsIgnoreCase(TAG)) {
            processTag(opening, output, xmlReader);
        }
    }

    private void processTag(boolean opening, Editable output, XMLReader xmlReader) {
        int len = output.length();

        if (opening) {
            String uri = getAttrubute(xmlReader, ATTRIBUTE_URI);
            String type = getAttrubute(xmlReader, ATTRIBUTE_TYPE);
            output.setSpan(new ClickSpan(uri, type, context), len, len, Spannable.SPAN_MARK_MARK);
        } else {
            Object obj = getLast(output, ClickSpan.class);
            int where = output.getSpanStart(obj);
            String uri = null, type = null;

            if (obj instanceof ClickSpan) {
                uri = ((ClickSpan)obj).getUrl();
                type = ((ClickSpan)obj).getType();
            }

            output.removeSpan(obj);

            if (where != len && uri != null && type != null) {
                output.setSpan(new ClickSpan(uri, type, context), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                if (ClickSpan.TYPE_MENTION.equals(type)) {
                    output.setSpan(new BackgroundColorSpan(backgroundColor), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    output.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.black_text)),
                            where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    private Object getLast(Editable text, Class kind) {
        Object[] objs = text.getSpans(0, text.length(), kind);

        if (objs.length == 0) {
            return null;
        } else {
            for(int i = objs.length;i>0;i--) {
                if(text.getSpanFlags(objs[i-1]) == Spannable.SPAN_MARK_MARK) {
                    return objs[i-1];
                }
            }
            return null;
        }
    }

    private String getAttrubute(XMLReader xmlReader, String attrName) {
        String attribute = null;
        try {
            Field elementField = xmlReader.getClass().getDeclaredField(FIELD_NEW_ELEMENT);
            elementField.setAccessible(true);
            Object element = elementField.get(xmlReader);

            Field attsField = element.getClass().getDeclaredField(FIELD_ATTS);
            attsField.setAccessible(true);
            Object atts = attsField.get(element);

            Field dataField = atts.getClass().getDeclaredField(FIELD_DATA);
            dataField.setAccessible(true);
            String[] data = (String[]) dataField.get(atts);

            Field lengthField = atts.getClass().getDeclaredField(FIELD_LENGTH);
            lengthField.setAccessible(true);
            int length = (Integer) lengthField.get(atts);

            for (int i = 0; i < length; i++) {
                if (attrName.equals(data[i * 5 + 1])) {
                    attribute = data[i * 5 + 4];
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException | NullPointerException e) {
            Log.d(ClickTagHandler.class.getSimpleName(),
                    "Error on getting attribute '" + attrName + "': " + e.toString());
        }
        return attribute;
    }
}
