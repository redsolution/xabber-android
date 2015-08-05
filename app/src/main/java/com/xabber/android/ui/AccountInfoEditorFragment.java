package com.xabber.android.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.soundcloud.android.crop.Crop;
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class AccountInfoEditorFragment extends Fragment implements OnVCardSaveListener, OnVCardListener, DatePickerDialog.OnDateSetListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.AccountInfoEditorFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_VCARD = "com.xabber.android.ui.AccountInfoEditorFragment.ARGUMENT_USER";
    public static final int ACCOUNT_INFO_EDITOR_RESULT_NEED_VCARD_REQUEST = 2;
    public static final int MAX_AVATAR_SIZE_PIXELS = 192;
    public static final String TEMP_FILE_NAME = "cropped";
    public static final int KB_SIZE_IN_BYTES = 1024;
    public static final int TAKE_PHOTO_REQUEST_CODE = 3;
    public static final String DATE_FORMAT = "yyyy-mm-dd";
    public static final String DATE_FORMAT_INT_TO_STRING = "%d-%02d-%02d";

    private VCard vCard;
    private EditText prefixName;
    private EditText givenName;
    private EditText middleName;
    private EditText familyName;
    private EditText suffixName;
    private EditText nickName;
    private String account;
    private EditText organization;
    private EditText organizationUnit;

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
    private TextView avatarSize;

    private Listener listener;

    private TextView birthDate;
    private DatePickerDialog datePicker;

    private View changeAvatarButton;
    private ImageView avatar;
    private Uri newAvatarImageUri;
    private Uri photoFileUri;
    private boolean removeAvatarFlag = false;
    private View birthDateRemoveButton;

    interface Listener {
        void onVCardSavingStarted();
        void onVCardSavingFinished();
    }

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (Listener) activity;
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

        avatarSize = (TextView) view.findViewById(R.id.vcard_avatar_size_text_view);

        prefixName = (EditText) view.findViewById(R.id.vcard_prefix_name);
        formattedName = (EditText) view.findViewById(R.id.vcard_formatted_name);
        givenName = (EditText) view.findViewById(R.id.vcard_given_name);
        middleName = (EditText) view.findViewById(R.id.vcard_middle_name);
        familyName = (EditText) view.findViewById(R.id.vcard_family_name);
        suffixName = (EditText) view.findViewById(R.id.vcard_suffix_name);
        nickName = (EditText) view.findViewById(R.id.vcard_nickname);

        avatar = (ImageView) view.findViewById(R.id.vcard_avatar);
        changeAvatarButton = view.findViewById(R.id.vcard_change_avatar);
        changeAvatarButton.setOnClickListener(new View.OnClickListener() {
                                                  @Override
                                                  public void onClick(View v) {
                                                      changeAvatar();
                                                  }
                                              }
        );

        birthDate = (TextView) view.findViewById(R.id.vcard_birth_date);
        birthDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePicker.show();
            }
        });

        birthDateRemoveButton = view.findViewById(R.id.vcard_birth_date_remove_button);
        birthDateRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBirthDate(null);
            }
        });

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

        VCardManager vCardManager = VCardManager.getInstance();
        if (vCardManager.isVCardRequested(account) || vCardManager.isVCardSaveRequested(account)) {
            enableProgressMode();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Application.getInstance().removeUIListener(OnVCardListener.class, this);
        Application.getInstance().removeUIListener(OnVCardSaveListener.class, this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
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

        setBirthDate(vCard.getField(VCardProperty.BDAY.name()));

        updateDatePickerDialog();

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

    public void updateDatePickerDialog() {
        Calendar calendar = null;

        String vCardBirthDate = vCard.getField(VCardProperty.BDAY.name());

        if (vCardBirthDate != null) {

            DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.US);
            Date result = null;
            try {
                result = dateFormat.parse(vCardBirthDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (result != null) {
                calendar = new GregorianCalendar();
                calendar.setTime(result);
            }
        }

        if (calendar == null) {
            calendar = Calendar.getInstance(TimeZone.getDefault());
        }
        datePicker = new DatePickerDialog(getActivity(),
                AccountInfoEditorFragment.this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.setCancelable(false);
    }

    private void changeAvatar() {
        PopupMenu menu = new PopupMenu(getActivity(), changeAvatarButton);
        menu.inflate(R.menu.change_avatar);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_choose_from_gallery:
                        chooseFromGallery();
                        return true;
                    case R.id.action_take_photo:
                        takePhoto();
                        return true;

                    case R.id.action_remove_avatar:
                        removeAvatar();
                        return true;

                    default:
                        return false;
                }

            }
        });
        menu.show();
    }

    private void chooseFromGallery() {
        Crop.pickImage(getActivity());
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File imageFile = null;
            try {
                imageFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (imageFile != null) {
                photoFileUri = Uri.fromFile(imageFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        photoFileUri);
                startActivityForResult(takePictureIntent, TAKE_PHOTO_REQUEST_CODE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        return File.createTempFile(
                TEMP_FILE_NAME,  /* prefix */
                ".jpg",         /* suffix */
                getActivity().getExternalFilesDir(null)      /* directory */
        );
    }

    private void removeAvatar() {
        newAvatarImageUri = null;
        removeAvatarFlag = true;
        avatar.setImageDrawable(AvatarManager.getInstance().getDefaultAccountAvatar(account));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == Crop.REQUEST_PICK && resultCode == Activity.RESULT_OK) {
            beginCrop(result.getData());
        } else if (requestCode == TAKE_PHOTO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            beginCrop(photoFileUri);
        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, result);
        }

    }

    private void beginCrop(Uri source) {
        newAvatarImageUri = Uri.fromFile(new File(getActivity().getCacheDir(), TEMP_FILE_NAME));
        Crop.of(source, newAvatarImageUri).withMaxSize(MAX_AVATAR_SIZE_PIXELS, MAX_AVATAR_SIZE_PIXELS).start(getActivity());
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == Activity.RESULT_OK) {
            // null prompts image view to reload file.
            avatar.setImageURI(null);
            avatar.setImageURI(newAvatarImageUri);
            removeAvatarFlag = false;

            File file = new File(newAvatarImageUri.getPath());
            avatarSize.setText(file.length() / KB_SIZE_IN_BYTES + "KB");
            avatarSize.setVisibility(View.VISIBLE);
        } else if (resultCode == Crop.RESULT_ERROR) {
            avatarSize.setVisibility(View.INVISIBLE);
            Toast.makeText(getActivity(), Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    String getValueFromEditText(TextView editText) {
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

        if (removeAvatarFlag) {
            vCard.removeAvatar();
        } else if (newAvatarImageUri != null) {
            try {
                vCard.setAvatar(new URL(newAvatarImageUri.toString()));
            } catch (MalformedURLException e) {
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
        listener.onVCardSavingStarted();
    }

    public void disableProgressMode() {
        progressBar.setVisibility(View.GONE);
        setEnabledRecursive(true, fields);
        listener.onVCardSavingFinished();
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

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        setBirthDate(String.format(DATE_FORMAT_INT_TO_STRING, year, monthOfYear + 1, dayOfMonth));
    }

    public void setBirthDate(String date) {
        birthDate.setText(date);
        birthDate.setTextColor(getResources().getColor(android.R.color.primary_text_light));
        if (date == null) {
            birthDateRemoveButton.setVisibility(View.INVISIBLE);
        } else {
            birthDateRemoveButton.setVisibility(View.VISIBLE);
        }
    }
}
