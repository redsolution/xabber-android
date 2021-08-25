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
import androidx.core.content.res.ResourcesCompat
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
import com.xabber.android.data.database.realmobjects.GroupMemberRealmObject
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.avatar.AvatarManager
import com.xabber.android.data.extension.file.FileManager
import com.xabber.android.data.extension.groups.GroupMemberManager
import com.xabber.android.data.extension.groups.GroupPrivacyType
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
import com.xabber.xmpp.groups.block.blocklist.GroupchatBlocklistItemElement
import org.apache.commons.io.FileUtils
import org.jivesoftware.smack.packet.XMPPError
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*

class GroupchatMemberActivity : ManagedActivity(), PopupMenu.OnMenuItemClickListener,
    OnGroupchatRequestListener {

    private lateinit var accountJid: AccountJid
    private lateinit var groupchatJid: ContactJid
    private lateinit var groupMember: GroupMemberRealmObject
    private lateinit var groupchat: GroupChat

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
    private var blocked = false

    companion object {

        private const val GROUPCHAT_MEMBER_ID =
            "com.xabber.android.ui.activity.GroupchatMemberActivity.GROUPCHAT_MEMBER_ID"

        private const val GROUPCHAT_JID =
            "com.xabber.android.ui.activity.GroupchatMemberActivity.GROUPCHAT_JID"

        private const val ACCOUNT_JID =
            "com.xabber.android.ui.activity.GroupchatMemberActivity.ACCOUNT_JID"

        fun createIntentForGroupchatAndMemberId(
            context: Context, groupchatMemberId: String, groupchat: GroupChat
        ) = Intent(context, GroupchatMemberActivity::class.java).apply {
            putExtra(GROUPCHAT_MEMBER_ID, groupchatMemberId)
            putExtra(GROUPCHAT_JID, groupchat.contactJid.toString())
            putExtra(ACCOUNT_JID, groupchat.account.toString())
        }

    }

    override fun onGroupchatMembersReceived(account: AccountJid, groupchatJid: ContactJid) {
        if (account == groupchat.account && groupchatJid == groupchat.contactJid) {
            Application.getInstance().runOnUiThread {
                updateBlockedStatus()
                setupAvatar()
                setupNameBlock()
                setupContactBar(accountMainColor, orientation)
            }
        }
    }

    override fun onMeReceived(accountJid: AccountJid, groupchatJid: ContactJid) {
        Application.getInstance().runOnUiThread {
            updateBlockedStatus()
            setupNameBlock()
            setupContactBar(accountMainColor, orientation)
        }
    }

    override fun onGroupchatMemberUpdated(
        accountJid: AccountJid, groupchatJid: ContactJid,
        groupchatMemberId: String
    ) {
        if (accountJid == this.accountJid
            && groupchatJid == this.groupchatJid
            && groupchatMemberId == this.groupMember.memberId
        ) {
            Application.getInstance().runOnUiThread {
                setupAvatar()
                setupNameBlock()
            }
        }
    }

    private fun updateBlockedStatus() {
        blocked = groupchat.listOfBlockedElements?.any {
            it.itemType == GroupchatBlocklistItemElement.ItemType.id
                    && it.blockedItem == groupMember.memberId
                    || it.itemType == GroupchatBlocklistItemElement.ItemType.jid
                    && it.blockedItem == groupMember.jid
        } ?: false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra(ACCOUNT_JID)?.let { accountJid = AccountJid.from(it) }
        intent.getStringExtra(GROUPCHAT_JID)?.let { groupchatJid = ContactJid.from(it) }
        groupchat = ChatManager.getInstance().getChat(accountJid, groupchatJid) as GroupChat
        intent.getStringExtra(GROUPCHAT_MEMBER_ID)?.let {
            groupMember =
                GroupMemberManager.getGroupMemberById(accountJid, groupchatJid, it) ?: return
        }

        updateBlockedStatus()

        GroupMemberManager.requestGroupchatBlocklistList(
            accountJid,
            groupchatJid,
            {
                Application.getInstance().runOnUiThread {
                    updateBlockedStatus()
                    setupNameBlock()
                    setupContactBar(accountMainColor, orientation)
                }
            },
            { }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
            && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
        ) {
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

        supportFragmentManager.beginTransaction().add(
            R.id.scrollable_container,
            GroupMemberRightsFragment(groupMember, groupchat),
            GroupMemberRightsFragment.TAG
        ).commit()

    }

    private fun setupNameBlock() {
        fun createAndShowEditNameDialog() {
            AlertDialog.Builder(this).apply {
                setTitle(getString(R.string.groupchat_member_nickname))

                val et = AppCompatEditText(this@GroupchatMemberActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    hint = groupMember.nickname
                }

                setView(et, 56, 0, 56, 0)

                setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
                setPositiveButton(R.string.groupchat_set_member_nickname) { _, _ ->
                    GroupMemberManager.sendSetMemberNicknameIqRequest(
                        groupchat,
                        groupMember.memberId,
                        et.text.toString()
                    )
                }
            }.show()
        }

        findViewById<TextView>(R.id.name).apply {
            text = (groupMember.bestName + " " + groupMember.badge)
            setOnClickListener {
                createAndShowEditNameDialog()
            }
        }

        findViewById<TextView>(R.id.address_text).apply {
            if (groupMember.jid != null) {
                text = groupMember.jid
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        if (groupMember.isMe) {
            findViewById<TextView>(R.id.groupchat_member_this_is_you_label).visibility =
                View.VISIBLE
        }

        findViewById<TextView>(R.id.groupchat_member_title).text = when {
            blocked -> getString(R.string.settings_group_member__placeholder_blocked)

            groupMember.subscriptionState == GroupMemberRealmObject.SubscriptionState.none -> getString(
                R.string.settings_group_member__placeholder_not_a_member
            )

            else -> getString(
                R.string.groupchat_member_of_group_name,
                groupMember.role?.toString()?.capitalize(Locale.getDefault()),
                groupchat.privacyType?.getLocalizedString()?.decapitalize(Locale.getDefault()),
                groupchat.name
            )
        }
    }

    private fun setupAvatar() {
        val backgroundSource =
            AvatarManager.getInstance().getGroupMemberAvatar(groupMember, accountJid)
                ?: ResourcesCompat.getDrawable(resources, R.drawable.about_backdrop, theme)

        Glide.with(this)
            .load(backgroundSource)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transform(
                MultiTransformation(
                    CenterCrop(),
                    BlurTransformation(25, 8,  /*this,*/accountMainColor)
                )
            )
            .into(findViewById(R.id.backgroundView))

        findViewById<ImageView>(R.id.ivAvatar).apply {
            setImageDrawable(backgroundSource)
            setOnClickListener { changeAvatar(it as ImageView) }
        }
    }

    override fun onResume() {
        super.onResume()
        //ContactTitleInflater.updateTitle(contactTitleView, this, bestContact, true)
        Application.getInstance().addUIListener(OnGroupchatRequestListener::class.java, this)
        GroupMemberManager.requestGroupchatMemberInfo(groupchat, groupMember.memberId)
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
        }.show()
    }

    private fun onChooseFromGalleryClick() {
        if (PermissionsRequester.requestFileReadPermissionIfNeeded(
                this, REQUEST_PERMISSION_GALLERY
            )
        ) {
            chooseFromGallery()
        }
    }

    private fun onTakePhotoClick() {
        if (PermissionsRequester.requestCameraPermissionIfNeeded(
                this, REQUEST_PERMISSION_CAMERA
            )
        ) {
            takePhoto()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val isPermissionsGranted = PermissionsRequester.isPermissionGranted(grantResults)

        when (requestCode) {
            REQUEST_PERMISSION_GALLERY ->
                if (isPermissionsGranted) {
                    chooseFromGallery()
                } else Toast
                    .makeText(this, R.string.no_permission_to_read_files, Toast.LENGTH_SHORT)
                    .show()

            REQUEST_PERMISSION_CAMERA ->
                if (isPermissionsGranted) {
                    takePhoto()
                } else Toast
                    .makeText(this, R.string.no_permission_to_camera, Toast.LENGTH_SHORT)
                    .show()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun saveAvatar(newAvatarImageUri: Uri?) {
        showProgressBar(true)
        val userAvatarManager =
            UserAvatarManager.getInstanceFor(
                AccountManager.getInstance().getAccount(accountJid)?.connection
            )
        if (newAvatarImageUri == null) {
            try {
                if (userAvatarManager.isSupportedByServer) {
                    //saving empty avatar
                    AvatarManager.getInstance()
                        .onAvatarReceived(
                            accountJid.fullJid.asBareJid(),
                            "",
                            null,
                            "xep"
                        ) //todo this
                }
            } catch (e: Exception) {
                LogManager.exception(this, e)
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
                LogManager.exception(this, e)
                showProgressBar(false)
            }
        }
        Application.getInstance().runInBackgroundUserRequest {
            if (newAvatarImageUri == null) {
                try {
                    //publishing empty (avatar) metadata
                    GroupMemberManager.removeMemberAvatar(groupchat, groupMember.memberId)
                    onAvatarSettingEnded(true)
                } catch (e: Exception) {
                    onAvatarSettingEnded(false)
                    LogManager.exception(this, e)
                }
            } else if (avatarData != null) {
                try {
                    GroupMemberManager.publishMemberAvatar(
                        groupchat,
                        groupMember.memberId,
                        avatarData ?: return@runInBackgroundUserRequest,
                        AccountActivity.FINAL_IMAGE_SIZE,
                        AccountActivity.FINAL_IMAGE_SIZE,
                        imageFileType ?: return@runInBackgroundUserRequest
                    )
                    onAvatarSettingEnded(true)
                } catch (e: Exception) {
                    onAvatarSettingEnded(false)
                    LogManager.exception(this, e)
                }

            }
        }
    }

    private fun onAvatarSettingEnded(isSuccessfully: Boolean) =
        Application.getInstance().runOnUiThread {
            if (isSuccessfully) {
                Toast.makeText(
                    baseContext,
                    getString(R.string.avatar_successfully_published),
                    Toast.LENGTH_LONG
                ).show()
            } else Toast.makeText(
                baseContext,
                getString(R.string.avatar_publishing_failed),
                Toast.LENGTH_LONG
            ).show()
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
            try {
                FileManager.createTempImageFile(AccountActivity.TEMP_FILE_NAME)?.let {
                    filePhotoUri = FileManager.getFileUri(it)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, filePhotoUri)
                    startActivityForResult(
                        takePictureIntent,
                        AccountInfoEditFragment.REQUEST_TAKE_PHOTO
                    )
                }
            } catch (e: IOException) {
                LogManager.exception(this, e)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK ->
                data?.data?.let { beginCropProcess(it) }

            requestCode == AccountInfoEditFragment.REQUEST_TAKE_PHOTO && resultCode == RESULT_OK ->
                filePhotoUri?.let { beginCropProcess(it) }

            requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                val result = CropImage.getActivityResult(data)
                if (resultCode == RESULT_OK) {
                    newAvatarImageUri = result.uri
                    handleCrop(resultCode)
                }
            }

            requestCode == Crop.REQUEST_CROP -> handleCrop(resultCode)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleCrop(resultCode: Int) {
        if (resultCode == RESULT_OK) {
            checkAvatarSizeAndPublish()
        } else {
            Toast.makeText(this, R.string.error_during_crop, Toast.LENGTH_SHORT).show()
            newAvatarImageUri = null
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
        Glide.with(this).asBitmap().load(source).diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap?>?
                ) {
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
                            LogManager.exception(javaClass.simpleName, e)
                        }
                        val rotatedImage: Uri? = if (imageType == "image/png") {
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
                    Toast.makeText(
                        baseContext,
                        R.string.error_during_image_processing,
                        Toast.LENGTH_SHORT
                    ).show()
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
            if (File(newAvatarImageUri?.path ?: return).length()
                / AccountActivity.KB_SIZE_IN_BYTES > 35
            ) {
                resize(newAvatarImageUri as Uri)
                return
            }
            AccountActivity.FINAL_IMAGE_SIZE = AccountActivity.MAX_IMAGE_RESIZE
            AccountActivity.MAX_IMAGE_RESIZE = 256
            saveAvatar(newAvatarImageUri)
        }
    }

    private fun resize(src: Uri) {
        Glide.with(this).asBitmap().load(src)
            .override(AccountActivity.MAX_IMAGE_RESIZE, AccountActivity.MAX_IMAGE_RESIZE)
            .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap?>?
                ) {
                    Application.getInstance().runInBackgroundUserRequest {
                        val stream = ByteArrayOutputStream()
                        if (imageFileType != null) {
                            if (imageFileType == UserAvatarManager.ImageType.PNG) {
                                resource.compress(Bitmap.CompressFormat.PNG, 90, stream)
                            } else resource.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        }
                        val data = stream.toByteArray()
                        if (data.size > 35 * AccountActivity.KB_SIZE_IN_BYTES) {
                            AccountActivity.MAX_IMAGE_RESIZE =
                                AccountActivity.MAX_IMAGE_RESIZE - AccountActivity.MAX_IMAGE_RESIZE / 8
                            if (AccountActivity.MAX_IMAGE_RESIZE == 0) {
                                Toast.makeText(
                                    baseContext,
                                    R.string.error_during_image_processing,
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                                return@runInBackgroundUserRequest
                            }
                            resize(src)
                            return@runInBackgroundUserRequest
                        }
                        resource.recycle()
                        try {
                            stream.close()
                        } catch (e: IOException) {
                            LogManager.exception(javaClass.simpleName, e)
                        }
                        val rotatedImage: Uri? =
                            if (imageFileType == UserAvatarManager.ImageType.PNG) {
                                FileManager.savePNGImage(data, "resize")
                            } else {
                                FileManager.saveImage(data, "resize")
                            }
                        if (rotatedImage == null) return@runInBackgroundUserRequest
                        try {
                            newAvatarImageUri?.path?.let {
                                FileUtils.writeByteArrayToFile(
                                    File(it),
                                    data
                                )
                            }
                        } catch (e: IOException) {
                            LogManager.exception(javaClass.simpleName, e)
                        }
                        Application.getInstance().runOnUiThread { checkAvatarSizeAndPublish() }
                    }
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    Toast.makeText(
                        baseContext,
                        R.string.error_during_image_processing,
                        Toast.LENGTH_SHORT
                    ).show()
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
                    contactTitleView?.visibility = View.VISIBLE
                    isShow = false
                }
            }
        })
        collapsingToolbar?.setContentScrimColor(accountMainColor)
    }

    private fun orientationLandscape() {
        val nameHolderView = findViewById<View>(R.id.name_holder) as LinearLayout
        toolbar?.title = ""
        toolbar?.setBackgroundColor(Color.TRANSPARENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = accountMainColor
        }
        toolbar?.overflowIcon?.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP)
        nameHolderView.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    nameHolderView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                } else nameHolderView.viewTreeObserver.removeGlobalOnLayoutListener(this)
                findViewById<LinearLayout>(R.id.scroll_v_card_child).setPadding(
                    0,
                    nameHolderView.height,
                    0,
                    0
                )
            }
        })
    }

    private fun setupDirectChatButtonLayout(color: Int, orientation: Int) {
        val imageButton = findViewById<ImageButton>(R.id.first_button)
        val textView = findViewById<TextView>(R.id.first_button_text)

        imageButton.setOnClickListener {
            if (groupchat.privacyType != GroupPrivacyType.INCOGNITO
                && !groupMember.jid.isNullOrEmpty()
            ) {
                startActivityForResult(
                    ChatActivity.createSpecificChatIntent(
                        this, groupchat.account, ContactJid.from(groupMember.jid)
                    ),
                    MainActivity.CODE_OPEN_CHAT
                )
            } else {
                GroupMemberManager.createChatWithIncognitoMember(groupchat, groupMember.memberId)
            }
        }

        imageButton.setColorFilter(if (blocked) resources.getColor(R.color.grey_500) else color)

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            textView.visibility = View.GONE
        } else {
            textView.visibility = View.VISIBLE
        }
    }

    private fun setupMessagesButtonLayout(color: Int, orientation: Int) {
        findViewById<ImageButton>(R.id.second_button).apply {
            this.isEnabled = !blocked
            setColorFilter(color)
            setOnClickListener {
                Toast.makeText(
                    this@GroupchatMemberActivity,
                    "Not implemented yet",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        findViewById<TextView>(R.id.second_button_text).visibility =
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) View.GONE else View.VISIBLE
    }

    private fun setupSetBadgeLayout(color: Int, orientation: Int) {
        val imageButt = findViewById<ImageButton>(R.id.third_button)

        imageButt.setOnClickListener {
            val adb = AlertDialog.Builder(this@GroupchatMemberActivity)
            adb.setTitle(
                groupMember.nickname + " " + getString(R.string.groupchat_member_badge).decapitalize(
                    Locale.getDefault()
                )
            )

            val et = AppCompatEditText(this@GroupchatMemberActivity)
            et.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            et.isSingleLine = true
            et.hint = groupMember.badge
            adb.setView(et, 64, 0, 64, 0)

            adb.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            adb.setPositiveButton(R.string.groupchat_set_member_badge) { _, _ ->
                GroupMemberManager.sendSetMemberBadgeIqRequest(
                    groupchat,
                    groupMember.memberId,
                    et.text.toString()
                )
            }
            adb.show()
        }

        imageButt.setColorFilter(
            if (blocked) resources.getColor(R.color.grey_500) else color
        )

        findViewById<TextView>(R.id.third_button_text).visibility =
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) View.GONE else View.VISIBLE
    }

    private fun unblock() {
        GroupMemberManager.unblockGroupMember(
            accountJid,
            groupchatJid,
            groupMember.memberId,
            object : BaseIqResultUiListener {
                override fun onSend() {}

                override fun onResult() {
                    GroupMemberManager.requestGroupchatMemberInfo(groupchat, groupMember.memberId)
                    GroupMemberManager.requestGroupchatBlocklistList(
                        accountJid,
                        groupchatJid,
                        {
                            Application.getInstance().runOnUiThread {
                                updateBlockedStatus()
                                setupNameBlock()
                                setupContactBar(accountMainColor, orientation)
                            }
                        },
                        { }
                    )
                }

                override fun onIqError(error: XMPPError) {
                    if (error.condition == XMPPError.Condition.not_allowed) {
                        Toast.makeText(
                            this@GroupchatMemberActivity,
                            getString(R.string.groupchat_you_have_no_permissions_to_do_it),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@GroupchatMemberActivity,
                            error.conditionText,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onOtherError(exception: Exception?) {
                    Toast.makeText(
                        this@GroupchatMemberActivity,
                        exception?.stackTraceToString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun block() {
        GroupMemberManager.kickAndBlockMember(groupMember.memberId, groupchat,
            object : BaseIqResultUiListener {
                override fun onSend() {}

                override fun onIqError(error: XMPPError) {
                    Application.getInstance().runOnUiThread {
                        if (error.condition == XMPPError.Condition.not_allowed) {
                            Toast.makeText(
                                this@GroupchatMemberActivity,
                                getString(R.string.groupchat_you_have_no_permissions_to_do_it),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@GroupchatMemberActivity,
                                getString(R.string.groupchat_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onOtherError(exception: Exception?) {
                    Application.getInstance().runOnUiThread {
                        Toast.makeText(
                            this@GroupchatMemberActivity,
                            getString(R.string.groupchat_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResult() {
                    GroupMemberManager.requestGroupchatMemberInfo(groupchat, groupMember.memberId)
                    GroupMemberManager.requestGroupchatBlocklistList(
                        accountJid,
                        groupchatJid,
                        {
                            Application.getInstance().runOnUiThread {
                                updateBlockedStatus()
                                setupNameBlock()
                                setupContactBar(accountMainColor, orientation)
                            }
                        },
                        { }
                    )
                }
            })
    }

    private fun setupKickBlockButtonLayout(orientation: Int) {
        findViewById<ImageButton>(R.id.fourth_button).apply {
            setOnClickListener {
                when {
                    blocked -> unblock()
                    groupMember.subscriptionState == GroupMemberRealmObject.SubscriptionState.none -> block()
                    else -> showKickBlockDialog()
                }
            }

            setColorFilter(resources.getColor(R.color.red_900))
        }

        findViewById<TextView>(R.id.fourth_button_text).apply {
            setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    when {
                        blocked || coloredBlockText -> R.color.red_900
                        SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light -> R.color.grey_600
                        else -> R.color.grey_400
                    },
                    theme
                )
            )

            visibility =
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
        }
    }

    private fun showKickBlockDialog() {
        AlertDialog.Builder(this).create().apply {

            title = context.getString(R.string.groupchat_kick_member)

            setMessage(
                context.getString(
                    R.string.groupchat_do_you_really_want_to_kick_membername,
                    groupMember.nickname
                )
            )

            setButton(
                AlertDialog.BUTTON_POSITIVE,
                "                                 " + context.getString(R.string.groupchat_kick)
            ) { _, _ ->
                GroupMemberManager.kickMember(groupMember.memberId, groupchat,
                    object : BaseIqResultUiListener {
                        override fun onSend() {}
                        override fun onIqError(error: XMPPError) {
                            Application.getInstance().runOnUiThread {
                                if (error.condition == XMPPError.Condition.not_allowed) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.groupchat_you_have_no_permissions_to_do_it),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.groupchat_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }

                        override fun onOtherError(exception: Exception?) {
                            Application.getInstance().runOnUiThread {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.groupchat_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onResult() {
                            Application.getInstance().runOnUiThread {
                                GroupMemberManager.requestGroupchatMemberInfo(
                                    groupchat,
                                    groupMember.memberId
                                )
                            }
                        }
                    })
            }

            setButton(
                AlertDialog.BUTTON_NEGATIVE,
                "                                 " + context.getString(R.string.groupchat_kick_and_block)
            ) { _, _ -> block() }

            setButton(
                AlertDialog.BUTTON_NEUTRAL,
                "                                  " + context.getString(R.string.cancel)
            ) { _, _ -> cancel() }

            show()
        }
    }

    private fun setupContactBar(color: Int, orientation: Int) {
        setupDirectChatButtonLayout(color, orientation)
        setupMessagesButtonLayout(color, orientation)
        setupSetBadgeLayout(color, orientation)
        setupKickBlockButtonLayout(orientation)

        val contactBarLayout =
            findViewById<ContactBarAutoSizingLayout>(R.id.contact_bar_layout)

        contactBarLayout?.setForGroupchatMember(blocked)

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            contactBarLayout?.redrawText()
        }
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