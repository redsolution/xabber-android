package com.xabber.android.presentation.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.soundcloud.android.crop.Crop
import com.theartofdev.edmodo.cropper.CropImage
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.extension.avatar.AvatarManager
import com.xabber.android.data.extension.file.FileManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.databinding.ActivityMainNewBinding
import com.xabber.android.presentation.base.BaseFragment
import com.xabber.android.presentation.base.FragmentTag
import com.xabber.android.presentation.signup.SignupFragment
import com.xabber.android.presentation.start.StartFragment
import com.xabber.android.presentation.toolbar.ToolbarFragment
import com.xabber.android.ui.activity.AccountActivity
import com.xabber.android.ui.fragment.AccountInfoEditFragment.REQUEST_TAKE_PHOTO
import com.xabber.android.ui.helper.PermissionsRequester
import com.xabber.android.ui.helper.PermissionsRequester.*
import com.xabber.xmpp.avatar.UserAvatarManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.FileUtils
import org.jivesoftware.smackx.vcardtemp.packet.VCard
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*
import kotlin.concurrent.schedule

// TODO("Переименовать xml после избавления от легаси")
// TODO("Сделать переворот без потери состояния")
class MainActivity : AppCompatActivity(R.layout.activity_main_new) {

    private val binding by viewBinding(ActivityMainNewBinding::bind)

    private var filePhotoUri: Uri? = null
    private var newAvatarImageUri: Uri? = null

//    var accountJid: AccountJid? = null
    lateinit var imageFileType: String

    private var avatarData: ByteArray? = null

    private val TEMP_FILE_NAME = "cropped"

    private val KB_SIZE_IN_BYTES: Int = 1024
    private var FINAL_IMAGE_SIZE: Int = 0
    private var MAX_IMAGE_RESIZE: Int = 256

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        initStartupFragment()
        setToolbar(ToolbarFragment())
        supportFragmentManager.addOnBackStackChangedListener {
            var fragmentContent = supportFragmentManager.findFragmentById(R.id.content_container)
            val fragmentToolbar = supportFragmentManager.findFragmentById(R.id.toolbar_container)

            if (fragmentContent != null){
                if (fragmentContent is SignupFragment)
                    fragmentContent.closeKeyboard()
            }

            binding.toolbarContainer.isVisible = supportFragmentManager.backStackEntryCount > 0

            fragmentContent = supportFragmentManager.findFragmentByTag(FragmentTag.Signup4.toString())

            if (fragmentContent != null)
                (fragmentToolbar as ToolbarFragment).showSkipButton(true)
            else
                (fragmentToolbar as ToolbarFragment).showSkipButton(false)
        }
    }

    override fun onBackPressed() {
        var fragment: Fragment?
        when (supportFragmentManager.backStackEntryCount) {
            0 -> finish()
            1 -> {
                fragment = supportFragmentManager.findFragmentByTag(FragmentTag.Start.toString())
                if (fragment != null && fragment.isHidden)
                    showFragment(fragment)
            }
            else -> {
                binding.toolbarContainer.visibility = View.VISIBLE
                fragment = supportFragmentManager.findFragmentByTag(FragmentTag.Signup3.toString())
                if (fragment != null && fragment.isHidden) {
                    showFragment(fragment)
                } else {
                    fragment = supportFragmentManager.findFragmentByTag(FragmentTag.Signup2.toString())
                    if (fragment != null && fragment.isHidden) {
                        showFragment(fragment)
                    }
                    else {
                        fragment = supportFragmentManager.findFragmentByTag(FragmentTag.Signup1.toString())
                        if (fragment != null && fragment.isHidden)
                            showFragment(fragment)
                    }
                }
            }
        }
        super.onBackPressed()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            REQUEST_PERMISSION_CAMERA -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)
                    takePhoto()

            }
            REQUEST_PERMISSION_GALLERY -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED)
                    chooseFromGallery()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        LogManager.d("onActivityResult", requestCode.toString())
        when {
            requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK ->
                data?.data?.let { beginCropProcess(it) }

            requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK ->
                filePhotoUri?.let { beginCropProcess(it) }

            requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                val result = CropImage.getActivityResult(data)
                if (resultCode == RESULT_OK) {
                    val fragment = supportFragmentManager.findFragmentByTag(FragmentTag.Signup4.toString())
                    (fragment as SignupFragment).setAvatar(result.uri)
                    newAvatarImageUri = result.uri
                    handleCrop(resultCode, data!!)
                }
            }

            requestCode == Crop.REQUEST_CROP -> handleCrop(resultCode, data!!)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun AppCompatActivity.initStartupFragment() {
//        supportFragmentManager.beginTransaction()
//            .replace(R.id.content_container, StartFragment(), FragmentTag.Start.toString())
//            .commit()
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_container, SignupFragment.newInstance(4), FragmentTag.Signup4.toString())
            .commit()
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .show(fragment)
            .commit()
    }

    fun setToolbar(fragment: BaseFragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.toolbar_container, fragment)
            .commit()
        binding.toolbarContainer.visibility = View.GONE
    }

    fun setToolbarTitle(titleId: Int) {
        val fragment = supportFragmentManager.findFragmentById(R.id.toolbar_container)
        if (fragment != null)
            (fragment as ToolbarFragment).setToolbarTitle(titleId)
    }

    fun popBackStackFragment() {
        onBackPressed()
    }

    fun setProgressBarAnimation(isAnimate: Boolean) {
        binding.progressBar.visibility =
//            if (isAnimate)
//                View.VISIBLE
//            else
                View.GONE
    }

    fun onTakePhoto() {
        if (requestCameraPermissionIfNeeded(
                this,
                REQUEST_PERMISSION_CAMERA
            ) && requestFileReadPermissionIfNeeded(
                this,
                REQUEST_READ_EXTERNAL_STORAGE
            )
        )
            takePhoto()
    }

    private fun takePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            FileManager.createTempImageFile(TEMP_FILE_NAME)?.let {
                filePhotoUri = FileManager.getFileUri(it)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, filePhotoUri)
                startActivityForResult(takePictureIntent,
                    REQUEST_TAKE_PHOTO
                )
            }
        } catch (e: IOException) {
            LogManager.e(this, e.toString())
        }
    }

    fun onChooseFromGallery() {
        if (requestFileReadPermissionIfNeeded(
                this,
                REQUEST_PERMISSION_GALLERY
            ) && requestFileReadPermissionIfNeeded(
                this,
                REQUEST_READ_EXTERNAL_STORAGE
            )
        )
            chooseFromGallery()
    }

    private fun chooseFromGallery() {
        Crop.pickImage(this)
    }

    private fun beginCropProcess(source: Uri) {
        newAvatarImageUri = Uri.fromFile(File(this.cacheDir, TEMP_FILE_NAME))
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
                        val imageFileType = cR.getType(source)
                        val stream = ByteArrayOutputStream()
                        if (imageFileType == "image/png") {
                            resource.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        } else {
                            resource.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                        }
                        val data = stream.toByteArray()
                        resource.recycle()
                        try {
                            stream.close()
                        } catch (e: IOException) {
                            LogManager.e("preprocessAndStartCrop", e.toString())
                        }
                        val rotatedImage: Uri? = if (imageFileType == "image/png") {
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
        imageFileType = cR.getType(srcUri)!!
        if (imageFileType == "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                CropImage.activity(srcUri).setAspectRatio(1, 1)
                    .setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                    .setOutputUri(newAvatarImageUri)
                    .start(this)
            else Crop.of(srcUri, newAvatarImageUri)
                .asSquare()
                .start(this)
        else
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                CropImage.activity(srcUri).setAspectRatio(1, 1)
                    .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                    .setOutputUri(newAvatarImageUri)
                    .start(this)
            else
                Crop.of(srcUri, newAvatarImageUri)
                .asSquare()
                .start(this)
    }

    private fun handleCrop(resultCode: Int, result: Intent) {
        when (resultCode) {
            RESULT_OK -> checkAvatarSizeAndPublish()
            Crop.RESULT_ERROR -> {
                Toast.makeText(this, R.string.error_during_crop, Toast.LENGTH_SHORT).show()
                newAvatarImageUri = null
            }
        }
    }

    private fun checkAvatarSizeAndPublish() {
        if (newAvatarImageUri != null) {
            val file = File(newAvatarImageUri!!.path!!)
            if (file.length() / KB_SIZE_IN_BYTES > 35) {
                Toast.makeText(
                    this,
                    "Image is too big, commencing additional processing!",
                    Toast.LENGTH_LONG
                ).show()
                resize(newAvatarImageUri!!)
                return
            }
            Toast.makeText(this, "Started Avatar Publishing!", Toast.LENGTH_LONG).show()

            LogManager.d("checkAvatarSizeAndPublish", newAvatarImageUri.toString())
            FINAL_IMAGE_SIZE = MAX_IMAGE_RESIZE
            MAX_IMAGE_RESIZE = 256
            saveAvatar()
        }
    }

    private fun resize(src: Uri) {
        LogManager.d("resize", src.toString())
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
                        if (imageFileType == "image/png") {
                            resource.compress(Bitmap.CompressFormat.PNG, 90, stream)
                        } else resource.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        val data = stream.toByteArray()
                        if (data.size > 35 * KB_SIZE_IN_BYTES) {
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
                            LogManager.e("resize", e.toString())
                        }
                        val rotatedImage: Uri? =
                            if (imageFileType == "image/png") {
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
                            LogManager.e("resize", e.toString())
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

    private fun saveAvatar() {
//        val userAvatarManager =
//            UserAvatarManager.getInstanceFor(AccountManager.getAccount(accountJid)?.connection)
//        if (newAvatarImageUri != null) {
//            try {
//                if (userAvatarManager.isSupportedByServer) {
//                    avatarData = VCard.getBytes(URL(newAvatarImageUri.toString()))
//                    val sh1 = AvatarManager.getAvatarHash(avatarData)
//                    AvatarManager.getInstance()
//                        .onAvatarReceived(accountJid!!.fullJid.asBareJid(), sh1, avatarData, "xep")
//                }
//            } catch (e: Exception) {
//                LogManager.exception(this, e)
//            }
//        }
        Application.getInstance().runInBackgroundUserRequest {
            if (avatarData != null) {
                try {
//                    if (imageFileType == "image/png") {
//                        userAvatarManager.publishAvatar(
//                            avatarData,
//                            AccountActivity.FINAL_IMAGE_SIZE,
//                            AccountActivity.FINAL_IMAGE_SIZE
//                        )
//                    } else userAvatarManager.publishAvatarJPG(
//                        avatarData,
//                        AccountActivity.FINAL_IMAGE_SIZE,
//                        AccountActivity.FINAL_IMAGE_SIZE
//                    )
                    Application.getInstance().runOnUiThread {
                        Toast.makeText(baseContext, "Avatar published!", Toast.LENGTH_LONG)
                            .show()
                    }
                } catch (e: Exception) {
                    LogManager.exception(this, e)
                    Application.getInstance().runOnUiThread {
                        Toast.makeText(
                            baseContext,
                            "Avatar publishing failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}