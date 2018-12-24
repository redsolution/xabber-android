package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.presentation.mvp.signup.SignUpRepo;
import com.xabber.android.ui.adapter.HostSpinnerAdapter;

import java.util.ArrayList;
import java.util.List;

public class XAccountSignUpFragment1 extends Fragment implements View.OnClickListener {

    private EditText edtUsername;
    private Spinner spinnerDomain;
    private View spinnerBorder;
    private TextView tvSocialProvider;
    private ProgressBar pbHosts;
    private Button btnNext;
    private WebView webViewDescription;

    private XAccountSignUpFragment1.Listener listener;

    private List<AuthManager.Host> hosts;
    private HostSpinnerAdapter adapter;

    public interface Listener {
        void onGetHosts();
        void onStep1Completed(String username, String host);
    }

    public static XAccountSignUpFragment1 newInstance(XAccountSignUpFragment1.Listener listener) {
        XAccountSignUpFragment1 fragment = new XAccountSignUpFragment1();
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xaccount_signup_1, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtUsername = view.findViewById(R.id.edtUsername);
        spinnerDomain = view.findViewById(R.id.spinnerDomain);
        spinnerDomain.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String htmlData = "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\" />"
                        + hosts.get(position).getDescription();
                webViewDescription.loadDataWithBaseURL("file:///android_asset/",
                        htmlData, "text/html", "UTF-8", null);

                if (adapter != null) adapter.setCurrentPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinnerBorder = view.findViewById(R.id.spinnerBorder);

        tvSocialProvider = view.findViewById(R.id.tvSocialProvider);
        pbHosts = view.findViewById(R.id.pbHosts);

        btnNext = view.findViewById(R.id.btnNext);
        btnNext.setOnClickListener(this);

        webViewDescription = view.findViewById(R.id.webViewDescription);

        if (listener != null) listener.onGetHosts();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupSocial();

        String username = SignUpRepo.getInstance().getUsername();
        if (username != null) edtUsername.setText(username);

        String errorMessage = SignUpRepo.getInstance().getLastErrorMessage();
        edtUsername.setError(errorMessage);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnNext:
                onNextClick();
                break;
        }
    }

    public void showHostsProgress(boolean visible) {
        pbHosts.setVisibility(visible ? View.VISIBLE : View.GONE);
        spinnerDomain.setVisibility(visible ? View.GONE : View.VISIBLE);
        spinnerBorder.setVisibility(visible ? View.GONE : View.VISIBLE);
        btnNext.setEnabled(!visible);
    }

    private void setupSocial() {
        String credentials = SignUpRepo.getInstance().getSocialCredentials();
        String provider = SignUpRepo.getInstance().getSocialProvider();
        if (credentials != null && provider != null) {
            if (tvSocialProvider != null) {
                tvSocialProvider.setVisibility(View.VISIBLE);
                tvSocialProvider.setText(getString(R.string.signup_with_social, AuthManager.getProviderName(provider)));

                Drawable drawable;
                switch (provider) {
                    case AuthManager.PROVIDER_TWITTER:
                        drawable = getResources().getDrawable(R.drawable.ic_twitter);
                        break;
                    case AuthManager.PROVIDER_FACEBOOK:
                        drawable = getResources().getDrawable(R.drawable.ic_facebook);
                        break;
                    default:
                        drawable = getResources().getDrawable(R.drawable.ic_google_plus);
                }
                tvSocialProvider.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            }
        } else if (tvSocialProvider != null) tvSocialProvider.setVisibility(View.GONE);
    }

    private void onNextClick() {
        String username = edtUsername.getText().toString().trim();

        if (verifyFields(username) && spinnerDomain.getSelectedItem() != null && listener != null) {
            listener.onStep1Completed(username, spinnerDomain.getSelectedItem().toString());
        }
    }

    private boolean verifyFields(String username) {

        if (username.isEmpty()) {
            edtUsername.setError(getString(R.string.empty_field));
            return false;
        }

        // Username can contain only latin letters, numbers and dots
        if (!username.matches("[a-z0-9.]+")) {
            edtUsername.setError(getString(R.string.username_rules));
            return false;
        }

        // Username must start with letter
        if (!username.substring(0, 1).matches("[a-z]")) {
            edtUsername.setError(getString(R.string.username_rules_first_symbol));
            return false;
        }

        // Username cannot have two dots in a row
        if (username.contains("..")) {
            edtUsername.setError(getString(R.string.username_rules_dots));
            return false;
        }

        return true;
    }

    public void setupHosts(List<AuthManager.Host> hosts) {
        if (spinnerDomain == null) return;

        this.hosts = hosts;
        List<String> strings = new ArrayList<>();
        for (AuthManager.Host host : hosts) {
            strings.add(host.getHost());
        }
        adapter = new HostSpinnerAdapter(getActivity(),
                android.R.layout.simple_spinner_item, strings, this.hosts);
        spinnerDomain.setAdapter(adapter);
    }
}
