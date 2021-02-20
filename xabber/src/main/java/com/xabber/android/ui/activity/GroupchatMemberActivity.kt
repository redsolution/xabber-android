package com.xabber.android.ui.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.soundcloud.android.crop.Crop
import com.theartofdev.edmodo.cropper.CropImage
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.BaseIqResultUiListener
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.avatar.AvatarManager
import com.xabber.android.data.extension.file.FileManager
import com.xabber.android.data.groups.GroupMember
import com.xabber.android.data.groups.GroupMemberManager
import com.xabber.android.data.groups.GroupPrivacyType
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.GroupChat
import com.xabber.android.ui.OnGroupchatRequestListener
import com.xabber.android.ui.color.AccountPainter
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.fragment.AccountInfoEditFragment
import com.xabber.android.ui.fragment.groups.GroupMemberRightsFragment
import com.xabber.android.ui.helper.BlurTransformation
import com.xabber.android.ui.helper.PermissionsRequester
import com.xabber.android.ui.helper.PermissionsRequester.REQUEST_PERMISSION_CAMERA
import com.xabber.android.ui.helper.PermissionsRequester.REQUEST_PERMISSION_GALLERY
import com.xabber.android.ui.widget.ContactBarAutoSizingLayout
import com.xabber.android.utils.Utils
import com.xabber.xmpp.avatar.UserAvatarManager
import kotlinx.android.synthetic.main.contact_edit_layout.*
import kotlinx.android.synthetic.main.dialog.*
import kotlinx.android.synthetic.main.dialog_block_jid.*
import org.apache.commons.io.FileUtils
import org.jivesoftware.smack.packet.XMPPError
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*

class GroupchatMemberActivity : ManagedActivity(), View.OnClickListener,
        PopupMenu.OnMenuItemClickListener, OnGroupchatRequestListener {

    private val LOG_TAG = this.javaClass.simpleName

    private var accountJid: AccountJid? = null
    private var groupchatJid: ContactJid? = null
    private var groupMember: GroupMember? = null
    private var groupchat: GroupChat? = null

    private var toolbar: Toolbar? = null
    private var contactTitleView: View? = null
    private var collapsingToolbar: CollapsingToolbarLayout? = null
    private var appBarLayout: AppBarLayout? = null

    private var accountMainColor = 0
    private var coloredBlockText = false

    private var filePhotoUri: Uri? = null
    private var imageFileType: UserAvatarManager.ImageType? = null
    private var avatarData: ByteArray? = null
    private var newAvatarImageUri: Uri? = null

    var orientation = 0
    private val blocked = false

    companion object {

        const val GROUPCHAT_MEMBER_ID = "com.xabber.android.ui.activity.GroupchatMemberActivity.GROUPCHAT_MEMBER_ID"
        const val GROUPCHAT_JID = "com.xabber.android.ui.activity.GroupchatMemberActivity.GROUPCHAT_JID"
        const val ACCOUNT_JID = "com.xabber.android.ui.activity.GroupchatMemberActivity.ACCOUNT_JID"

        fun createIntentForGroupchatAndMemberId(context: Context, groupchatMemberId: String,
                                                groupchat: GroupChat): Intent {
            val intent = Intent(context, GroupchatMemberActivity::class.java)
            intent.putExtra(GROUPCHAT_MEMBER_ID, groupchatMemberId)
            intent.putExtra(GROUPCHAT_JID, groupchat.contactJid.toString())
            intent.putExtra(ACCOUNT_JID, groupchat.account.toString())
            return intent
        }

    }

    override fun onGroupchatMembersReceived(account: AccountJid, groupchatJid: ContactJid) {
        if (account == groupchat?.account && groupchatJid == groupchat?.contactJid) {
            Application.getInstance()
                    .runOnUiThread(::setupNameBlock)
        }
    }

    override fun onMeReceived(accountJid: AccountJid, groupchatJid: ContactJid) {}

    override fun onGroupchatMemberUpdated(accountJid: AccountJid, groupchatJid: ContactJid,
                                          groupchatMemberId: String) {
        if (accountJid == this.accountJid && groupchatJid == this.groupchatJid
                && groupchatMemberId == this.groupMember?.id) {
            Application.getInstance().runOnUiThread {
                setupAvatar()
                setupNameBlock()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupMember = GroupMemberManager.getInstance()
                .getGroupMemberById(intent.getStringExtra(GROUPCHAT_MEMBER_ID))
        accountJid = AccountJid.from(intent.getStringExtra(ACCOUNT_JID)!!)
        groupchatJid = ContactJid.from(intent.getStringExtra(GROUPCHAT_JID))
        groupchat = ChatManager.getInstance().getChat(accountJid, groupchatJid) as GroupChat

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val win = window
            val winParams = win.attributes
            winParams.flags = winParams.flags or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            win.attributes = winParams
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val win = window
            val winParams = win.attributes
            winParams.flags = winParams.flags and WindowManager.LayoutParams
                    .FLAG_TRANSLUCENT_STATUS.inv()
            win.attributes = winParams
            win.statusBarColor = Color.TRANSPARENT
        }

        setContentView(R.layout.activity_groupchat_member_info)

        //TODO: saved state restoring

        toolbar = findViewById(R.id.toolbar_default)
        toolbar?.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp)
        toolbar?.setNavigationOnClickListener { finish() }

        val colorLevel = AccountPainter.getAccountColorLevel(accountJid)
        accountMainColor = ColorManager.getInstance().accountPainter.getAccountMainColor(accountJid)
        coloredBlockText = colorLevel == 0 || colorLevel == 1 || colorLevel == 3

        //TODO is it necessary?
        contactTitleView = findViewById(R.id.contact_title_expanded)

        //TODO check for block status was here

        orientation = resources.configuration.orientation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (this.isInMultiWindowMode) {
                orientation = Configuration.ORIENTATION_PORTRAIT
            }
        }

        setupContactBar(accountMainColor, orientation)
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            orientationPortrait()
        } else {
            orientationLandscape()
        }

        setupNameBlock()
        setupAvatar()

        supportFragmentManager.beginTransaction()
                .add(R.id.scrollable_container,
                        GroupMemberRightsFragment(groupMember!!, groupchat!!),
                        GroupMemberRightsFragment.TAG)
                .commit()

    }

    private fun setupNameBlock() {
        val nameTv = findViewById<TextView>(R.id.name)
        nameTv.text = (groupMember?.bestName + " " + groupMember?.badge)
        nameTv.setOnClickListener {
            val adb = AlertDialog.Builder(this)
            adb.setTitle(getString(R.string.groupchat_member_nickname))

            val et = AppCompatEditText(this)
            et.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            et.hint = groupMember?.nickname
            adb.setView(et, 56, 0, 56, 0)

            adb.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            adb.setPositiveButton(R.string.groupchat_set_member_nickname) { _, _ ->
                GroupMemberManager.getInstance()
                        .sendSetMemberNicknameIqRequest(groupchat, groupMember, et.text.toString())
            }
            adb.show()
        }
        if (groupchat!!.privacyType!! != GroupPrivacyType.INCOGNITO)
            findViewById<TextView>(R.id.address_text).text = (groupMember!!.jid)

        findViewById<TextView>(R.id.groupchat_member_title).text = getString(R.string.groupchat_member_of_group_name,
                groupMember?.role?.capitalize(Locale.getDefault()),
                groupchat?.privacyType?.getLocalizedString()?.decapitalize(Locale.getDefault()),
                groupchat?.name)
    }

    private fun setupAvatar() {
        var backgroundSource = AvatarManager.getInstance()
                .getGroupchatMemberAvatar(groupMember, accountJid)
        if (backgroundSource == null) backgroundSource = resources.getDrawable(R.drawable.about_backdrop)
        Glide.with(this)
                .load(backgroundSource)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(MultiTransformation(CenterCrop(), BlurTransformation(25, 8,  /*this,*/accountMainColor)))
                .into(findViewById(R.id.backgroundView))

        val avatarIv = findViewById<ImageView>(R.id.ivAvatar)
        avatarIv.setImageDrawable(backgroundSource)
        avatarIv.setOnClickListener { changeAvatar(it as ImageView) }

    }

    override fun onResume() {
        super.onResume()
        //ContactTitleInflater.updateTitle(contactTitleView, this, bestContact, true)
        Application.getInstance().addUIListener(OnGroupchatRequestListener::class.java, this)
        GroupMemberManager.getInstance().requestGroupchatMemberInfo(groupchat, groupMember?.id)
        appBarResize()
    }

    override fun onPause() {
        super.onPause()
        Application.getInstance().removeUIListener(OnGroupchatRequestListener::class.java, this)
    }

    private fun changeAvatar(avatarIv: ImageView) {
        val contextThemeWrapper = ContextThemeWrapper(this, R.style.PopupMenuOverlapAnchor)
        android.widget.PopupMenu(contextThemeWrapper, avatarIv).apply {
            inflate(R.menu.change_avatar)
            menu.findItem(R.id.action_remove_avatar).isVisible = avatarIv.drawable != null
            setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.action_choose_from_gallery -> {
                        onChooseFromGalleryClick()
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_take_photo -> {
                        onTakePhotoClick()
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_remove_avatar -> {
                        saveAvatar(null)
                        return@setOnMenuItemClickListener true
                    }
                    else -> return@setOnMenuItemClickListener false
                }
            }
        }
                .show()
    }

    private fun onChooseFromGalleryClick() {
        if (PermissionsRequester.requestFileReadPermissionIfNeeded(
                        this, REQUEST_PERMISSION_GALLERY))
            chooseFromGallery()
    }

    private fun onTakePhotoClick() {
        if (PermissionsRequester.requestCameraPermissionIfNeeded(
                        this, REQUEST_PERMISSION_CAMERA))
            takePhoto()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val isPermissionsGranted = PermissionsRequester.isPermissionGranted(grantResults)

        when (requestCode) {
            REQUEST_PERMISSION_GALLERY ->
                if (isPermissionsGranted) chooseFromGallery()
                else Toast
                        .makeText(this, R.string.no_permission_to_read_files, Toast.LENGTH_SHORT)
                        .show()

            REQUEST_PERMISSION_CAMERA ->
                if (isPermissionsGranted) takePhoto()
                else Toast
                        .makeText(this, R.string.no_permission_to_camera, Toast.LENGTH_SHORT)
                        .show()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun saveAvatar(newAvatarImageUri: Uri?) {
        showProgressBar(true)
        val userAvatarManager = UserAvatarManager
                .getInstanceFor(AccountManager.getInstance().getAccount(accountJid)?.connection)
        if (newAvatarImageUri == null) {
            try {
                if (userAvatarManager.isSupportedByServer) {
                    //saving empty avatar
                    AvatarManager.getInstance().onAvatarReceived(accountJid!!.fullJid.asBareJid(), "", null, "xep") //todo this
                }
            } catch (e: Exception) {
                LogManager.exception(LOG_TAG, e)
                showProgressBar(false)
            }
        } else {
            try {
                if (userAvatarManager.isSupportedByServer) { //check if server supports PEP, if true - proceed with saving the avatar as XEP-0084 one
                    //xep-0084 av
//                    avatarData = VCard.getBytes(URL(newAvatarImageUri.toString()))
//                    val sh1 = AvatarManager.getAvatarHash(avatarData)
//                    AvatarManager.getInstance().onAvatarReceived(accountJid?.fullJid?.asBareJid(), sh1, avatarData, "xep") //todo this
                }
            } catch (e: Exception) {
                LogManager.exception(LOG_TAG, e)
                showProgressBar(false)
            }
        }
        Application.getInstance().runInBackgroundUserRequest {
            if (newAvatarImageUri == null) {
                try {
                    //publishing empty (avatar) metadata
                    GroupMemberManager.getInstance().removeMemberAvatar(groupchat,
                            groupMember?.id)
                    onAvatarSettingEnded(true)
                } catch (e: Exception) {
                    onAvatarSettingEnded(false)
                    LogManager.exception(LOG_TAG, e)
                }
            } else if (avatarData != null) {
                try {
                    GroupMemberManager.getInstance().publishMemberAvatar(groupchat,
                            groupMember?.id, avatarData, AccountActivity.FINAL_IMAGE_SIZE,
                            AccountActivity.FINAL_IMAGE_SIZE, imageFileType)
                    onAvatarSettingEnded(true)
                } catch (e: Exception) {
                    onAvatarSettingEnded(false)
                    LogManager.exception(LOG_TAG, e)
                }

            }
        }
    }

    private fun onAvatarSettingEnded(isSuccessfully: Boolean) =
            Application.getInstance().runOnUiThread {
                if (isSuccessfully)
                    Toast.makeText(baseContext, getString(R.string.avatar_successfully_published), Toast.LENGTH_LONG).show() //todo use resource strings
                else Toast.makeText(baseContext, getString(R.string.avatar_publishing_failed), Toast.LENGTH_LONG).show()
                setupAvatar()
                showProgressBar(false)
            }

    private fun showProgressBar(show: Boolean) {
        findViewById<ImageView>(R.id.ivAvatar).visibility = if (show) View.VISIBLE else View.GONE
        Utils.lockScreenRotation(this, show)
    }

    private fun chooseFromGallery() = Crop.pickImage(this)

    private fun takePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(this.packageManager) != null) {
            var imageFile: File? = null
            try {
                imageFile = FileManager.createTempImageFile(AccountActivity.TEMP_FILE_NAME)
            } catch (e: IOException) {
                LogManager.exception(this, e)
            }
            if (imageFile != null) {
                filePhotoUri = FileManager.getFileUri(imageFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, filePhotoUri)
                startActivityForResult(takePictureIntent, AccountInfoEditFragment.REQUEST_TAKE_PHOTO)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK) {
            //picked gallery
            data?.data?.let { beginCropProcess(it) }
        } else if (requestCode == AccountInfoEditFragment.REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            //picked camera
            filePhotoUri?.let { beginCropProcess(it) }
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            //processing data after initial crop with CropImage
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                newAvatarImageUri = result.uri
                handleCrop(resultCode)
            }
        } else if (requestCode == Crop.REQUEST_CROP) {
            //processing data after initial crop with Crop
            handleCrop(resultCode)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleCrop(resultCode: Int) {
        when (resultCode) {
            RESULT_OK -> checkAvatarSizeAndPublish()
            else -> {
                Toast.makeText(this, R.string.error_during_crop, Toast.LENGTH_SHORT).show()
                newAvatarImageUri = null
            }
        }
    }

    private fun beginCropProcess(source: Uri) {
        newAvatarImageUri = Uri.fromFile(File(this.cacheDir, AccountActivity.TEMP_FILE_NAME))
        Application.getInstance().runInBackgroundUserRequest {
            val isImageNeedPreprocess = (FileManager.isImageSizeGreater(source, 256)
                    || FileManager.isImageNeedRotation(source))
            Application.getInstance().runOnUiThread {
                if (isImageNeedPreprocess) {
                    preprocessAndStartCrop(source)
                } else {
                    startCrop(source)
                }
            }
        }
    }

    private fun preprocessAndStartCrop(source: Uri) {
        Glide.with(this).asBitmap().load(source).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
                .into(object : CustomTarget<Bitmap?>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                        Application.getInstance().runInBackgroundUserRequest {
                            val cR = Application.getInstance().applicationContext.contentResolver
                            val imageType = cR.getType(source)
                            val stream = ByteArrayOutputStream()
                            if (imageType == "image/png") {
                                resource.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            } else {
                                resource.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                            }
                            val data = stream.toByteArray()
                            resource.recycle()
                            try {
                                stream.close()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                            val rotatedImage: Uri?
                            rotatedImage = if (imageType == "image/png") {
                                FileManager.savePNGImage(data, AccountActivity.ROTATE_FILE_NAME)
                            } else {
                                FileManager.saveImage(data, AccountActivity.ROTATE_FILE_NAME)
                            }
                            if (rotatedImage == null) return@runInBackgroundUserRequest
                            Application.getInstance().runOnUiThread { startCrop(rotatedImage) }
                        }
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        Toast.makeText(baseContext, R.string.error_during_image_processing, Toast.LENGTH_SHORT).show()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
    }

    private fun startCrop(srcUri: Uri) {
        val cR = Application.getInstance().applicationContext.contentResolver
        imageFileType = when (cR.getType(srcUri)) {
            "image/png" -> UserAvatarManager.ImageType.PNG
            "image/jpeg" -> UserAvatarManager.ImageType.JPEG
            else -> null
        }
        if (imageFileType != null) {
            if (imageFileType == UserAvatarManager.ImageType.PNG) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    CropImage.activity(srcUri).setAspectRatio(1, 1)
                            .setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                            .setOutputUri(newAvatarImageUri)
                            .start(this)
                } else Crop.of(srcUri, newAvatarImageUri)
                        .asSquare()
                        .start(this)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    CropImage.activity(srcUri).setAspectRatio(1, 1)
                            .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                            .setOutputUri(newAvatarImageUri)
                            .start(this)
                } else Crop.of(srcUri, newAvatarImageUri)
                        .asSquare()
                        .start(this)
            }
        }
    }

    private fun checkAvatarSizeAndPublish() {
        if (newAvatarImageUri != null) {
            val file = File(newAvatarImageUri?.path)
            if (file.length() / AccountActivity.KB_SIZE_IN_BYTES > 35) {
                resize(newAvatarImageUri as Uri)
                return
            }
            AccountActivity.FINAL_IMAGE_SIZE = AccountActivity.MAX_IMAGE_RESIZE
            AccountActivity.MAX_IMAGE_RESIZE = 256
            saveAvatar(newAvatarImageUri)
        }
    }

    private fun resize(src: Uri) {
        Glide.with(this).asBitmap().load(src).override(AccountActivity.MAX_IMAGE_RESIZE, AccountActivity.MAX_IMAGE_RESIZE)
                .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
                .into(object : CustomTarget<Bitmap?>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                        Application.getInstance().runInBackgroundUserRequest {
                            val stream = ByteArrayOutputStream()
                            if (imageFileType != null) {
                                if (imageFileType == UserAvatarManager.ImageType.PNG) {
                                    resource.compress(Bitmap.CompressFormat.PNG, 90, stream)
                                } else {
                                    resource.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                                }
                            }
                            val data = stream.toByteArray()
                            if (data.size > 35 * AccountActivity.KB_SIZE_IN_BYTES) {
                                AccountActivity.MAX_IMAGE_RESIZE = AccountActivity.MAX_IMAGE_RESIZE - AccountActivity.MAX_IMAGE_RESIZE / 8
                                if (AccountActivity.MAX_IMAGE_RESIZE == 0) {
                                    Toast.makeText(baseContext, R.string.error_during_image_processing, Toast.LENGTH_LONG).show()
                                    return@runInBackgroundUserRequest
                                }
                                resize(src)
                                return@runInBackgroundUserRequest
                            }
                            resource.recycle()
                            try {
                                stream.close()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                            var rotatedImage: Uri? = null
                            if (imageFileType != null) {
                                rotatedImage = if (imageFileType == UserAvatarManager.ImageType.PNG) {
                                    FileManager.savePNGImage(data, "resize")
                                } else {
                                    FileManager.saveImage(data, "resize")
                                }
                            }
                            if (rotatedImage == null) return@runInBackgroundUserRequest
                            try {
                                FileUtils.writeByteArrayToFile(File(newAvatarImageUri?.path), data)
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                            Application.getInstance().runOnUiThread { checkAvatarSizeAndPublish() }
                        }
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        Toast.makeText(baseContext, R.string.error_during_image_processing, Toast.LENGTH_SHORT).show()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
    }

    private fun appBarResize() {
        val avatar = findViewById<ImageView>(R.id.ivAvatar)
        val avatarQR = findViewById<ImageView>(R.id.ivAvatarQR)
        if (avatar.drawable == null) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                avatar.visibility = View.GONE
                avatarQR.visibility = View.GONE
            } else {
                avatarQR.visibility = View.VISIBLE
            }
        }
    }

    private fun orientationPortrait() {
        collapsingToolbar = findViewById<View>(R.id.collapsing_toolbar) as CollapsingToolbarLayout
        appBarLayout = findViewById<View>(R.id.appbar) as AppBarLayout
        appBarLayout?.addOnOffsetChangedListener(object : OnOffsetChangedListener {
            var isShow = true
            var scrollRange = -1
            override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.totalScrollRange
                }
                if (scrollRange + verticalOffset == 0) {
                    //todo collapsingToolbar?.setTitle(bestContact!!.name)
                    contactTitleView!!.visibility = View.INVISIBLE
                    isShow = true
                } else if (isShow) {
                    collapsingToolbar?.title = " "
                    contactTitleView!!.visibility = View.VISIBLE
                    isShow = false
                }
            }
        })
        collapsingToolbar?.setContentScrimColor(accountMainColor)
    }

    private fun orientationLandscape() {
        val nameHolderView = findViewById<View>(R.id.name_holder) as LinearLayout
        toolbar!!.title = ""
        toolbar!!.setBackgroundColor(Color.TRANSPARENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val win = window
            win.statusBarColor = accountMainColor
        }
        if (toolbar!!.overflowIcon != null) toolbar!!.overflowIcon!!.setColorFilter(Color.BLACK,
                PorterDuff.Mode.SRC_ATOP)
        val scrollView = findViewById<NestedScrollView>(R.id.scroll_v_card)
        val ll = findViewById<LinearLayout>(R.id.scroll_v_card_child)
        nameHolderView.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    nameHolderView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                } else nameHolderView.viewTreeObserver.removeGlobalOnLayoutListener(this)
                val topPadding = nameHolderView.height
                ll.setPadding(0, topPadding, 0, 0)
            }
        })
    }

    private fun setupDirectChatButtonLayout(color: Int, orientation: Int) {
        val imageButton = findViewById<ImageButton>(R.id.first_button)
        val textView = findViewById<TextView>(R.id.first_button_text)

        imageButton.setOnClickListener {
            if (groupchat!!.privacyType != GroupPrivacyType.INCOGNITO
                    && !groupMember!!.jid.isNullOrEmpty()) {
                val contactJid = ContactJid.from(groupMember!!.jid)
                startActivityForResult(ChatActivity.createSpecificChatIntent(this,
                        groupchat!!.account, contactJid), MainActivity.CODE_OPEN_CHAT)
            } else {
                GroupMemberManager.getInstance().createChatWithIncognitoMember(groupchat!!, groupMember)
            }
        }

        imageButton.setColorFilter(if (blocked) resources.getColor(R.color.grey_500) else color)

        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            textView.visibility = View.GONE
        else textView.visibility = View.VISIBLE
    }

    private fun setupMessagesButtonLayout(color: Int, orientation: Int) {
        val imageButton = findViewById<ImageButton>(R.id.second_button)
        val textView = findViewById<TextView>(R.id.second_button_text)

        imageButton.isEnabled = !blocked

        imageButton.setColorFilter(color)

        imageButton.setOnClickListener {
            //todo this
        }

        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            textView.visibility = View.GONE
        else textView.visibility = View.VISIBLE
    }

    private fun setupSetBadgeLayout(color: Int, orientation: Int) {
        val imageButton = findViewById<ImageButton>(R.id.third_button)
        val textView = findViewById<TextView>(R.id.third_button_text)

        imageButton?.setOnClickListener {
            val adb = AlertDialog.Builder(this)
            adb.setTitle(groupMember?.nickname + " " + getString(R.string.groupchat_member_badge).decapitalize())

            val et = AppCompatEditText(this)
            et.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            et.isSingleLine = true
            et.hint = groupMember?.badge
            adb.setView(et, 64, 0, 64, 0)

            adb.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            adb.setPositiveButton(R.string.groupchat_set_member_badge) { _, _ ->
                GroupMemberManager.getInstance()
                        .sendSetMemberBadgeIqRequest(groupchat, groupMember, et.text.toString())
            }
            adb.show()
        }

        imageButton!!.setColorFilter(if (blocked) resources.getColor(R.color.grey_500) else color)

        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            textView!!.visibility = View.GONE
        else textView!!.visibility = View.VISIBLE
    }

    private fun setupKickBlockButtonLayout(color: Int, orientation: Int) {
        val imageButton = findViewById<ImageButton>(R.id.fourth_button)
        val textView = findViewById<TextView>(R.id.fourth_button_text)

        imageButton.setOnClickListener { showKickBlockDialog() }

        textView.setText(if (blocked) R.string.contact_bar_unblock else R.string.contact_bar_block)
        textView.setTextColor(resources.getColor(
                if (blocked || coloredBlockText) R.color.red_900
                else if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
                    R.color.grey_600
                else R.color.grey_400))

        imageButton.setColorFilter(resources.getColor(R.color.red_900))
        imageButton.isEnabled = !blocked

        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            textView.visibility = View.GONE
        else textView.visibility = View.VISIBLE
    }

    private fun showKickBlockDialog(){
        AlertDialog.Builder(this).create().apply {
            title = context.getString(R.string.groupchat_kick_member)
            setMessage(context.getString(R.string.groupchat_do_you_really_want_to_kick_membername, groupMember?.nickname))
            setButton(AlertDialog.BUTTON_POSITIVE,
                    "                                 " + context.getString(R.string.groupchat_kick))
                    { _, _ -> GroupMemberManager.getInstance().kickMember(groupMember, groupchat,
                            object: BaseIqResultUiListener {
                                override fun onSend() {}
                                override fun onIqError(error: XMPPError) {
                                    Application.getInstance().runOnUiThread {
                                        if (error.condition == XMPPError.Condition.not_allowed){
                                            Toast.makeText(context,
                                                    context.getString(R.string.groupchat_you_have_no_permissions_to_do_it),
                                                    Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context,
                                                    context.getString(R.string.groupchat_error),
                                                    Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                                override fun onOtherError(exception: Exception?) {
                                    Application.getInstance().runOnUiThread {
                                        Toast.makeText(context,
                                                context.getString(R.string.groupchat_error),
                                                Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onResult() {}
                            })
            }
            setButton(AlertDialog.BUTTON_NEGATIVE,
                    "                                 " + context.getString(R.string.groupchat_kick_and_block))
                    { _, _ -> GroupMemberManager.getInstance().kickAndBlockMember(groupMember, groupchat,
                            object: BaseIqResultUiListener {
                                override fun onSend() {}

                                override fun onIqError(error: XMPPError) {
                                    Application.getInstance().runOnUiThread {
                                        if (error.condition == XMPPError.Condition.not_allowed){
                                            Toast.makeText(context,
                                                    context.getString(R.string.groupchat_you_have_no_permissions_to_do_it),
                                                    Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context,
                                                    context.getString(R.string.groupchat_error),
                                                    Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                                override fun onOtherError(exception: Exception?) {
                                    Application.getInstance().runOnUiThread {
                                        Toast.makeText(context,
                                                context.getString(R.string.groupchat_error),
                                                Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onResult() {}
                            })
            }
            setButton(AlertDialog.BUTTON_NEUTRAL,
                    "                                  " + context.getString(R.string.cancel))
                    { _, _ -> cancel() }
            show()
        }
    }

    private fun setupContactBar(color: Int, orientation: Int) {
        setupDirectChatButtonLayout(color, orientation)
        setupMessagesButtonLayout(color, orientation)
        setupSetBadgeLayout(color, orientation)
        setupKickBlockButtonLayout(color, orientation)

        val contactBarLayout =
                findViewById<ContactBarAutoSizingLayout>(R.id.contact_bar_layout)

        contactBarLayout?.setForGroupchatMember()

        if (orientation == Configuration.ORIENTATION_PORTRAIT) contactBarLayout!!.redrawText()
    }

    fun manageAvailableUsernameSpace() {
        if (this.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val view = findViewById<TextView>(R.id.action_add_contact)
            if (view != null) {
                val width: Int
                val bounds = Rect()
                val textPaint: Paint = view.paint
                textPaint.getTextBounds(view.text.toString(), 0, view.text.length, bounds)
                width = bounds.width()
                val nameL = findViewById<LinearLayout>(R.id.name_layout)
                (nameL.layoutParams as LinearLayout.LayoutParams)
                        .setMargins(0, 0, width + 100, 0)
            }
        }
    }

    fun getActionBarSize(): Int {
        val styledAttributes =
                theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))

        val actionBarSize = styledAttributes.getDimension(0, 0f).toInt()
        styledAttributes.recycle()
        return actionBarSize
    }

    override fun onClick(v: View?) {
        TODO("Not yet implemented")
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        TODO("Not yet implemented")
    }

    private fun showToolbarMenu(isVisible: Boolean) {
        toolbar?.menu?.clear()
        if (isVisible) {
            toolbar?.setNavigationIcon(R.drawable.ic_clear_white_24dp)

            toolbar?.inflateMenu(R.menu.update_groupchat_member)
            toolbar?.setOnMenuItemClickListener {
                (supportFragmentManager.findFragmentByTag(GroupMemberRightsFragment.TAG)
                        as GroupMemberRightsFragment).sendSaveRequest()
                true
            }
        } else {
            toolbar?.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp)
            toolbar?.setNavigationOnClickListener { finish() }
        }
    }

    fun onNewMemberRightsFormFieldChanged(count: Int) = Application.getInstance().runOnUiThread {
        showToolbarMenu(count > 0)
    }

}