package com.xabber.android.ui.text;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.style.ClickableSpan;
import android.view.View;

public class ClickSpan extends ClickableSpan {

    private final String url;
    private final Context context;

    public ClickSpan(String url, Context context) {
        this.url = url;
        this.context = context;
    }

    @Override
    public void onClick(View view) {
        if (url != null && context != null) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(browserIntent);
        }
    }

    public String getUrl() {
        return url;
    }
}
