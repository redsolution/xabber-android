package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.xabber.android.R;
import com.xabber.android.ui.activity.XabberAccountInfoActivity;

import java.util.List;

public class XAccountSignUpFragment extends Fragment implements View.OnClickListener {

    private static final String CAPTCHA_TOKEN = "RECAPTCHA";
    private static final String SOCIAL_TOKEN = "SOCIAL_TOKEN";
    private static final String SOCIAL_PROVIDER = "SOCIAL_PROVIDER";

    private EditText edtUsername;
    private EditText edtPass;
    private Spinner spinnerDomain;
    private Button btnSignUp;

    private String socialToken;
    private String socialProvider;

    public static XAccountSignUpFragment newInstance(String socialToken, String socialProvider) {
        XAccountSignUpFragment fragment = new XAccountSignUpFragment();
        Bundle args = new Bundle();
        args.putString(SOCIAL_TOKEN, socialToken);
        args.putString(SOCIAL_PROVIDER, socialProvider);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.socialToken = getArguments().getString(SOCIAL_TOKEN);
        this.socialProvider = getArguments().getString(SOCIAL_PROVIDER);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xaccount_signup, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtUsername = view.findViewById(R.id.edtUsername);
        edtPass = view.findViewById(R.id.edtPass);
        spinnerDomain = view.findViewById(R.id.spinnerDomain);
        btnSignUp = view.findViewById(R.id.btnSignUp);

        btnSignUp.setOnClickListener(this);

        TextView tvSocialProvider = view.findViewById(R.id.tvSocialProvider);
        tvSocialProvider.setText(socialProvider);
    }

    @Override
    public void onResume() {
        super.onResume();
        // TODO: 08.08.18 после закрытия окна с каптчей, снова делается запрос доменов?
        ((XabberAccountInfoActivity)getActivity()).getHosts();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSignUp:
                onSignUpClick();
                break;
        }
    }

    private void onSignUpClick() {
        String username = edtUsername.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();

        if (verifyFields(username, pass)) {
            // todo оставить только одно место для хранения socialToken. infoActivity или этот фрагмент
            if (socialToken != null)
                ((XabberAccountInfoActivity)getActivity()).signUp(username,
                        spinnerDomain.getSelectedItem().toString(), pass, null);
            else getCaptchaToken(username, pass, spinnerDomain.getSelectedItem().toString());
        }
    }

    private boolean verifyFields(String username, String pass) {

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

        if (pass.isEmpty()) {
            edtPass.setError(getString(R.string.empty_field));
            return false;
        }

        if (pass.length() < 4) {
            edtPass.setError(getString(R.string.pass_too_short));
            return false;
        }

        return true;
    }

    private void getCaptchaToken(final String username, final String pass, final String domain) {
        SafetyNet.getClient(getActivity()).verifyWithRecaptcha(getActivity().getString(R.string.RECAPTCHA_KEY))
            .addOnSuccessListener(getActivity(),
                new OnSuccessListener<SafetyNetApi.RecaptchaTokenResponse>() {
                    @Override
                    public void onSuccess(SafetyNetApi.RecaptchaTokenResponse response) {
                        // Indicates communication with reCAPTCHA service was
                        // successful.
                        String userResponseToken = response.getTokenResult();
                        if (!userResponseToken.isEmpty()) {
                            // Validate the user response token using the
                            // reCAPTCHA siteverify API.
                            Log.d(CAPTCHA_TOKEN, "Success: " + userResponseToken);
                            ((XabberAccountInfoActivity)getActivity()).signUp(username, domain,
                                    pass, userResponseToken);
                        }
                    }
                })
            .addOnFailureListener(getActivity(),
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof ApiException) {
                            // An error occurred when communicating with the
                            // reCAPTCHA service. Refer to the status code to
                            // handle the error appropriately.
                            ApiException apiException = (ApiException) e;
                            int statusCode = apiException.getStatusCode();
                            Log.d(CAPTCHA_TOKEN, "Error: "
                                    + CommonStatusCodes.getStatusCodeString(statusCode));
                        } else {
                            // A different, unknown type of error occurred.
                            Log.d(CAPTCHA_TOKEN, "Error: " + e.getMessage());
                        }
                    }
                });
    }

    public void setupSpinner(List<String> domains) {
        if (spinnerDomain == null) return;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, domains);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDomain.setAdapter(adapter);
    }
}
