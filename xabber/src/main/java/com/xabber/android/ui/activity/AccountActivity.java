package com.xabber.android.ui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.soundcloud.android.crop.Crop;
import com.theartofdev.edmodo.cropper.CropImage;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountErrorEvent;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.OnAccountChangedListener;
import com.xabber.android.ui.OnBlockedListChangedListener;
import com.xabber.android.ui.adapter.accountoptions.AccountOption;
import com.xabber.android.ui.adapter.accountoptions.AccountOptionsAdapter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.dialog.AccountColorDialog;
import com.xabber.android.ui.fragment.ContactVcardViewerFragment;
import com.xabber.android.ui.helper.BlurTransformation;
import com.xabber.android.ui.helper.ContactTitleInflater;
import com.xabber.android.ui.helper.PermissionsRequester;
import com.xabber.android.utils.Utils;
import com.xabber.xmpp.avatar.UserAvatarManager;
import com.xabber.xmpp.vcard.VCard;

import org.apache.commons.io.FileUtils;
import org.greenrobot.eventbus.Subscribe;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import static com.xabber.android.ui.fragment.AccountInfoEditFragment.REQUEST_TAKE_PHOTO;
import static com.xabber.android.ui.helper.PermissionsRequester.REQUEST_PERMISSION_GALLERY;

public class AccountActivity extends ManagedActivity implements AccountOptionsAdapter.Listener,
        OnAccountChangedListener, OnBlockedListChangedListener, ContactVcardViewerFragment.Listener,
        MenuItem.OnMenuItemClickListener, View.OnClickListener, Toolbar.OnMenuItemClickListener {

    private static final String LOG_TAG = AccountActivity.class.getSimpleName();
    private static final String ACTION_CONNECTION_SETTINGS = AccountActivity.class.getName() + "ACTION_CONNECTION_SETTINGS";
    public static final String TEMP_FILE_NAME = "cropped";
    public static final String ROTATE_FILE_NAME = "rotated";
    public static final int KB_SIZE_IN_BYTES = 1024;
    public static int MAX_IMAGE_RESIZE = 256;
    public static int FINAL_IMAGE_SIZE;

    private AccountJid account;
    private ContactJid fakeAccountUser;
    private AbstractContact bestContact;
    private AccountItem accountItem;

    private View contactTitleView;
    private View statusIcon;
    private View statusGroupIcon;
    private ImageView avatar;
    private MenuItem qrCodePortrait;
    private MenuItem colorPickerPortrait;
    private SwitchCompat switchCompat;
    private CollapsingToolbarLayout collapsingToolbar;
    private ProgressBar progressBar;

    private AccountOptionsAdapter accountOptionsAdapter;
    private Uri newAvatarImageUri;
    private Uri photoFileUri;
    private String imageFileType;
    private byte[] avatarData;
    private boolean defaultAvatar = false;
    private boolean removeAvatarFlag;
    private boolean isAvatarSuccessful = false;
    private boolean isConnectionSettingsAction;
    private int accountMainColor;
    private int orientation;

    public AccountActivity() {
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    @NonNull
    public static Intent createIntent(Context context, AccountJid account) {
        return new AccountIntentBuilder(context, AccountActivity.class).setAccount(account).build();
    }

    @NonNull
    public static Intent createConnectionSettingsIntent(Context context, AccountJid account) {
        Intent intent = new AccountIntentBuilder(context, AccountActivity.class).setAccount(account)
                .build();
        intent.setAction(ACTION_CONNECTION_SETTINGS);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        account = getAccount(intent);
        if (account == null) {
            LogManager.i(LOG_TAG, "Account is null, finishing!");
            finish();
            return;
        }

        accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) {
            Application.getInstance().onError(R.string.NO_SUCH_ACCOUNT);
            finish();
            return;
        }

        if (ACTION_CONNECTION_SETTINGS.equals(intent.getAction())) {
            isConnectionSettingsAction = true;
            startAccountSettingsActivity();
            setIntent(null);
        }

        setScreenWindowSettings();

        setContentView(R.layout.activity_account);

        Toolbar toolbar = findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);

        toolbar.setNavigationOnClickListener(v ->
                NavUtils.navigateUpFromSameTask(AccountActivity.this));

        toolbar.inflateMenu(R.menu.toolbar_account);

        MenuItem item = toolbar.getMenu().findItem(R.id.action_account_switch);
        switchCompat = item.getActionView().findViewById(R.id.account_switch_view);
        switchCompat.setOnCheckedChangeListener((buttonView, isChecked) ->
                AccountManager.getInstance().setEnabled(accountItem.getAccount(), isChecked));

        try {
            fakeAccountUser = ContactJid.from(account.getFullJid().asBareJid());
        } catch (ContactJid.ContactJidCreateException e) {
            throw new IllegalStateException();
        }

        bestContact = RosterManager.getInstance().getBestContact(account, fakeAccountUser);

        accountMainColor = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);

        contactTitleView = findViewById(R.id.contact_title_expanded);
        TextView contactAddressView = findViewById(R.id.address_text);
        contactAddressView.setText(account.getFullJid().asBareJid().toString());

        avatar = findViewById(R.id.ivAvatar);
        avatar.setOnClickListener(view -> changeAvatar());
        statusIcon = findViewById(R.id.ivStatus);
        statusGroupIcon = findViewById(R.id.ivStatusGroupchat);
        progressBar = findViewById(R.id.avatar_publishing_progress);

        toolbar.setOnMenuItemClickListener(this);
        qrCodePortrait = toolbar.getMenu().findItem(R.id.action_generate_qrcode);
        colorPickerPortrait = toolbar.getMenu().findItem(R.id.action_account_color);

        RecyclerView recyclerView = findViewById(R.id.account_options_recycler_view);
        accountOptionsAdapter = new AccountOptionsAdapter(AccountOption.getValues(), this, accountItem);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(accountOptionsAdapter);
        recyclerView.setNestedScrollingEnabled(false);

        orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            orientationPortrait();
        } else {
            orientationLandscape();
        }

    }

    private void orientationPortrait() {
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        AppBarLayout appBarLayout = findViewById(R.id.appbar);
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
        final LinearLayout nameHolderView = findViewById(R.id.name_holder);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window win = getWindow();
            win.setStatusBarColor(accountMainColor);
        }
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            setSwitchButtonColor();
        }
        qrCodePortrait.setVisible(false);
        ImageView qrCodeLand = findViewById(R.id.generate_qrcode);
        qrCodeLand.setOnClickListener(this);

        colorPickerPortrait.setVisible(false);
        ImageView colorPickerLand = findViewById(R.id.change_color);
        colorPickerLand.setOnClickListener(this);

        final LinearLayout ll = findViewById(R.id.scroll_view_child);
        nameHolderView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver
                .OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    nameHolderView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                else nameHolderView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                int topPadding = (nameHolderView.getHeight());
                ll.setPadding(0,topPadding,0,0);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AccountManager.getInstance().getAccount(account) == null) {
            // in case if account was removed
            finish();
            return;
        }
        updateTitle();
        updateAccountColor();
        updateOptions();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnBlockedListChangedListener.class, this);
    }

    @Override
    protected void onPause() {
        Application.getInstance().removeUIListener(OnBlockedListChangedListener.class, this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);

        isConnectionSettingsAction = false;
        super.onPause();
    }

    private void updateTitle() {
        ContactTitleInflater.updateTitle(contactTitleView, this, bestContact, true, false);
        statusIcon.setVisibility(View.GONE);
        statusGroupIcon.setVisibility(View.GONE);
        if (avatar.getDrawable() == null) {
            defaultAvatar = true;
            avatar.setImageDrawable(new ColorDrawable(ColorManager.getInstance().getAccountPainter()
                    .getAccountMainColor(account)));
            findViewById(R.id.ivSetAvatar).setVisibility(View.VISIBLE);
        } else {
            defaultAvatar = false;
            findViewById(R.id.ivSetAvatar).setVisibility(View.GONE);
        }
        if (accountMainColor != ColorManager.getInstance().getAccountPainter()
                .getAccountMainColor(account))
            updateAccountColor();
        switchCompat.setChecked(accountItem.isEnabled());
    }

    private void updateAccountColor() {
        accountMainColor = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window win = getWindow();
                win.setStatusBarColor(accountMainColor);
            }
        }

        if (collapsingToolbar != null)
            collapsingToolbar.setContentScrimColor(accountMainColor);

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                setSwitchButtonColor();
        }

        ImageView background = findViewById(R.id.backgroundView);
        Drawable backgroundSource = bestContact.getAvatar(false);
        if (backgroundSource == null)
            backgroundSource = getResources().getDrawable(R.drawable.about_backdrop);
        Glide.with(this)
                .load(backgroundSource)
                .transform(new MultiTransformation<Bitmap>(new CenterCrop(), new BlurTransformation(25, 8, /*this,*/ accountMainColor)))
                .into(background);
    }

    private void setSwitchButtonColor() {
        DrawableCompat.setTintList(switchCompat.getTrackDrawable(), new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        accountMainColor,
                        getResources().getColor(R.color.grey_500)
                }
        ));
    }

    private void updateOptions() {
        AccountOption.SYNCHRONIZATION.setDescription(getString(R.string.account_sync_summary));

        AccountOption.CONNECTED_DEVICES.setDescription(getConnectedDevicesDescription());

        AccountOption.CONNECTION_SETTINGS.setDescription(account.getFullJid().asBareJid().toString());

        AccountOption.VCARD.setDescription(getString(R.string.account_vcard_summary));

        AccountOption.PUSH_NOTIFICATIONS.setDescription(getString(accountItem.isPushWasEnabled()
                ? R.string.account_push_state_enabled : R.string.account_push_state_disabled));

        AccountOption.COLOR.setDescription(ColorManager.getInstance().getAccountPainter().getAccountColorName(account));

        updateBlockListOption();

        AccountOption.SERVER_INFO.setDescription(getString(R.string.account_server_info_description));

        AccountOption.CHAT_HISTORY.setDescription(getString(R.string.account_history_options_summary));

        AccountOption.BOOKMARKS.setDescription(getString(R.string.account_bookmarks_summary));

        AccountOption.DELETE_ACCOUNT.setDescription(getString(R.string.account_delete_summary));

        accountOptionsAdapter.notifyDataSetChanged();
    }

    private void updateBlockListOption() {
        BlockingManager blockingManager = BlockingManager.getInstance();

        Boolean supported = blockingManager.isSupported(account);

        String description;
        if (supported == null) {
            description  = getString(R.string.blocked_contacts_unknown);
        } else if (!supported) {
            description  = getString(R.string.blocked_contacts_not_supported);
        } else {
            int size = blockingManager.getCachedBlockedContacts(account).size();
            if (size == 0) {
                description = getString(R.string.blocked_contacts_empty);
            } else {
                description = getResources().getQuantityString(R.plurals.blocked_contacts_number, size, size);
            }
        }

        AccountOption.BLOCK_LIST.setDescription(description);
        accountOptionsAdapter.notifyItemChanged(AccountOption.BLOCK_LIST.ordinal());
    }

    private String getConnectedDevicesDescription() {
        int connectedDevices = PresenceManager.INSTANCE.getAvailableAccountPresences(account).size();
        if (connectedDevices != 0) {
            return getResources().getQuantityString(R.plurals.account_connected_devices, connectedDevices, connectedDevices);
        } else return getResources().getString(R.string.account_connected_devices_none);
    }

    @Override
    public void onAccountOptionClick(AccountOption option) {
        switch (option) {
            case CONNECTED_DEVICES:
                if (PresenceManager.INSTANCE.getAvailableAccountPresences(account).size() > 0) {
                    startActivity(ConnectedDevicesActivity.createIntent(this, account));
                }
                break;
            case CONNECTION_SETTINGS:
                startAccountSettingsActivity();
                break;
            case VCARD:
                startActivity(AccountInfoEditActivity.createIntent(this, account));
                break;
            case PUSH_NOTIFICATIONS:
                startActivity(AccountPushActivity.createIntent(this, account));
                break;
            case COLOR:
                runColorPickerDialog();
                break;
            case BLOCK_LIST:
                startActivity(BlockedListActivity.createIntent(this, account));
                break;
            case SERVER_INFO:
                startActivity(ServerInfoActivity.createIntent(this, account));
                break;
            case CHAT_HISTORY:
                startActivity(AccountHistorySettingsActivity.createIntent(this, account));
                break;
            case BOOKMARKS:
                startActivity(BookmarksActivity.createIntent(this, account));
                break;
            case DELETE_ACCOUNT:
                startActivity(AccountDeleteActivity.createIntent(this, account));
                break;
            case SYNCHRONIZATION:
                if (XabberAccountManager.getInstance().getAccount() != null) {
                    if (accountItem.isSyncNotAllowed()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(R.string.sync_not_allowed_summary)
                                .setTitle(R.string.sync_status_not_allowed)
                                .setPositiveButton(R.string.ok, null);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } else startActivity(AccountSyncActivity.createIntent(this, account));
                } else startActivity(TutorialActivity.createIntent(this));
                break;
            case SESSIONS:
                startActivity(ActiveSessionsActivity.createIntent(this, account));
                break;
        }
    }

    private void startAccountSettingsActivity() {
        startActivity(AccountSettingsActivity.createIntent(this, account));
    }

    @Override
    public void onAccountsChanged(@org.jetbrains.annotations.Nullable Collection<? extends AccountJid> accounts) {
        if (accounts.contains(account)) {
            Application.getInstance().runOnUiThread(() -> {
                updateTitle();
                updateOptions();
            });
        }
    }

    @Override
    public void onBlockedListChanged(AccountJid account) {
        if (this.account.equals(account)) {
            Application.getInstance().runOnUiThread(this::updateBlockListOption);
        }
    }

    @Override
    public void onVCardReceived() {
        updateTitle();
    }

    @Override
    public void registerVCardFragment(ContactVcardViewerFragment fragment) {}

    @Subscribe(sticky = true)
    @Override
    public void onAuthErrorEvent(AccountErrorEvent accountErrorEvent) {
        LogManager.i(LOG_TAG, "onAuthErrorEvent ");

        if (!isConnectionSettingsAction) {
            super.onAuthErrorEvent(accountErrorEvent);
        }
    }

    private void setScreenWindowSettings() {

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
    }

    private void generateQR() {
        RosterContact rosterContact = RosterManager.getInstance().getRosterContact(account, fakeAccountUser);
        Intent intent = QRCodeActivity.createIntent(AccountActivity.this, account);
        String textName = rosterContact != null ? rosterContact.getName() : "";
        intent.putExtra("account_name", textName);
        String textAddress =  account.getFullJid().asBareJid().toString();
        intent.putExtra("account_address", textAddress);
        intent.putExtra("caller", "AccountActivity");
        startActivity(intent);
    }

    private void runColorPickerDialog() {
        AccountColorDialog.newInstance(account).show(getFragmentManager(),
                AccountColorDialog.class.getSimpleName());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.generate_qrcode:
                generateQR();
                break;
            case R.id.change_color:
                runColorPickerDialog();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch(menuItem.getItemId()) {
            case R.id.action_generate_qrcode:
                generateQR();
                return true;
            case R.id.action_account_color:
                runColorPickerDialog();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return onOptionsItemSelected(menuItem);
    }

    private void changeAvatar() {
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this, R.style.PopupMenuOverlapAnchor);
        PopupMenu menu = new PopupMenu(contextThemeWrapper, avatar);

        menu.inflate(R.menu.change_avatar);
        menu.getMenu().findItem(R.id.action_remove_avatar).setVisible(!defaultAvatar);
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_choose_from_gallery:
                    onChooseFromGalleryClick();
                    removeAvatarFlag = false;
                    return true;
                case R.id.action_take_photo:
                    onTakePhotoClick();
                    removeAvatarFlag = false;
                    return true;
                case R.id.action_remove_avatar:
                    removeAvatar();
                    saveAvatar();
                    return true;
                default:
                    return false;
            }
        });
        menu.show();
    }

    private void removeAvatar() {
        newAvatarImageUri = null;
        removeAvatarFlag = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSION_GALLERY:
                if (PermissionsRequester.isPermissionGranted(grantResults)) {
                    chooseFromGallery();
                } else {
                    Toast.makeText(this, R.string.no_permission_to_read_files, Toast.LENGTH_SHORT).show();
                }
                break;
            case PermissionsRequester.REQUEST_PERMISSION_CAMERA:
                if (PermissionsRequester.isPermissionGranted(grantResults)) {
                    takePhoto();
                } else {
                    Toast.makeText(this, R.string.no_permission_to_camera, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void onChooseFromGalleryClick() {
        if (PermissionsRequester.requestFileReadPermissionIfNeeded(
                this, REQUEST_PERMISSION_GALLERY)) {
            chooseFromGallery();
        }
    }

    private void chooseFromGallery() {
        Crop.pickImage(this);
    }

    private void onTakePhotoClick() {
        if (PermissionsRequester.requestCameraPermissionIfNeeded(this,
                PermissionsRequester.REQUEST_PERMISSION_CAMERA)) {
            takePhoto();
        }
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(this.getPackageManager()) != null) {
            File imageFile = null;
            try {
                imageFile = FileManager.createTempImageFile(TEMP_FILE_NAME);
            } catch (IOException e) {
                LogManager.exception(this, e);
            }

            if (imageFile != null) {
                photoFileUri = FileManager.getFileUri(imageFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFileUri);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Crop.REQUEST_PICK && resultCode == Activity.RESULT_OK) {
            //picked gallery
            beginCropProcess(data.getData());
        } else if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            //picked camera
            beginCropProcess(photoFileUri);
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            //processing data after initial crop with CropImage
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == Activity.RESULT_OK) {
                newAvatarImageUri = result.getUri();
                handleCrop(resultCode, data);
            }
        }  else if (requestCode == Crop.REQUEST_CROP) {
            //processing data after initial crop with Crop
            handleCrop(resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void beginCropProcess(final Uri source) {
        newAvatarImageUri = Uri.fromFile(new File(this.getCacheDir(), TEMP_FILE_NAME));

        Application.getInstance().runInBackgroundUserRequest(() -> {
            final boolean isImageNeedPreprocess = FileManager.isImageSizeGreater(source, 256)
                    || FileManager.isImageNeedRotation(source);

            Application.getInstance().runOnUiThread(() -> {
                if (isImageNeedPreprocess) {
                    preprocessAndStartCrop(source);
                } else {
                    startCrop(source);
                }
            });
        });
    }

    private void preprocessAndStartCrop(final Uri source) {
        Glide.with(this).asBitmap().load(source).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull final Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Application.getInstance().runInBackgroundUserRequest(() -> {
                            ContentResolver cR = Application.getInstance().getApplicationContext().getContentResolver();
                            String imageType = cR.getType(source);
                            imageFileType = imageType;

                            ByteArrayOutputStream stream = new ByteArrayOutputStream();

                            if (imageFileType.equals("image/png")) {
                                resource.compress(Bitmap.CompressFormat.PNG, 100, stream);
                            } else {
                                resource.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                            }

                            byte[] data = stream.toByteArray();
                            resource.recycle();
                            try {
                                stream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Uri rotatedImage;
                            if (imageType.equals("image/png")) {
                                rotatedImage = FileManager.savePNGImage(data, ROTATE_FILE_NAME);
                            } else {
                                rotatedImage = FileManager.saveImage(data, ROTATE_FILE_NAME);
                            }
                            if (rotatedImage == null) return;

                            Application.getInstance().runOnUiThread(() -> startCrop(rotatedImage));

                        });
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        Toast.makeText(getBaseContext(), R.string.error_during_image_processing, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) { }
                });
    }

    private void startCrop(Uri srcUri) {
        ContentResolver cR = Application.getInstance().getApplicationContext().getContentResolver();

        imageFileType = cR.getType(srcUri);
        if(cR.getType(srcUri)!=null) {
            if (cR.getType(srcUri).equals("image/png")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    CropImage.activity(srcUri).setAspectRatio(1, 1)
                            .setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                            .setOutputUri(newAvatarImageUri)
                            .start(this);
                } else
                    Crop.of(srcUri, newAvatarImageUri)
                            .asSquare()
                            .start(this);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    CropImage.activity(srcUri).setAspectRatio(1, 1)
                            .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                            .setOutputUri(newAvatarImageUri)
                            .start(this);
                } else
                    Crop.of(srcUri, newAvatarImageUri)
                            .asSquare()
                            .start(this);
            }
        }
    }

    private void handleCrop(int resultCode, Intent result) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                checkAvatarSizeAndPublish();
                break;
            case Crop.RESULT_ERROR:
                Toast.makeText(this, R.string.error_during_crop, Toast.LENGTH_SHORT).show();
                // no break!
            default:
                newAvatarImageUri = null;
        }
    }

    private void checkAvatarSizeAndPublish() {
        if (newAvatarImageUri != null) {
            File file = new File(newAvatarImageUri.getPath());

            if (file.length() / KB_SIZE_IN_BYTES>35) {
                Toast.makeText(this, "Image is too big, commencing additional processing!", Toast.LENGTH_LONG).show();
                resize(newAvatarImageUri);
                return;
            }
            Toast.makeText(this, "Started Avatar Publishing!", Toast.LENGTH_LONG).show();
            FINAL_IMAGE_SIZE = MAX_IMAGE_RESIZE;
            MAX_IMAGE_RESIZE = 256;
            saveAvatar();
        }
    }

    private void resize(final Uri src){
        Glide.with(this).asBitmap().load(src).override(MAX_IMAGE_RESIZE, MAX_IMAGE_RESIZE)
                .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull final Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Application.getInstance().runInBackgroundUserRequest(() -> {
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            if (imageFileType != null) {
                                if (imageFileType.equals("image/png")) {
                                    resource.compress(Bitmap.CompressFormat.PNG, 90, stream);
                                } else {
                                    resource.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                                }

                            }
                            byte[] data = stream.toByteArray();
                            if (data.length > 35 * KB_SIZE_IN_BYTES) {
                                MAX_IMAGE_RESIZE = MAX_IMAGE_RESIZE - MAX_IMAGE_RESIZE / 8;
                                if (MAX_IMAGE_RESIZE == 0) {
                                    Toast.makeText(getBaseContext(), "Error with resizing", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                resize(src);
                                return;
                            }
                            resource.recycle();
                            try {
                                stream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Uri rotatedImage = null;
                            if (imageFileType != null) {
                                if (imageFileType.equals("image/png")) {
                                    rotatedImage = FileManager.savePNGImage(data, "resize");
                                } else {
                                    rotatedImage = FileManager.saveImage(data, "resize");
                                }
                            }
                            if (rotatedImage == null) return;
                            try {
                                FileUtils.writeByteArrayToFile(new File(newAvatarImageUri.getPath()), data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            Application.getInstance().runOnUiThread(() -> checkAvatarSizeAndPublish());
                        });
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        Toast.makeText(getBaseContext(), R.string.error_during_image_processing, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) { }
                });
    }

    private void saveAvatar(){
        showProgressBar(true);
        AccountItem item = AccountManager.getInstance().getAccount(account);
        final UserAvatarManager mng = UserAvatarManager.getInstanceFor(item.getConnection());
        if (removeAvatarFlag) {
            try {
                if (mng.isSupportedByServer()) {
                    //saving empty avatar
                    AvatarManager.getInstance().onAvatarReceived(account.getFullJid().asBareJid(), "", null, "xep");
                }
            } catch (XMPPException.XMPPErrorException | SmackException.NotConnectedException
                    | InterruptedException | SmackException.NoResponseException e) {
                e.printStackTrace();
                showProgressBar(false);
            }
        } else if (newAvatarImageUri != null) {
            try {
                if (mng.isSupportedByServer()) { //check if server supports PEP, if true - proceed with saving the avatar as XEP-0084 one
                    //xep-0084 av
                    avatarData = VCard.getBytes(new URL(newAvatarImageUri.toString()));
                    String sh1 = AvatarManager.getAvatarHash(avatarData);
                    AvatarManager.getInstance().onAvatarReceived(account.getFullJid().asBareJid(), sh1, avatarData, "xep");
                }
            } catch (IOException | XMPPException.XMPPErrorException | SmackException.NotConnectedException
                    | InterruptedException | SmackException.NoResponseException e) {
                e.printStackTrace();
                showProgressBar(false);
            }
        }
        Application.getInstance().runInBackgroundUserRequest(() -> {

            if (removeAvatarFlag) {
                try {
                    //publishing empty (avatar) metadata
                    mng.unpublishAvatar();
                    isAvatarSuccessful = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                final boolean isSuccessfulFinal = isAvatarSuccessful;
                Application.getInstance().runOnUiThread(() -> {

                    if (isSuccessfulFinal) {
                        Toast.makeText(getBaseContext(), "Avatar published!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getBaseContext(), "Avarar publishing failed", Toast.LENGTH_LONG).show();
                    }
                    showProgressBar(false);
                    updateTitle();
                });
            } else if(avatarData!=null) {
                try {
                    if(imageFileType.equals("image/png")) {
                        mng.publishAvatar(avatarData, FINAL_IMAGE_SIZE, FINAL_IMAGE_SIZE);
                    } else mng.publishAvatarJPG(avatarData, FINAL_IMAGE_SIZE, FINAL_IMAGE_SIZE);
                    isAvatarSuccessful = true;
                } catch (XMPPException.XMPPErrorException | SmackException.NotConnectedException
                        | InterruptedException | SmackException.NoResponseException e) {
                    e.printStackTrace();
                }

                final boolean isSuccessfulFinal = isAvatarSuccessful;
                Application.getInstance().runOnUiThread(() -> {

                    if (isSuccessfulFinal) {
                        Toast.makeText(getBaseContext(), "Avatar published!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getBaseContext(), "Avatar publishing failed", Toast.LENGTH_LONG).show();
                    }
                    showProgressBar(false);
                    updateTitle();
                });
            }
        });
    }

    public void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        Utils.lockScreenRotation(this, show);
    }

}
