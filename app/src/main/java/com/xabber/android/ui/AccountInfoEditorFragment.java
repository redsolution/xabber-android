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

import com.xabber.android.R;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.vcard.VCardManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AccountInfoEditorFragment extends Fragment {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.AccountInfoEditorFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_VCARD = "com.xabber.android.ui.AccountInfoEditorFragment.ARGUMENT_USER";
    private VCard vCard;
    private EditText prefixName;
    private EditText givenName;
    private EditText middleName;
    private EditText familyName;
    private EditText suffixName;
    private String account;
    private ImageView avatar;
    private Uri imageUri;

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


        prefixName = (EditText) view.findViewById(R.id.vcard_prefix_name);
        givenName = (EditText) view.findViewById(R.id.vcard_given_name);
        middleName = (EditText) view.findViewById(R.id.vcard_middle_name);
        familyName = (EditText) view.findViewById(R.id.vcard_family_name);
        suffixName = (EditText) view.findViewById(R.id.vcard_suffix_name);

        avatar = (ImageView) view.findViewById(R.id.vcard_avatar);
        view.findViewById(R.id.vcard_change_avatar).setOnClickListener(new View.OnClickListener() {
                                                                           @Override
                                                                           public void onClick(View v) {
                                                                               changeAvatar();
                                                                           }
                                                                       }
        );


        prefixName.setText(vCard.getPrefix());
        givenName.setText(vCard.getFirstName());
        middleName.setText(vCard.getMiddleName());
        familyName.setText(vCard.getLastName());
        suffixName.setText(vCard.getSuffix());

        avatar.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(account));

        return view;
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

    @Override
    public void onStop() {
        super.onStop();

        vCard.setPrefix(prefixName.getText().toString());
        vCard.setFirstName(givenName.getText().toString());
        vCard.setMiddleName(middleName.getText().toString());
        vCard.setLastName(familyName.getText().toString());
        vCard.setSuffix(suffixName.getText().toString());

        if (imageUri != null) {
            try {
                InputStream inputStream = getActivity().getContentResolver().openInputStream(imageUri);
                vCard.setAvatar(getBytes(inputStream));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        VCardManager.saveVCard(account, vCard);
    }
}
