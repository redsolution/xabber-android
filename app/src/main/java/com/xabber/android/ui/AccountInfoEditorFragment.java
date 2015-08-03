package com.xabber.android.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.vcard.OnVCardListener;
import com.xabber.android.data.extension.vcard.OnVCardSaveListener;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.vcard.TelephoneType;
import com.xabber.xmpp.vcard.VCardProperty;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AccountInfoEditorFragment extends Fragment implements OnVCardSaveListener, OnVCardListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.AccountInfoEditorFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_VCARD = "com.xabber.android.ui.AccountInfoEditorFragment.ARGUMENT_USER";
    public static final int ACCOUNT_INFO_EDITOR_RESULT_NEED_VCARD_REQUEST = 2;

    private VCard vCard;
    private EditText prefixName;
    private EditText givenName;
    private EditText middleName;
    private EditText familyName;
    private EditText suffixName;
    private EditText nickName;
    private String account;
    private ImageView avatar;
    private Uri imageUri;
    private EditText organization;
    private EditText organizationUnit;
    private EditText birthDate;
    private EditText title;
    private EditText role;
    private EditText url;
    private EditText description;
    private EditText emailHome;
    private EditText emailWork;
    private EditText phoneHome;
    private EditText phoneWork;
    private EditText formattedName;
    private View progressBar;
    private boolean isSaveSuccess;
    private LinearLayout fields;

    public static AccountInfoEditorFragment newInstance(String account, String vCard) {
        AccountInfoEditorFragment fragment = new AccountInfoEditorFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_ACCOUNT, account);
        arguments.putString(ARGUMENT_VCARD, vCard);
        fragment.setArguments(arguments);
        return fragment;
    }

    public AccountInfoEditorFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        account = args.getString(ARGUMENT_ACCOUNT, null);
        String vCardString = args.getString(ARGUMENT_VCARD, null);
        if (vCardString != null) {
            try {
                vCard = ContactVcardViewerFragment.parseVCard(vCardString);
            } catch (XmlPullParserException | IOException | SmackException e) {
                LogManager.exception(this, e);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.account_info_editor_fragment, container, false);

        fields = (LinearLayout)view.findViewById(R.id.vcard_fields_layout);

        progressBar = view.findViewById(R.id.vcard_save_progress_bar);

        prefixName = (EditText) view.findViewById(R.id.vcard_prefix_name);
        formattedName = (EditText) view.findViewById(R.id.vcard_formatted_name);
        givenName = (EditText) view.findViewById(R.id.vcard_given_name);
        middleName = (EditText) view.findViewById(R.id.vcard_middle_name);
        familyName = (EditText) view.findViewById(R.id.vcard_family_name);
        suffixName = (EditText) view.findViewById(R.id.vcard_suffix_name);
        nickName = (EditText) view.findViewById(R.id.vcard_nickname);

        avatar = (ImageView) view.findViewById(R.id.vcard_avatar);
        view.findViewById(R.id.vcard_change_avatar).setOnClickListener(new View.OnClickListener() {
                                                                           @Override
                                                                           public void onClick(View v) {
                                                                               changeAvatar();
                                                                           }
                                                                       }
        );

        birthDate = (EditText) view.findViewById(R.id.vcard_birth_date);

        title = (EditText) view.findViewById(R.id.vcard_title);
        role = (EditText) view.findViewById(R.id.vcard_role);
        organization = (EditText) view.findViewById(R.id.vcard_organization_name);
        organizationUnit = (EditText) view.findViewById(R.id.vcard_organization_unit);

        url = (EditText) view.findViewById(R.id.vcard_url);

        description = (EditText) view.findViewById(R.id.vcard_decsription);

        phoneHome = (EditText) view.findViewById(R.id.vcard_phone_home);
        phoneWork = (EditText) view.findViewById(R.id.vcard_phone_work);

        emailHome = (EditText) view.findViewById(R.id.vcard_email_home);
        emailWork = (EditText) view.findViewById(R.id.vcard_email_work);

        setFieldsFromVCard();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Application.getInstance().addUIListener(OnVCardSaveListener.class, this);
        Application.getInstance().addUIListener(OnVCardListener.class, this);

    }

    @Override
    public void onPause() {
        super.onPause();

        Application.getInstance().removeUIListener(OnVCardListener.class, this);
        Application.getInstance().removeUIListener(OnVCardSaveListener.class, this);
    }

    private void setFieldsFromVCard() {
        formattedName.setText(vCard.getField(VCardProperty.FN.name()));
        prefixName.setText(vCard.getPrefix());
        givenName.setText(vCard.getFirstName());
        middleName.setText(vCard.getMiddleName());
        familyName.setText(vCard.getLastName());
        suffixName.setText(vCard.getSuffix());
        nickName.setText(vCard.getNickName());

        avatar.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(account));

        birthDate.setText(vCard.getField(VCardProperty.BDAY.name()));

        title.setText(vCard.getField(VCardProperty.TITLE.name()));
        role.setText(vCard.getField(VCardProperty.ROLE.name()));
        organization.setText(vCard.getOrganization());
        organizationUnit.setText(vCard.getOrganizationUnit());

        url.setText(vCard.getField(VCardProperty.URL.name()));

        description.setText(vCard.getField(VCardProperty.DESC.name()));

        for (TelephoneType telephoneType : TelephoneType.values() ) {
            String phone = vCard.getPhoneHome(telephoneType.name());
            if (phone != null && !phone.isEmpty()) {
                phoneHome.setText(phone);
            }
        }

        for (TelephoneType telephoneType : TelephoneType.values() ) {
            String phone = vCard.getPhoneWork(telephoneType.name());
            if (phone != null && !phone.isEmpty()) {
                phoneWork.setText(phone);
            }
        }

        emailHome.setText(vCard.getEmailHome());
        emailWork.setText(vCard.getEmailWork());
    }

    private void changeAvatar() {
        Intent pickAvatar = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickAvatar, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            imageUri = data.getData();
            avatar.setImageURI(imageUri);
        }

    }

    public static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    String getValueFromEditText(EditText editText) {
        String trimText = editText.getText().toString().trim();
        if (trimText.isEmpty()) {
            return null;
        }

        return trimText;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void updateVCardFromFields() {

        vCard.setPrefix(getValueFromEditText(prefixName));
        vCard.setFirstName(getValueFromEditText(givenName));
        vCard.setMiddleName(getValueFromEditText(middleName));
        vCard.setLastName(getValueFromEditText(familyName));
        vCard.setSuffix(getValueFromEditText(suffixName));
        vCard.setNickName(getValueFromEditText(nickName));

        String formattedNameText = getValueFromEditText(formattedName);
        if (formattedNameText != null) {
            vCard.setField(VCardProperty.FN.name(), formattedNameText);
        }

        if (imageUri != null) {
            try {
                InputStream inputStream = getActivity().getContentResolver().openInputStream(imageUri);
                vCard.setAvatar(getBytes(inputStream));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        vCard.setField(VCardProperty.BDAY.name(), getValueFromEditText(birthDate));

        vCard.setField(VCardProperty.TITLE.name(), getValueFromEditText(title));
        vCard.setField(VCardProperty.ROLE.name(), getValueFromEditText(role));
        vCard.setOrganization(getValueFromEditText(organization));
        vCard.setOrganizationUnit(getValueFromEditText(organizationUnit));

        vCard.setField(VCardProperty.URL.name(), getValueFromEditText(url));

        vCard.setField(VCardProperty.DESC.name(), getValueFromEditText(description));

        vCard.setPhoneHome(TelephoneType.VOICE.name(), getValueFromEditText(phoneHome));
        vCard.setPhoneWork(TelephoneType.VOICE.name(), getValueFromEditText(phoneWork));

        vCard.setEmailHome(getValueFromEditText(emailHome));
        vCard.setEmailWork(getValueFromEditText(emailWork));
    }

    public void saveVCard() {
        ChatViewer.hideKeyboard(getActivity());
        updateVCardFromFields();
        VCardManager.getInstance().saveVCard(account, vCard);
        isSaveSuccess = false;
        enableProgressMode();

    }

    public void enableProgressMode() {
        setEnabledRecursive(false, fields);
        progressBar.setVisibility(View.VISIBLE);
    }

    public void disableProgressMode() {
        progressBar.setVisibility(View.GONE);
        setEnabledRecursive(true, fields);
    }

    private void setEnabledRecursive(boolean enabled, ViewGroup viewGroup){
        for (int i = 0; i < viewGroup.getChildCount(); i++){
            View child = viewGroup.getChildAt(i);
            child.setEnabled(enabled);
            if (child instanceof ViewGroup){
                setEnabledRecursive(enabled, (ViewGroup) child);
            }
        }
    }

    @Override
    public void onVCardSaveSuccess(String account) {
        if (!Jid.getBareAddress(this.account).equals(Jid.getBareAddress(account))) {
            return;
        }

        enableProgressMode();
        VCardManager.getInstance().request(account, account);
        isSaveSuccess = true;
    }

    @Override
    public void onVCardSaveFailed(String account) {
        if (!Jid.getBareAddress(this.account).equals(Jid.getBareAddress(account))) {
            return;
        }

        disableProgressMode();
        Toast.makeText(getActivity(), getString(R.string.account_user_info_save_fail), Toast.LENGTH_LONG).show();
        isSaveSuccess = false;
    }

    @Override
    public void onVCardReceived(String account, String bareAddress, VCard vCard) {
        if (!Jid.getBareAddress(this.account).equals(Jid.getBareAddress(bareAddress))) {
            return;
        }


        if (isSaveSuccess) {
            Toast.makeText(getActivity(), getString(R.string.account_user_info_save_success), Toast.LENGTH_LONG).show();
            isSaveSuccess = false;

            Intent data = new Intent();
            data.putExtra(ARGUMENT_VCARD, vCard.getChildElementXML().toString());
            getActivity().setResult(Activity.RESULT_OK, data);

            getActivity().finish();
        } else {
            disableProgressMode();

            this.vCard = vCard;
            setFieldsFromVCard();
        }
    }

    @Override
    public void onVCardFailed(String account, String bareAddress) {
        if (!Jid.getBareAddress(this.account).equals(Jid.getBareAddress(bareAddress))) {
            return;
        }

        if (isSaveSuccess) {
            Toast.makeText(getActivity(), getString(R.string.account_user_info_save_success), Toast.LENGTH_LONG).show();
            isSaveSuccess = false;
            getActivity().setResult(ACCOUNT_INFO_EDITOR_RESULT_NEED_VCARD_REQUEST);
            getActivity().finish();
        }
    }
}
