package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.soundcloud.android.crop.Crop;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.vcard.OnVCardListener;
import com.xabber.android.data.extension.vcard.OnVCardSaveListener;
import com.xabber.android.data.extension.vcard.VCardManager;
import com.xabber.android.ui.activity.ChatViewer;
import com.xabber.android.ui.helper.PermissionsRequester;
import com.xabber.xmpp.address.Jid;
import com.xabber.xmpp.vcard.AddressProperty;
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

public class AccountInfoEditorFragment extends Fragment implements OnVCardSaveListener, OnVCardListener, DatePickerDialog.OnDateSetListener, TextWatcher {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.fragment.AccountInfoEditorFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_VCARD = "com.xabber.android.ui.fragment.AccountInfoEditorFragment.ARGUMENT_USER";
    public static final String SAVE_NEW_AVATAR_IMAGE_URI = "com.xabber.android.ui.fragment.AccountInfoEditorFragment.SAVE_NEW_AVATAR_IMAGE_URI";
    public static final String SAVE_PHOTO_FILE_URI = "com.xabber.android.ui.fragment.AccountInfoEditorFragment.SAVE_PHOTO_FILE_URI";
    public static final String SAVE_REMOVE_AVATAR_FLAG = "com.xabber.android.ui.fragment.AccountInfoEditorFragment.SAVE_REMOVE_AVATAR_FLAG";

    public static final int REQUEST_NEED_VCARD = 2;
    public static final int REQUEST_TAKE_PHOTO = 3;
    private static final int REQUEST_PERMISSION_GALLERY = 4;
    private static final int REQUEST_PERMISSION_CAMERA = 5;


    public static final int MAX_AVATAR_SIZE_PIXELS = 192;
    public static final String TEMP_FILE_NAME = "cropped";
    public static final String ROTATE_FILE_NAME = "rotated";
    public static final int KB_SIZE_IN_BYTES = 1024;
    public static final String DATE_FORMAT = "yyyy-mm-dd";
    public static final String DATE_FORMAT_INT_TO_STRING = "%d-%02d-%02d";
    public static final int MAX_IMAGE_SIZE = 512;

    private VCard vCard;
    private String account;
    private View progressBar;
    private boolean isSaveSuccess;
    private Listener listener;
    private boolean updateFromVCardFlag = true;

    private TextView account_jid;
    private LinearLayout fields;

    private EditText formattedName;
    private EditText prefixName;
    private EditText givenName;
    private EditText middleName;
    private EditText familyName;
    private EditText suffixName;
    private EditText nickName;

    private EditText title;
    private EditText role;
    private EditText organizationUnit;
    private EditText organization;

    private EditText url;
    private EditText description;
    private EditText emailHome;
    private EditText emailWork;
    private EditText phoneHome;
    private EditText phoneWork;

    private EditText addressHomePostOfficeBox;
    private EditText addressHomePostExtended;
    private EditText addressHomePostStreet;
    private EditText addressHomeLocality;
    private EditText addressHomeRegion;
    private EditText addressHomeCountry;
    private EditText addressHomePostalCode;

    private EditText addressWorkPostOfficeBox;
    private EditText addressWorkPostExtended;
    private EditText addressWorkPostStreet;
    private EditText addressWorkLocality;
    private EditText addressWorkRegion;
    private EditText addressWorkCountry;
    private EditText addressWorkPostalCode;

    private ImageView avatar;
    private TextView avatarSize;
    private View changeAvatarButton;
    private Uri newAvatarImageUri;
    private Uri photoFileUri;
    private boolean removeAvatarFlag = false;

    private TextView birthDate;
    private DatePickerDialog datePicker;
    private View birthDateRemoveButton;

    public interface Listener {
        void onProgressModeStarted(String message);
        void onProgressModeFinished();
        void enableSave();
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

        if (savedInstanceState != null) {
            final String avatarImageUriString = savedInstanceState.getString(SAVE_NEW_AVATAR_IMAGE_URI);
            if (avatarImageUriString != null) {
                newAvatarImageUri = Uri.parse(avatarImageUriString);
            }

            final String photoFileUriString = savedInstanceState.getString(SAVE_PHOTO_FILE_URI);
            if (photoFileUriString != null) {
                photoFileUri = Uri.parse(photoFileUriString);
            }

            removeAvatarFlag = savedInstanceState.getBoolean(SAVE_REMOVE_AVATAR_FLAG);
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.account_info_editor_fragment, container, false);

        fields = (LinearLayout)view.findViewById(R.id.vcard_fields_layout);

        progressBar = view.findViewById(R.id.vcard_save_progress_bar);

        account_jid = (TextView) view.findViewById(R.id.vcard_jid);

        prefixName = setUpInputField(view, R.id.vcard_prefix_name);
        formattedName = setUpInputField(view, R.id.vcard_formatted_name);
        givenName = setUpInputField(view, R.id.vcard_given_name);
        middleName = setUpInputField(view, R.id.vcard_middle_name);
        familyName = setUpInputField(view, R.id.vcard_family_name);
        suffixName = setUpInputField(view, R.id.vcard_suffix_name);
        nickName = setUpInputField(view, R.id.vcard_nickname);

        avatar = (ImageView) view.findViewById(R.id.vcard_avatar);
        avatarSize = (TextView) view.findViewById(R.id.vcard_avatar_size_text_view);
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
        birthDate.addTextChangedListener(this);

        birthDateRemoveButton = view.findViewById(R.id.vcard_birth_date_remove_button);
        birthDateRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBirthDate(null);
            }
        });

        title = setUpInputField(view, R.id.vcard_title);
        role = setUpInputField(view, R.id.vcard_role);
        organization = setUpInputField(view, R.id.vcard_organization_name);
        organizationUnit = setUpInputField(view, R.id.vcard_organization_unit);

        url = setUpInputField(view, R.id.vcard_url);

        description = setUpInputField(view, R.id.vcard_decsription);

        phoneHome = setUpInputField(view, R.id.vcard_phone_home);
        phoneWork = setUpInputField(view, R.id.vcard_phone_work);

        emailHome = setUpInputField(view, R.id.vcard_email_home);
        emailWork = setUpInputField(view, R.id.vcard_email_work);

        addressHomePostOfficeBox = setUpInputField(view, R.id.vcard_address_home_post_office_box);
        addressHomePostExtended = setUpInputField(view, R.id.vcard_address_home_post_extended);
        addressHomePostStreet = setUpInputField(view, R.id.vcard_address_home_post_street);
        addressHomeLocality = setUpInputField(view, R.id.vcard_address_home_locality);
        addressHomeRegion = setUpInputField(view, R.id.vcard_address_home_region);
        addressHomeCountry = setUpInputField(view, R.id.vcard_address_home_country);
        addressHomePostalCode = setUpInputField(view, R.id.vcard_address_home_postal_code);

        addressWorkPostOfficeBox = setUpInputField(view, R.id.vcard_address_work_post_office_box);
        addressWorkPostExtended = setUpInputField(view, R.id.vcard_address_work_post_extended);
        addressWorkPostStreet = setUpInputField(view, R.id.vcard_address_work_post_street);
        addressWorkLocality = setUpInputField(view, R.id.vcard_address_work_locality);
        addressWorkRegion = setUpInputField(view, R.id.vcard_address_work_region);
        addressWorkCountry = setUpInputField(view, R.id.vcard_address_work_country);
        addressWorkPostalCode = setUpInputField(view, R.id.vcard_address_work_postal_code);

        setFieldsFromVCard();

        return view;
    }

    private EditText setUpInputField(View rootView, int resourceId) {
        EditText inputField = (EditText) rootView.findViewById(resourceId);
        inputField.addTextChangedListener(this);
        return inputField;
    }

    @Override
    public void onResume() {
        super.onResume();

        Application.getInstance().addUIListener(OnVCardSaveListener.class, this);
        Application.getInstance().addUIListener(OnVCardListener.class, this);

        VCardManager vCardManager = VCardManager.getInstance();
        if (vCardManager.isVCardRequested(account) || vCardManager.isVCardSaveRequested(account)) {
            enableProgressMode(getString(R.string.saving));
        }
        updateFromVCardFlag = false;
    }

    @Override
    public void onPause() {
        super.onPause();

        Application.getInstance().removeUIListener(OnVCardListener.class, this);
        Application.getInstance().removeUIListener(OnVCardSaveListener.class, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (newAvatarImageUri != null) {
            outState.putString(SAVE_NEW_AVATAR_IMAGE_URI, newAvatarImageUri.toString());
        }
        if (photoFileUri != null) {
            outState.putString(SAVE_PHOTO_FILE_URI, photoFileUri.toString());
        }
        outState.putBoolean(SAVE_REMOVE_AVATAR_FLAG, removeAvatarFlag);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    private void setFieldsFromVCard() {
        account_jid.setText(Jid.getBareAddress(account));

        formattedName.setText(vCard.getField(VCardProperty.FN.name()));
        prefixName.setText(vCard.getPrefix());
        givenName.setText(vCard.getFirstName());
        middleName.setText(vCard.getMiddleName());
        familyName.setText(vCard.getLastName());
        suffixName.setText(vCard.getSuffix());
        nickName.setText(vCard.getNickName());

        setUpAvatarView();

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

        addressHomePostOfficeBox.setText(vCard.getAddressFieldHome(AddressProperty.POBOX.name()));
        addressHomePostExtended.setText(vCard.getAddressFieldHome(AddressProperty.EXTADR.name()));
        addressHomePostStreet.setText(vCard.getAddressFieldHome(AddressProperty.STREET.name()));
        addressHomeLocality.setText(vCard.getAddressFieldHome(AddressProperty.LOCALITY.name()));
        addressHomeRegion.setText(vCard.getAddressFieldHome(AddressProperty.REGION.name()));
        addressHomeCountry.setText(vCard.getAddressFieldHome(AddressProperty.CTRY.name()));
        addressHomePostalCode.setText(vCard.getAddressFieldHome(AddressProperty.PCODE.name()));

        addressWorkPostOfficeBox.setText(vCard.getAddressFieldWork(AddressProperty.POBOX.name()));
        addressWorkPostExtended.setText(vCard.getAddressFieldWork(AddressProperty.EXTADR.name()));
        addressWorkPostStreet.setText(vCard.getAddressFieldWork(AddressProperty.STREET.name()));
        addressWorkLocality.setText(vCard.getAddressFieldWork(AddressProperty.LOCALITY.name()));
        addressWorkRegion.setText(vCard.getAddressFieldWork(AddressProperty.REGION.name()));
        addressWorkCountry.setText(vCard.getAddressFieldWork(AddressProperty.CTRY.name()));
        addressWorkPostalCode.setText(vCard.getAddressFieldWork(AddressProperty.PCODE.name()));
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
                        onChooseFromGalleryClick();
                        return true;
                    case R.id.action_take_photo:
                        onTakePhotoClick();
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

    private void onTakePhotoClick() {
        if (PermissionsRequester.requestCameraPermissionIfNeeded(this, REQUEST_PERMISSION_CAMERA)) {
            takePhoto();
        }
    }

    private void onChooseFromGalleryClick() {
        if (PermissionsRequester.requestFileReadPermissionIfNeeded(this, REQUEST_PERMISSION_GALLERY)) {
            chooseFromGallery();
        }
    }

    private void chooseFromGallery() {
        Crop.pickImage(getActivity());
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File imageFile = null;
            try {
                imageFile = FileManager.createTempImageFile(TEMP_FILE_NAME);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (imageFile != null) {
                photoFileUri = Uri.fromFile(imageFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFileUri);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void removeAvatar() {
        newAvatarImageUri = null;
        removeAvatarFlag = true;
        setUpAvatarView();
        listener.enableSave();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSION_GALLERY:
                if (PermissionsRequester.isPermissionGranted(grantResults)) {
                    chooseFromGallery();
                } else {
                    Toast.makeText(getActivity(), R.string.no_permission_to_read_files, Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_PERMISSION_CAMERA:
                if (PermissionsRequester.isPermissionGranted(grantResults)) {
                    takePhoto();
                } else {
                    Toast.makeText(getActivity(), R.string.no_permission_camera, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == Crop.REQUEST_PICK && resultCode == Activity.RESULT_OK) {
            beginCrop(result.getData());
        } else if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            beginCrop(photoFileUri);
        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, result);
        }
    }

    private void beginCrop(final Uri source) {
        newAvatarImageUri = Uri.fromFile(new File(getActivity().getCacheDir(), TEMP_FILE_NAME));

        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                final boolean isImageNeedPreprocess = FileManager.isImageSizeGreater(source, MAX_IMAGE_SIZE)
                        || FileManager.isImageNeedRotation(source);

                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isImageNeedPreprocess) {
                            preprocessAndStartCrop(source);
                        } else {
                            startImageCropActivity(source);
                        }
                    }
                });
            }
        });
    }

    private void preprocessAndStartCrop(Uri source) {
        enableProgressMode(getString(R.string.processing_image));
        Glide.with(this).load(source).asBitmap().toBytes().override(MAX_IMAGE_SIZE, MAX_IMAGE_SIZE).into(new SimpleTarget<byte[]>() {
            @Override
            public void onResourceReady(final byte[] data, GlideAnimation anim) {
                Application.getInstance().runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        final Uri rotatedImage = FileManager.saveImage(data, ROTATE_FILE_NAME);
                        if (rotatedImage == null) return;

                        Application.getInstance().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startImageCropActivity(rotatedImage);
                                disableProgressMode();
                            }
                        });

                    }
                });
            }

            @Override
            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                super.onLoadFailed(e, errorDrawable);
                disableProgressMode();
                Toast.makeText(getActivity(), R.string.error_during_image_processing, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startImageCropActivity(Uri srcUri) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        Crop.of(srcUri, newAvatarImageUri).withMaxSize(MAX_AVATAR_SIZE_PIXELS, MAX_AVATAR_SIZE_PIXELS).start(activity);
    }

    private void handleCrop(int resultCode, Intent result) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                setUpAvatarView();
                break;
            case Crop.RESULT_ERROR:
                avatarSize.setVisibility(View.INVISIBLE);
                Toast.makeText(getActivity(), R.string.error_during_crop, Toast.LENGTH_SHORT).show();
                // no break!
            default:
                newAvatarImageUri = null;
        }
    }

    private void setUpAvatarView() {
        if (newAvatarImageUri != null) {
            // null prompts image view to reload file.
            avatar.setImageURI(null);
            avatar.setImageURI(newAvatarImageUri);
            removeAvatarFlag = false;

            File file = new File(newAvatarImageUri.getPath());
            avatarSize.setText(file.length() / KB_SIZE_IN_BYTES + "KB");
            avatarSize.setVisibility(View.VISIBLE);
            if (listener != null) {
                listener.enableSave();
            }
        } else if (removeAvatarFlag) {
            avatar.setImageDrawable(AvatarManager.getInstance().getDefaultAccountAvatar(account));
            avatarSize.setVisibility(View.INVISIBLE);
        } else {
            avatar.setImageDrawable(AvatarManager.getInstance().getAccountAvatar(account));
            avatarSize.setVisibility(View.INVISIBLE);
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

        vCard.setAddressFieldHome(AddressProperty.POBOX.name(), getValueFromEditText(addressHomePostOfficeBox));
        vCard.setAddressFieldHome(AddressProperty.EXTADR.name(), getValueFromEditText(addressHomePostExtended));
        vCard.setAddressFieldHome(AddressProperty.STREET.name(), getValueFromEditText(addressHomePostStreet));
        vCard.setAddressFieldHome(AddressProperty.LOCALITY.name(), getValueFromEditText(addressHomeLocality));
        vCard.setAddressFieldHome(AddressProperty.REGION.name(), getValueFromEditText(addressHomeRegion));
        vCard.setAddressFieldHome(AddressProperty.CTRY.name(), getValueFromEditText(addressHomeCountry));
        vCard.setAddressFieldHome(AddressProperty.PCODE.name(), getValueFromEditText(addressHomePostalCode));

        vCard.setAddressFieldWork(AddressProperty.POBOX.name(), getValueFromEditText(addressWorkPostOfficeBox));
        vCard.setAddressFieldWork(AddressProperty.EXTADR.name(), getValueFromEditText(addressWorkPostExtended));
        vCard.setAddressFieldWork(AddressProperty.STREET.name(), getValueFromEditText(addressWorkPostStreet));
        vCard.setAddressFieldWork(AddressProperty.LOCALITY.name(), getValueFromEditText(addressWorkLocality));
        vCard.setAddressFieldWork(AddressProperty.REGION.name(), getValueFromEditText(addressWorkRegion));
        vCard.setAddressFieldWork(AddressProperty.CTRY.name(), getValueFromEditText(addressWorkCountry));
        vCard.setAddressFieldWork(AddressProperty.PCODE.name(), getValueFromEditText(addressWorkPostalCode));
    }

    public void saveVCard() {
        ChatViewer.hideKeyboard(getActivity());
        updateVCardFromFields();
        enableProgressMode(getString(R.string.saving));
        VCardManager.getInstance().saveVCard(account, vCard);
        isSaveSuccess = false;
    }

    public void enableProgressMode(String message) {
        setEnabledRecursive(false, fields);
        progressBar.setVisibility(View.VISIBLE);
        if (listener != null) {
            listener.onProgressModeStarted(message);
        }
    }

    public void disableProgressMode() {
        progressBar.setVisibility(View.GONE);
        setEnabledRecursive(true, fields);
        if (listener != null) {
            listener.onProgressModeFinished();
        }
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

        enableProgressMode(getString(R.string.saving));
        VCardManager.getInstance().request(account, account);
        isSaveSuccess = true;
    }

    @Override
    public void onVCardSaveFailed(String account) {
        if (!Jid.getBareAddress(this.account).equals(Jid.getBareAddress(account))) {
            return;
        }

        disableProgressMode();
        listener.enableSave();
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
            updateFromVCardFlag = true;
            setFieldsFromVCard();
            updateFromVCardFlag = false;
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
            getActivity().setResult(REQUEST_NEED_VCARD);
            getActivity().finish();
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        setBirthDate(String.format(DATE_FORMAT_INT_TO_STRING, year, monthOfYear + 1, dayOfMonth));
    }

    public void setBirthDate(String date) {
        birthDate.setText(date);
        if (date == null) {
            birthDateRemoveButton.setVisibility(View.INVISIBLE);
        } else {
            birthDateRemoveButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (!updateFromVCardFlag && listener != null) {
            listener.enableSave();
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}
