package com.xabber.android.ui.text;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

public class ClickSpan extends ClickableSpan {

    public final static String TYPE_HYPERLINK = "hyperlink";
    public final static String TYPE_MENTION = "mention";

    private final String url;
    private final String type;
    private final Context context;

    public ClickSpan(String url, String type, Context context) {
        this.url = url;
        this.type = type;
        this.context = context;
    }

    @Override
    public void onClick(View view) {
        if (url != null && context != null) {
            if (TYPE_HYPERLINK.equals(type)) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(browserIntent);
            }
        }
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        if (TYPE_HYPERLINK.equals(type)) ds.setUnderlineText(true);
        ds.setColor(ds.linkColor);
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }
}
