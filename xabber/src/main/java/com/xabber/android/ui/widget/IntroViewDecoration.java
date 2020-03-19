package com.xabber.android.ui.widget;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.carbons.CarbonManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.mam.NextMamManager;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.extension.rrr.RrrManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

public class IntroViewDecoration extends RecyclerView.ItemDecoration {

    private View introView;
    private AccountJid account;
    private UserJid user;
    private int distanceFromMessage = 60;
    private int offsetFromTop = 10;


    public IntroViewDecoration(View introView, AccountJid account, UserJid user) {
        this.introView = introView;
        this.account = account;
        this.user = user;
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDraw(c, parent, state);

        int dx = parent.getMeasuredWidth() / 14;

        introView.layout(parent.getLeft() + dx, 0, parent.getRight() - dx, introView.getMeasuredHeight());
        View firstItem = parent.getChildAt(0);
        if (parent.getChildAdapterPosition(firstItem) == 0) {
            c.save();
            int height = introView.getMeasuredHeight();

            int centerY = parent.getMeasuredHeight() / 2;
            int introRadius = height / 2;
            int dy;
            if (firstItem.getTop() > centerY + introRadius) {
                dy = centerY - introRadius;
            } else {
                dy = firstItem.getTop() - height - distanceFromMessage + offsetFromTop;
            }

            c.translate(dx, dy);
            introView.draw(c);
            c.restore();
        }
    }

    private void setUpIntroViewFields() {
        SecurityLevel securityLevel = OTRManager.getInstance().getSecurityLevel(account, user);

        if (securityLevel == SecurityLevel.plain) {
            // no encryption right now
            introView.findViewById(R.id.iv_encryption);
            introView.findViewById(R.id.tv_encryption);
        } else {
            // with encryption
            introView.findViewById(R.id.iv_encryption);
            introView.findViewById(R.id.tv_encryption);
        }

        if (NextMamManager.getInstance().isSupported(account)) {
            // archive is working
            introView.findViewById(R.id.iv_archive);
            introView.findViewById(R.id.tv_archive);
        } else {
            // archive doesn't work
            introView.findViewById(R.id.iv_archive);
            introView.findViewById(R.id.tv_archive);
        }

        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem != null) {
            try {
                if (CarbonManager.getInstance().isSupportedByServer(accountItem)) {
                    // carbons are supported
                    introView.findViewById(R.id.iv_carbons);
                    introView.findViewById(R.id.tv_carbons);
                } else {
                    // carbons are not supported
                    introView.findViewById(R.id.iv_carbons);
                    introView.findViewById(R.id.tv_carbons);
                }
            } catch (XMPPException.XMPPErrorException | SmackException.NotConnectedException | InterruptedException | SmackException.NoResponseException e) {
                e.printStackTrace();
            }
        } else {
            // carbons are not supported
            introView.findViewById(R.id.iv_carbons);
            introView.findViewById(R.id.tv_carbons);
        }

        if (HttpFileUploadManager.getInstance().isFileUploadSupported(account)) {
            // file upload supported
            introView.findViewById(R.id.iv_file_sharing);
            introView.findViewById(R.id.tv_file_sharing);
        } else {
            // file upload not supported
            introView.findViewById(R.id.iv_file_sharing);
            introView.findViewById(R.id.tv_file_sharing);
        }

        if (RrrManager.getInstance().isSupported(account)) {
            // retract supported on our server
            introView.findViewById(R.id.iv_message_editing);
            introView.findViewById(R.id.tv_message_editing);
        } else {
            // retract not supported on our server
            introView.findViewById(R.id.iv_message_editing);
            introView.findViewById(R.id.tv_message_editing);
        }
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (parent.getChildAdapterPosition(view) == 0) {
            //setUpIntroViewFields();
            introView.measure(View.MeasureSpec.makeMeasureSpec(parent.getMeasuredWidth() * 8 / 10, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(parent.getMeasuredHeight(), View.MeasureSpec.AT_MOST));
            outRect.set(0, introView.getMeasuredHeight() + distanceFromMessage, 0, 0);
        } else {
            outRect.setEmpty();
        }
    }
}
