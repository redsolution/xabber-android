/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui.activity;


import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.blocking.OnBlockedListChangedListener;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.NotificationState;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.AccountPainter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.dialog.BlockContactDialog;
import com.xabber.android.ui.dialog.SnoozeDialog;
import com.xabber.android.ui.fragment.ContactVcardViewerFragment;
import com.xabber.android.ui.helper.BlurTransformation;
import com.xabber.android.ui.helper.ContactTitleInflater;
import com.xabber.android.ui.widget.ContactBarAutoSizingLayout;

import java.util.Collection;
import java.util.Collections;

public class ContactActivity extends ManagedActivity implements
        OnContactChangedListener, OnAccountChangedListener, ContactVcardViewerFragment.Listener, View.OnClickListener,
        View.OnLongClickListener, SnoozeDialog.OnSnoozeListener, BlockingManager.UnblockContactListener, OnBlockedListChangedListener {

    private static final String LOG_TAG = ContactActivity.class.getSimpleName();
    private AccountJid account;
    private UserJid user;
    private AbstractChat chat;
    private Toolbar toolbar;
    private View contactTitleView;
    private TextView contactAddress;
    private TextView contactName;
    private AbstractContact bestContact;
    private CollapsingToolbarLayout collapsingToolbar;
    private AppBarLayout appBarLayout;
    private ImageView background;
    private ImageView QRgen;
    private ImageButton chatButton;
    private ImageButton callsButton;
    private ImageButton blockButton;
    private ImageButton notifyButton;
    private int accountMainColor;
    private boolean coloredBlockText;
    private TextView chatButtonText;
    private TextView callsButtonText;
    private TextView blockButtonText;
    private TextView notifyButtonText;
    private ContactBarAutoSizingLayout contactBarLayout;

    public int orientation;
    private boolean blocked;

    public static Intent createIntent(Context context, AccountJid account, UserJid user) {
        return new EntityIntentBuilder(context, ContactActivity.class)
                .setAccount(account).setUser(user).build();
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    private static UserJid getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    protected Toolbar getToolbar() {
        return toolbar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        account = getAccount(getIntent());
        user = getUser(getIntent());

        AccountItem accountItem = AccountManager.getInstance().getAccount(this.account);
        if (accountItem == null) {
            LogManager.e(LOG_TAG, "Account item is null " + account);
            finish();
            return;
        }

        if (user != null && user.getBareJid().equals(account.getFullJid().asBareJid())) {
            try {
                user = UserJid.from(accountItem.getRealJid().asBareJid());
            } catch (UserJid.UserJidCreateException e) {
                LogManager.exception(this, e);
            }
        }

        if (account == null || user == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Window win = getWindow();
            WindowManager.LayoutParams winParams = win.getAttributes();
            winParams.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            win.setAttributes(winParams);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window win = getWindow();
            WindowManager.LayoutParams winParams = win.getAttributes();
            winParams.flags &= ~WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            win.setAttributes(winParams);
            win.setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_contact_new);

        if (savedInstanceState == null) {
            Fragment fragment = ContactVcardViewerFragment.newInstance(account, user);
            getSupportFragmentManager().beginTransaction().add(R.id.scrollable_container, fragment).commit();
        }

        bestContact = RosterManager.getInstance().getBestContact(account, user);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        contactBarLayout = findViewById(R.id.contact_bar_layout);

        chatButton = findViewById(R.id.chat_button);
        chatButtonText = findViewById(R.id.chat_button_text);
        chatButton.setOnClickListener(this);

        callsButton = findViewById(R.id.call_button);
        callsButtonText = findViewById(R.id.call_button_text);
        callsButton.setOnClickListener(this);

        blockButton = findViewById(R.id.block_button);
        blockButtonText = findViewById(R.id.block_text);
        blockButton.setOnClickListener(this);

        notifyButton = findViewById(R.id.notify_button);
        notifyButtonText = findViewById(R.id.notification_text);
        notifyButton.setOnClickListener(this);

        int colorLevel = AccountPainter.getAccountColorLevel(account);
        accountMainColor = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);
        final int accountDarkColor = ColorManager.getInstance().getAccountPainter().getAccountDarkColor(account);
        if (colorLevel == 0 || colorLevel == 1 || colorLevel == 3) {
            coloredBlockText = true;
        } else
            coloredBlockText = false;

        contactTitleView = findViewById(R.id.contact_title_expanded_new);
        contactAddress = (TextView) findViewById(R.id.address_text);
        contactAddress.setText(user.getBareJid().toString());
        contactName = (TextView) findViewById(R.id.name);

        chat = MessageManager.getInstance().getOrCreateChat(account, user);
        checkForBlockedStatus();

        orientation = getResources().getConfiguration().orientation;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (this.isInMultiWindowMode()) {
                orientation = Configuration.ORIENTATION_PORTRAIT;
            }
        }
        setContactBar(accountMainColor, orientation);
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            orientationPortrait();
        } else {
            orientationLandscape();
        }

        background = findViewById(R.id.backgroundView);
        Drawable backgroundSource = bestContact.getAvatar(false);
        if (backgroundSource == null)
            backgroundSource = getResources().getDrawable(R.drawable.about_backdrop);
        Glide.with(this)
                .load(backgroundSource)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(new MultiTransformation<Bitmap>(new CenterCrop(), new BlurTransformation(25, 8, /*this,*/ accountMainColor)))
                .into(background);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnBlockedListChangedListener.class, this);
        ContactTitleInflater.updateTitle(contactTitleView, this, bestContact, true);
        updateName();
        appBarResize();
    }

    @Override
    public void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnBlockedListChangedListener.class, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void appBarResize() {
        ImageView avatar = findViewById(R.id.ivAvatar);
        ImageView avatarQR = findViewById(R.id.ivAvatarQR);
        if (avatar.getDrawable() == null) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                avatar.setVisibility(View.GONE);
                avatarQR.setVisibility(View.GONE);
            } else {
                QRgen.setVisibility(View.GONE);
                avatarQR.setVisibility(View.VISIBLE);
            }
        }
    }

    private void orientationPortrait() {
        collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = true;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbar.setTitle(bestContact.getName());
                    contactTitleView.setVisibility(View.INVISIBLE);
                    isShow = true;
                } else if (isShow) {
                    collapsingToolbar.setTitle(" ");
                    contactTitleView.setVisibility(View.VISIBLE);
                    isShow = false;
                }
            }
        });
        collapsingToolbar.setContentScrimColor(accountMainColor);
    }

    private void orientationLandscape() {
        final LinearLayout nameHolderView = (LinearLayout) findViewById(R.id.name_holder);

        toolbar.setTitle("");
        toolbar.setBackgroundColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window win = getWindow();
            win.setStatusBarColor(accountMainColor);
        }

        if (toolbar.getOverflowIcon() != null)
            toolbar.getOverflowIcon().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);

        QRgen = findViewById(R.id.generate_qrcode);
        QRgen.setOnClickListener(this);

        final NestedScrollView scrollView = findViewById(R.id.scroll_v_card);
        final LinearLayout ll = findViewById(R.id.scroll_v_card_child);
        //final int actionBarSize = getActionBarSize();

        nameHolderView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    nameHolderView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else nameHolderView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                int topPadding = nameHolderView.getHeight();
                ll.setPadding(0, topPadding, 0, 0);
            }
        });

        //final View divider = findViewById(R.id.divider);
        /*if (scrollView != null) {
            scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
                @Override
                public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    int i = v.getChildAt(0).getPaddingTop();
                    if (scrollY >= (v.getChildAt(0).getPaddingTop())) {
                        divider.setVisibility(View.VISIBLE);
                        contactName.setMaxLines(1);
                    } else if (divider.getVisibility() != View.INVISIBLE) {
                        divider.setVisibility(View.INVISIBLE);
                        contactName.setMaxLines(2);
                    }
                }
            });
        }*/
    }

    private void setContactBar(int color, int orientation) {
        boolean notify = true;
        if (chat != null) {
            chat.enableNotificationsIfNeed();
            if (chat.notifyAboutMessage() && !blocked)
                notifyButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_bell));
            else {
                notify = false;
                NotificationState notificationState = chat.getNotificationState();
                switch (notificationState.getMode()) {
                    case disabled:
                        notifyButton.setImageDrawable((getResources().getDrawable(R.drawable.ic_snooze_forever)));
                        break;
                    case snooze1d:
                    case snooze2h:
                    case snooze1h:
                    case snooze15m:
                    default:
                        if (blocked) notifyButton.setImageDrawable((getResources().getDrawable(R.drawable.ic_snooze_forever)));
                        else notifyButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_snooze));
                        break;
                }
            }
        }
        chatButton.setColorFilter(blocked ? getResources().getColor(R.color.grey_500) : color);
        callsButton.setColorFilter(blocked ? getResources().getColor(R.color.grey_500) : color);
        notifyButton.setColorFilter(blocked || !notify ? getResources().getColor(R.color.grey_500) : color);
        blockButton.setColorFilter(getResources().getColor(R.color.red_900));

        callsButton.setEnabled(!blocked);
        notifyButton.setEnabled(!blocked);

        blockButtonText.setText(blocked ? R.string.contact_bar_unblock : R.string.contact_bar_block);
        blockButtonText.setTextColor(getResources().getColor(blocked || coloredBlockText ?
                R.color.red_900 :
                SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light ?
                R.color.grey_600 :
                R.color.grey_400));

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            chatButtonText.setVisibility(View.GONE);
            callsButtonText.setVisibility(View.GONE);
            blockButtonText.setVisibility(View.GONE);
            notifyButtonText.setVisibility(View.GONE);
        } else {
            chatButtonText.setVisibility(View.VISIBLE);
            callsButtonText.setVisibility(View.VISIBLE);
            blockButtonText.setVisibility(View.VISIBLE);
            notifyButtonText.setVisibility(View.VISIBLE);
            contactBarLayout.redrawText();
        }
    }

    public void manageAvailableUsernameSpace() {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            TextView view = findViewById(R.id.action_add_contact);
            if (view != null) {
                int width;
                Rect bounds = new Rect();
                Paint textPaint = view.getPaint();
                textPaint.getTextBounds(view.getText().toString(), 0, view.getText().length(), bounds);
                width = bounds.width();
                LinearLayout nameL = findViewById(R.id.name_layout);
                ((LinearLayout.LayoutParams) nameL.getLayoutParams())
                        .setMargins(0, 0, width + 100, 0);
            }
        }
    }

    public int getActionBarSize() {
        TypedArray styledAttributes = getTheme().obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
        int actionBarSize = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        return actionBarSize;
    }

    public void generateQR() {
        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(getAccount(), getUser());
        Intent intent = QRCodeActivity.createIntent(this, getAccount());
        String textName = rosterContact != null ? rosterContact.getName() : "";
        intent.putExtra("account_name", textName);
        String textAddress = getUser().toString();
        intent.putExtra("account_address", textAddress);
        intent.putExtra("caller", "ContactViewerActivity");
        startActivity(intent);
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        for (BaseEntity entity : entities) {
            if (entity.equals(account, user)) {
                updateName();
                break;
            }
        }
    }

    protected View getTitleView() {
        return contactTitleView;
    }

    private void updateName() {
        contactName.setText(bestContact.getName());
        contactName.setVisibility(bestContact.getName().equals(user.getBareJid().toString()) ? View.GONE : View.VISIBLE);
        /*if (MUCManager.getInstance().isMucPrivateChat(account, user)) {
            String vCardName = VCardManager.getInstance().getName(user.getJid());
            if (!"".equals(vCardName)) {
                collapsingToolbar.setTitle(vCardName);
            } else {
                collapsingToolbar.setTitle(user.getJid().getResourceOrNull().toString());
            }

        } else {
            collapsingToolbar.setTitle(RosterManager.getInstance().getBestContact(account, user).getName());
        }*/
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        if (accounts.contains(account)) {
            updateName();
        }
    }

    protected AccountJid getAccount() {
        return account;
    }

    protected UserJid getUser() {
        return user;
    }

    @Override
    public void onVCardReceived() {
        ContactTitleInflater.updateTitle(contactTitleView, this, bestContact, true);
    }

    @Override
    public void registerVCardFragment(ContactVcardViewerFragment fragment) {
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.chat_button:
                startActivity(ChatActivity.createSpecificChatIntent(this, account, user));
                finish();
                break;
            case R.id.call_button:
                Snackbar.make(view, "Feature is coming in future updates!", Snackbar.LENGTH_LONG).show();
                break;
            case R.id.notify_button:
                if (chat.notifyAboutMessage())
                    showSnoozeDialog(chat);
                else
                    removeSnooze(chat);
                break;
            case R.id.block_button:
                if (blocked)
                    removeBlock();
                else
                    showBlockDialog();
                break;
            case R.id.generate_qrcode:
                generateQR();
                break;
            default:
        }
    }

    public void showSnoozeDialog(AbstractChat chat) {
        SnoozeDialog dialog = SnoozeDialog.newInstance(chat, this);
        dialog.show(getSupportFragmentManager(), SnoozeDialog.class.getName());
    }

    public void removeSnooze(AbstractChat chat) {
        if (chat != null)
            chat.setNotificationStateOrDefault(
                    new NotificationState(NotificationState.NotificationMode.enabled,
                            0), true);
        onSnoozed();
    }

    public void showBlockDialog() {
        BlockContactDialog dialog = BlockContactDialog.newInstance(getAccount(), getUser());
        dialog.show(getSupportFragmentManager(), BlockContactDialog.class.getName());
    }

    private void removeBlock() {
        BlockingManager.getInstance().unblockContacts(getAccount(), Collections.singletonList(getUser()), this);
    }

    private void checkForBlockedStatus() {
        blocked = BlockingManager.getInstance().contactIsBlocked(account, user);
    }

    @Override
    public void onSnoozed() {
        setContactBar(accountMainColor, orientation);
    }

    //Block listeners
    @Override
    public void onBlockedListChanged(AccountJid account) {
        if (account.getFullJid().asBareJid().equals(getAccount().getFullJid().asBareJid())) {
            checkForBlockedStatus();
            setContactBar(accountMainColor, orientation);
        }
    }
    @Override
    public void onSuccessUnblock() {
        blocked = false;
        setContactBar(accountMainColor, orientation);
    }
    @Override
    public void onErrorUnblock() { }

    @Override
    public boolean onLongClick(View view) {
        /*int[] location = new int[2];
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_message_square, (ViewGroup) findViewById(R.id.message));
        TextView description = (TextView) layout.findViewById(R.id.description);
        String desc = "";
        if (view.getContentDescription()!=null)
             desc = view.getContentDescription().toString();
        description.setText(desc);
        switch (view.getId()) {
            case R.id.chat_button:
            case R.id.call_button:
            case R.id.video_button:
            case R.id.notify_button:
                view.getLocationOnScreen(location);
                final Toast toast = new Toast(getApplicationContext());
                int offset = calculateOffset(view, layout, description);
                toast.setGravity(Gravity.START|Gravity.TOP , location[0] + offset, location[1]);
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        toast.cancel();
                    }
                }, 500);
                break;
        }*/
        return true;
    }


    /*private int calculateOffset(View buttonView, View toastView, TextView desc) {
     *//*int I = desc.getWidth();
        int II = desc.getMeasuredWidth();
        Rect rect = new Rect();
        desc.getWindowVisibleDisplayFrame(rect);
        int III = rect.right - rect.left;*//*
        return (buttonView.getWidth()  / 4);
    }*/
}
