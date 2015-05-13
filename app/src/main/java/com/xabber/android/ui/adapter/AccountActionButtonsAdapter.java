package com.xabber.android.ui.adapter;

import android.app.Activity;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.melnykov.fab.FloatingActionButton;
import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.extension.avatar.AvatarManager;

import java.util.ArrayList;
import java.util.Collections;

import de.hdodenhof.circleimageview.CircleImageView;


public class AccountActionButtonsAdapter implements UpdatableAdapter {

    private final Activity activity;

    /**
     * Listener for click on elements.
     */
    private final View.OnClickListener onClickListener;

    /**
     * Layout to be populated.
     */
    private final LinearLayout linearLayout;

    /**
     * List of accounts.
     */
    private final ArrayList<String> accounts;
    private int[] accountActionBarColors;
    private int[] accountStatusBarColors;
    private int[] accountBackgroundColors;

    public AccountActionButtonsAdapter(Activity activity,
                                       View.OnClickListener onClickListener, LinearLayout linearLayout) {
        super();
        this.activity = activity;
        this.onClickListener = onClickListener;
        this.linearLayout = linearLayout;
        accounts = new ArrayList<>();

        Resources resources = activity.getResources();

        accountActionBarColors = resources.getIntArray(R.array.account_action_bar);
        accountStatusBarColors = resources.getIntArray(R.array.account_status_bar);
        accountBackgroundColors = resources.getIntArray(R.array.account_background);
    }

    /**
     * Rebuild list of accounts.
     * <p/>
     * Call it on account creation, deletion, enable or disable.
     */
    public void rebuild() {
        accounts.clear();
        accounts.addAll(AccountManager.getInstance().getAccounts());

        Collections.sort(accounts);
        final int size = accounts.size();
        final LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        while (linearLayout.getChildCount() < size) {
            View view = inflater.inflate(R.layout.account_action_button, linearLayout, false);
            view.setOnClickListener(onClickListener);
            linearLayout.addView(view);
        }

        while (linearLayout.getChildCount() > size) {
            linearLayout.removeViewAt(size);
        }
        onChange();
    }

    @Override
    public void onChange() {
        for (int index = 0; index < accounts.size(); index++) {
            View view = linearLayout.getChildAt(index);

            final CircleImageView circleImageView = (CircleImageView) view.findViewById(R.id.account_avatar);
            final String account = accounts.get(index);
            circleImageView.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(account));

            FloatingActionButton backgroundActionButton = (FloatingActionButton) view.findViewById(R.id.fab);
            int colorLevel = AccountManager.getInstance().getColorLevel(account);
            backgroundActionButton.setColorNormal(accountActionBarColors[colorLevel]);
            backgroundActionButton.setColorPressed(accountStatusBarColors[colorLevel]);
            backgroundActionButton.setColorRipple(accountBackgroundColors[colorLevel]);

            String selectedAccount = AccountManager.getInstance().getSelectedAccount();

            int shadowVisibility;

            if (selectedAccount == null) {
                shadowVisibility = View.GONE;
            } else {
                shadowVisibility = View.VISIBLE;
                if (selectedAccount.equalsIgnoreCase(account)) {
                    shadowVisibility = View.GONE;
                }
            }

            view.findViewById(R.id.account_unselected_shadow).setVisibility(shadowVisibility);

            StatusMode statusMode = AccountManager.getInstance().getAccount(account).getDisplayStatusMode();
            int offlineShadowVisibility;
            if (statusMode == StatusMode.connection || statusMode == StatusMode.unavailable) {
                offlineShadowVisibility = View.VISIBLE;
            } else {
                offlineShadowVisibility = View.GONE;
            }
            view.findViewById(R.id.account_offline_shadow).setVisibility(offlineShadowVisibility);

        }
    }

    public int getCount() {
        return accounts.size();
    }

    public Object getItem(int position) {
        return accounts.get(position);
    }

    public String getItemForView(View view) {
        for (int index = 0; index < linearLayout.getChildCount(); index++) {
            if (view == linearLayout.getChildAt(index)) {
                return accounts.get(index);
            }
        }
        return null;
    }
}
