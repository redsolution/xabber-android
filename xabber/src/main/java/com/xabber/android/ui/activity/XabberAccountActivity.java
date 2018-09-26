package com.xabber.android.ui.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.connection.NetworkManager;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.data.xaccount.XMPPAuthManager;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.fragment.XAccountXMPPLoginFragment;
import com.xabber.android.ui.fragment.XabberAccountInfoFragment;
import com.xabber.android.utils.RetrofitErrorConverter;

import okhttp3.ResponseBody;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class XabberAccountActivity extends BaseLoginActivity
        implements Toolbar.OnMenuItemClickListener, XAccountXMPPLoginFragment.Listener {

    private final static String LOG_TAG = XabberAccountActivity.class.getSimpleName();
    private final static String FRAGMENT_INFO = "fragment_info";
    private final static String FRAGMENT_LOGIN = "fragment_login";

    private FragmentTransaction fTrans;
    private Fragment fragmentInfo;
    private Fragment fragmentLogin;

    private Toolbar toolbar;
    private BarPainter barPainter;
    private ProgressDialog progressDialog;

    private Dialog logoutDialog;
    private boolean dialogShowed;

    @NonNull
    public static Intent createIntent(Context context) {
        return new Intent(context, XabberAccountActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.Theme_LightToolbar);
        setContentView(R.layout.activity_xabber_account_info);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.inflateMenu(R.menu.toolbar_xabber_account_info);
        toolbar.setTitleTextColor(getResources().getColor(R.color.black_text));
        barPainter = new BarPainter(this, toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        onPrepareOptionsMenu(toolbar.getMenu());
        subscribeForXabberAccount();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (logoutDialog != null)
            logoutDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_quit:
                if (!dialogShowed) {
                    dialogShowed = true;
                    showLogoutDialog();
                }
                return true;
            case R.id.action_sync:
                onSyncClick(false);
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void showProgress(String title) {
        if (!isFinishing()) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle(title);
            progressDialog.setMessage(getResources().getString(R.string.progress_message));
            progressDialog.show();
        }
    }

    @Override
    protected void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void subscribeForXabberAccount() {
        compositeSubscription.add(XabberAccountManager.getInstance().subscribeForAccount()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext(new Action1<XabberAccount>() {
                @Override
                public void call(XabberAccount account) {
                    if (account != null) showInfoFragment();
                    else showLoginFragment();
                }
            }).subscribe());
    }

    /** Social Auth */

    @Override
    protected void onSocialAuthSuccess(String provider, String credentials) {
        bindSocial(provider, credentials);
    }

    /** XMPP-login */

    @Override
    public void onAccountClick(String jid) {
        loginXabberAccountViaXMPP(jid);
    }

    public void loginXabberAccountViaXMPP(String accountJid) {
        if (NetworkManager.isNetworkAvailable()) {
            requestXMPPCode(accountJid);
        } else
            Toast.makeText(this, R.string.toast_no_internet, Toast.LENGTH_LONG).show();
    }

    private void requestXMPPCode(final String jid) {

        // ! show progress !

        Subscription requestXMPPCodeSubscription = AuthManager.requestXMPPCode(jid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<AuthManager.XMPPCode>() {
                    @Override
                    public void call(AuthManager.XMPPCode code) {
                        handleSuccessRequestXMPPCode(code, jid);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorRequestXMPPCode(throwable);
                    }
                });
        compositeSubscription.add(requestXMPPCodeSubscription);
    }

    private void handleSuccessRequestXMPPCode(AuthManager.XMPPCode code, String jid) {

        // ! hide progress !

        XMPPAuthManager.getInstance().addRequest(code.getRequestId(), code.getApiJid(), jid);
    }

    private void handleErrorRequestXMPPCode(Throwable throwable) {
        // ! hide progress !
        // TODO: 07.09.18 сделать корректную обработку ошибок
        Toast.makeText(this, "Error while xmpp-auth: " + throwable.toString(), Toast.LENGTH_LONG).show();
    }

    /** Xabber Account Info */

    public void onLogoutClick(boolean deleteAccounts) {
        if (NetworkManager.isNetworkAvailable()) {
            logout(deleteAccounts);
        } else
            Toast.makeText(this, R.string.toast_no_internet, Toast.LENGTH_LONG).show();
    }

    public void onSyncClick(boolean needGoToMainActivity) {
        if (NetworkManager.isNetworkAvailable()) {
            synchronize(needGoToMainActivity);
        } else
            Toast.makeText(this, R.string.toast_no_internet, Toast.LENGTH_LONG).show();
    }

    private void showInfoFragment() {
        if (fragmentInfo == null)
            fragmentInfo = XabberAccountInfoFragment.newInstance();

        fTrans = getFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragmentInfo, FRAGMENT_INFO);
        fTrans.commit();

        toolbar.setTitle(R.string.title_xabber_account);
        barPainter.setLiteGrey();
        setupMenu(true);
    }

    private void showLoginFragment() {
        if (fragmentLogin == null)
            fragmentLogin = XAccountXMPPLoginFragment.newInstance();

        fTrans = getFragmentManager().beginTransaction();
        fTrans.replace(R.id.container, fragmentLogin, FRAGMENT_LOGIN);
        fTrans.commit();

        toolbar.setTitle(R.string.title_login_xabber_account);
        barPainter.setLiteGrey();
        setupMenu(false);
    }

    private void setupMenu(boolean show) {
        Menu menu = toolbar.getMenu();
        menu.findItem(R.id.action_quit).setVisible(show);
        menu.findItem(R.id.action_sync).setVisible(show);
    }

    @Override
    protected void updateAccountInfo(XabberAccount account) {
        if (account != null) {
            XabberAccountInfoFragment fragment = (XabberAccountInfoFragment) getFragmentManager().findFragmentByTag(FRAGMENT_INFO);
            if (fragment != null && fragment.isVisible())
                ((XabberAccountInfoFragment) fragmentInfo).updateData(account);
        }
    }

    @Override
    protected void updateLastSyncTime() {
        XabberAccountInfoFragment fragment = (XabberAccountInfoFragment) getFragmentManager().findFragmentByTag(FRAGMENT_INFO);
        if (fragment != null && fragment.isVisible())
            ((XabberAccountInfoFragment) fragmentInfo).updateLastSyncTime();
    }

    /** LOGOUT */

    private void showLogoutDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_logout_xabber_account, null);
        final CheckBox chbDeleteAccounts = (CheckBox) view.findViewById(R.id.chbDeleteAccounts);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.progress_title_quit)
                .setMessage(R.string.logout_summary)
                .setView(view)
                .setPositiveButton(R.string.button_quit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onLogoutClick(chbDeleteAccounts.isChecked());
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        logoutDialog = builder.create();
        logoutDialog.show();
        dialogShowed = false;
    }

    private void logout(final boolean deleteAccounts) {
        showProgress(getResources().getString(R.string.progress_title_quit));
        Subscription logoutSubscription = AuthManager.logout(deleteAccounts)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody s) {
                        handleSuccessLogout(s, deleteAccounts);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorLogout(throwable);
                    }
                });
        compositeSubscription.add(logoutSubscription);
    }

    private void handleSuccessLogout(ResponseBody s, boolean deleteAccounts) {
        if (deleteAccounts) XabberAccountManager.getInstance().deleteSyncedLocalAccounts();
        XabberAccountManager.getInstance().removeAccount();
        //showLoginFragment();
        hideProgress();
        Toast.makeText(this, R.string.quit_success, Toast.LENGTH_SHORT).show();
        Intent intent = ContactListActivity.createIntent(XabberAccountActivity.this);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void handleErrorLogout(Throwable throwable) {
        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            if (message.equals("Invalid token")) {
                XabberAccountManager.getInstance().onInvalidToken();
                //showLoginFragment();
            } else {
                Log.d(LOG_TAG, "Error while logout: " + message);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(LOG_TAG, "Error while logout: " + throwable.toString());
            Toast.makeText(this, R.string.logout_error, Toast.LENGTH_LONG).show();
        }
        hideProgress();
    }

}
