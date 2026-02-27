package com.xabber.android.presentation.signup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.extension.avatar.AvatarManager
import com.xabber.android.data.extension.file.FileManager
import com.xabber.android.data.log.LogManager
import com.xabber.android.databinding.FragmentSignupBinding
import com.xabber.android.presentation.avatar.AvatarBottomSheet
import com.xabber.android.presentation.base.APP_FM_BACKSTACK
import com.xabber.android.presentation.base.BaseFragment
import com.xabber.android.presentation.base.FragmentTag
import com.xabber.android.presentation.main.MainActivity
import com.xabber.android.ui.activity.AccountActivity
import com.xabber.android.util.AppConstants.TEMP_FILE_NAME
import com.xabber.android.util.dp
import com.xabber.xmpp.avatar.UserAvatarManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.*
import org.apache.commons.io.FileUtils
import org.jivesoftware.smackx.vcardtemp.packet.VCard
import org.jxmpp.jid.impl.JidCreate.domainBareFrom
import org.jxmpp.jid.parts.Domainpart
import org.jxmpp.jid.parts.Localpart
import org.jxmpp.jid.parts.Resourcepart
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URL
import kotlin.properties.Delegates


class SignupFragment : BaseFragment(R.layout.fragment_signup), OnKeyboardVisibilityListener {

    private val binding by viewBinding(FragmentSignupBinding::bind)

    private var stepCounter by Delegates.notNull<Int>()
    private var username by Delegates.notNull<String>()
    private var host by Delegates.notNull<String>()
    private var password by Delegates.notNull<String>()
    private val viewModel = SignupViewModel()
    var accountJid: AccountJid? = null

    private val newAvatarImageUri: Uri by lazy {
        File(requireContext().cacheDir, TEMP_FILE_NAME).toUri()
    }

    private var avatarData: ByteArray? = null
    private var imageFileType: String? = null

    private val KB_SIZE_IN_BYTES: Int = 1024
    private var FINAL_IMAGE_SIZE: Int = 0
    private var MAX_IMAGE_RESIZE: Int = 256

    override fun onVisibilityChanged(visible: Boolean) {
        if (visible)
            binding.signupEditText.requestFocus()
        else
            binding.signupEditText.clearFocus()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stepCounter = requireArguments().getInt(STEP_COUNTER_TAG)
        username = requireArguments().getString(USERNAME_TAG) ?: ""
        host = requireArguments().getString(HOST_TAG) ?: ""
        password = requireArguments().getString(PASSWORD_TAG) ?: ""
        accountJid = requireArguments().getParcelable(ACCOUNT_JID_TAG)
        when (stepCounter) {
            1 -> (activity as MainActivity).setToolbarTitle(R.string.signup_toolbar_title_1)
            2 -> (activity as MainActivity).setToolbarTitle(R.string.signup_toolbar_title_2)
            3 -> (activity as MainActivity).setToolbarTitle(R.string.signup_toolbar_title_3)
            4 -> (activity as MainActivity).setToolbarTitle(R.string.signup_toolbar_title_4)
        }

        with(binding) {
            changeSubtitleColor(R.color.grey_600)

            signupEditText.setOnFocusChangeListener { _, hasFocused ->
                when  {
                    hasFocused ->
                        signupEditText.hint = ""
                    !hasFocused && signupEditText.text.isNotEmpty() ->
                        signupEditText.hint = ""
                    else ->
                        signupEditText.hint = resources.getString(
                            when (stepCounter) {
                                1 -> R.string.signup_edit_text_label_1
                                2 -> R.string.signup_edit_text_label_2
                                else -> R.string.signup_edit_text_label_3
                            }
                        )
                }
            }
            signupEditText.setOnEditorActionListener { _, i, _ ->
                if (i == EditorInfo.IME_ACTION_DONE) {
                    btnNext.performClick()
                    closeKeyboard()
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }
            when (stepCounter) {
                1 -> {
                    signupEditText.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            p0: CharSequence?,
                            p1: Int,
                            p2: Int,
                            p3: Int
                        ) {
                        }

                        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                        override fun afterTextChanged(p0: Editable?) {
                            val name: String = p0.toString()
                            signupEditText.hint = if (name.isEmpty())
                                resources.getString(R.string.signup_edit_text_label_1)
                            else
                                ""
                            btnNext.isEnabled = name.length > 1
                        }
                    })

                    btnNext.setOnClickListener {
                        closeKeyboard()
                        replace(
                            newInstance(2, signupEditText.text.toString(), host),
                            FragmentTag.Signup2.toString()
                        )
//                        hide(this@SignupFragment)
                    }
                }
                2 -> {
                    signupTitle.text = resources.getString(R.string.signup_title_label_2)
                    signupEditText.hint = resources.getString(R.string.signup_edit_text_label_2)
                    signupSubtitle.text = resources.getString(R.string.signup_subtitle_2)

                    signupEditText.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                        override fun afterTextChanged(p0: Editable?) {

                            btnNext.isEnabled = false
                            signupSubtitle.text = resources.getString(R.string.signup_subtitle_2)
                            changeSubtitleColor(R.color.grey_600)

                            signupEditText.removeTextChangedListener(this)
                            signupEditText.setText(p0.toString().lowercase().replace(' ', '.'))
                            signupEditText.addTextChangedListener(this)
                            signupEditText.setSelection(signupEditText.text.length)

                            if (p0.toString() != signupEditText.text.toString()) {
                                signupSubtitle.text =
                                    resources.getString(R.string.signup_error_subtitle_2)
                                changeSubtitleColor(R.color.red_600)
                                btnNext.isEnabled = false
                            }

                            signupEditText.hint = if (p0.toString().isEmpty())
                                resources.getString(R.string.signup_edit_text_label_2)
                            else
                                ""
                            if (p0.toString().length > 3) {
                                compositeDisposable.clear()
                                compositeDisposable.add(
                                    viewModel.checkIfNameAvailable(p0.toString().trimStart(), host)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .doAfterSuccess {
                                            username = p0.toString().trimStart()
                                            signupSubtitle.text =
                                                resources.getString(R.string.signup_success_subtitle_2)
                                            changeSubtitleColor(R.color.blue_600)
                                            btnNext.isEnabled = true
                                        }
                                        .doOnDispose {
                                            btnNext.isEnabled = false
                                            signupSubtitle.text =
                                                resources.getString(R.string.signup_subtitle_2)
                                            changeSubtitleColor(R.color.grey_600)
                                        }
                                        .subscribe({}, {
                                            signupSubtitle.text =
                                                resources.getString(R.string.signup_error_subtitle_2)
                                            changeSubtitleColor(R.color.red_600)
                                            btnNext.isEnabled = false
                                            logError(it)
                                        })
                                )
                            } else {
                                btnNext.isEnabled = false
                                signupSubtitle.text = resources.getString(R.string.signup_subtitle_2)
                                changeSubtitleColor(R.color.grey_600)
                            }
                        }
                    })

                    btnNext.setOnClickListener {
                        closeKeyboard()
                        replace(newInstance(3, username, host), FragmentTag.Signup3.toString())
                    }
                }
                3 -> {
                    signupTitle.text = resources.getString(R.string.signup_title_label_3)
                    signupEditText.hint = resources.getString(R.string.signup_edit_text_label_3)
                    signupEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                    signupSubtitle.text = resources.getString(R.string.signup_subtitle_3)

                    signupEditText.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                        override fun afterTextChanged(p0: Editable?) {
                            password = p0.toString()
                            signupEditText.hint = if (password.isEmpty())
                                resources.getString(R.string.signup_edit_text_label_3)
                            else
                                ""
                            btnNext.isEnabled = password.length > 5
                        }
                    })

                    btnNext.setOnClickListener {
                        progressBar.isVisible = true
                        btnNext.isEnabled = false
                        btnNext.text = ""
                        compositeDisposable.clear()
                        compositeDisposable.add(
                            viewModel.registerAccount(username, host, password)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .doAfterSuccess {
                                    accountJid = AccountJid.from(
                                        Localpart.from(it.username),
                                        domainBareFrom(
                                            Domainpart.from(it.domain)
                                        ),
                                        Resourcepart.EMPTY
                                    )
                                    closeKeyboard()
                                    clearBackstack()
                                    clearBackstack(null)
                                    replace(
                                        newInstance(4, username, host),
                                        FragmentTag.Signup4.toString(),
                                        APP_FM_BACKSTACK
                                    )
                                }
                                .doOnDispose {
                                    progressBar.isVisible = false
                                    btnNext.isEnabled = true
                                    btnNext.text = resources.getString(R.string.signup_next_button)
                                }
                                .subscribe({}, {
                                    logError(it)
                                })
                        )
                    }
                }
                4 -> {
                    signupTitle.text = resources.getString(R.string.signup_title_label_4)
                    signupEditText.visibility = View.GONE
                    signupSubtitle.visibility = View.GONE
                    btnNext.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        verticalBias = 1.0f
                    }
                    profileImageBackground.visibility = View.VISIBLE
                    profilePhotoBackground.visibility = View.VISIBLE

                    profileImageBackground.setOnClickListener {
                        AvatarBottomSheet().show(parentFragmentManager, null)
                    }
                    profilePhotoBackground.setOnClickListener {
                        AvatarBottomSheet().show(parentFragmentManager, null)
                    }

                    btnNext.setOnClickListener {
//                        checkAvatarSizeAndPublish()
                        Toast.makeText(
                            requireContext(),
                            resources.getString(R.string.feature_not_created), Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        setKeyboardVisibilityListener(this@SignupFragment)
    }

    private fun setKeyboardVisibilityListener(onKeyboardVisibilityListener: OnKeyboardVisibilityListener) {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            private var alreadyOpen = false
            private val defaultKeyboardHeightDP = 100
            private val EstimatedKeyboardDP =
                defaultKeyboardHeightDP + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) 48 else 0
            private val rect: Rect = Rect()


            override fun onGlobalLayout() {
                lifecycleScope.launch(Dispatchers.IO) {
                    while (true) {
                        if (view != null) {
                            withContext(Dispatchers.Main) {
                                val estimatedKeyboardHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                    EstimatedKeyboardDP.toFloat(), binding.root.resources.displayMetrics)
                                binding.root.getWindowVisibleDisplayFrame(rect)
                                val heightDiff = binding.root.rootView.height - (rect.bottom - rect.top)
                                val isShown = heightDiff >= estimatedKeyboardHeight

                                if (isShown == alreadyOpen) {
                                    LogManager.i("Keyboard state", "Ignoring global layout change...")
                                    this@launch.cancel()
                                }
                                alreadyOpen = isShown
                                onKeyboardVisibilityListener.onVisibilityChanged(isShown)
                                this@launch.cancel()
                            }
                        }
                        delay(1000 / 60)
                    }
                }
            }
        })
    }

    fun setAvatar(uri: Uri?) {
        with(binding) {
            Glide.with(this@SignupFragment)
                .load(uri)
                .apply(
                    RequestOptions()
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .dontAnimate())
                .into(profileImageEmoji)
            btnNext.isEnabled = true
        }
    }

    fun setAvatar(bitmap: Bitmap) {
        with(binding) {
            profileImageEmoji.setPadding(0.dp)
            Glide.with(this@SignupFragment)
                .load(bitmap)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.ic_baseline_insert_emoticon_24)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .dontAnimate())
                .into(profileImageEmoji)
            btnNext.isEnabled = true
        }
    }

    fun closeKeyboard() {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            binding.signupEditText.windowToken,
            0
        )
    }

    private fun changeSubtitleColor(@ColorRes colorId: Int) {
        binding.signupSubtitle.setTextColor(
            ResourcesCompat.getColor(
                resources,
                colorId,
                requireContext().theme
            )
        )
    }

    private fun checkAvatarSizeAndPublish() {
        val file = File(newAvatarImageUri.path!!)
        if (file.length() / KB_SIZE_IN_BYTES > 35) {
            Toast.makeText(
                requireContext(),
                "Image is too big, commencing additional processing!",
                Toast.LENGTH_LONG
            ).show()
            resize(newAvatarImageUri)
            return
        }
        Toast.makeText(requireContext(), "Started Avatar Publishing!", Toast.LENGTH_LONG).show()

        FINAL_IMAGE_SIZE = MAX_IMAGE_RESIZE
        MAX_IMAGE_RESIZE = 256
        saveAvatar()
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
                        val cR = Application.getInstance().applicationContext.contentResolver
                        imageFileType = cR.getType(newAvatarImageUri)
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
                                    requireContext(),
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
                            newAvatarImageUri.path?.let {
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
                        requireContext(),
                        R.string.error_during_image_processing,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun saveAvatar() {
        val userAvatarManager =
            UserAvatarManager.getInstanceFor(AccountManager.getAccount(accountJid)?.connection)
        try {
            if (userAvatarManager.isSupportedByServer) {
                avatarData = VCard.getBytes(URL(newAvatarImageUri.toString()))
                val sh1 = AvatarManager.getAvatarHash(avatarData)
                AvatarManager.getInstance()
                    .onAvatarReceived(accountJid!!.fullJid.asBareJid(), sh1, avatarData, "xep")
            }
        } catch (e: Exception) {
            LogManager.exception(this, e)
        }
        Application.getInstance().runInBackgroundUserRequest {
            if (avatarData != null) {
                try {
                    if (imageFileType == "image/png") {
                        userAvatarManager.publishAvatar(
                            avatarData,
                            AccountActivity.FINAL_IMAGE_SIZE,
                            AccountActivity.FINAL_IMAGE_SIZE
                        )
                    } else userAvatarManager.publishAvatarJPG(
                        avatarData,
                        AccountActivity.FINAL_IMAGE_SIZE,
                        AccountActivity.FINAL_IMAGE_SIZE
                    )
                    Application.getInstance().runOnUiThread {
                        Toast.makeText(requireContext(), "Avatar published!", Toast.LENGTH_LONG)
                            .show()
                    }
                } catch (e: Exception) {
                    LogManager.exception(this, e)
                    Application.getInstance().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Avatar publishing failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    companion object {

        private const val STEP_COUNTER_TAG = "STEP_COUNTER_TAG"
        private const val USERNAME_TAG = "USERNAME_TAG"
        private const val HOST_TAG = "HOST_TAG"
        private const val PASSWORD_TAG = "PASSWORD_TAG"
        private const val ACCOUNT_JID_TAG = "ACCOUNT_JID_TAG"

        @JvmStatic
        fun newInstance(
            stepCounter: Int = 1,
            username: String = "",
            host: String = "",
            password: String = "",
            jid: AccountJid? = null
        ): SignupFragment {
            val args = Bundle()
            val fragment = SignupFragment()

            args.putInt(STEP_COUNTER_TAG, stepCounter)
            args.putString(USERNAME_TAG, username)
            args.putString(HOST_TAG, host)
            args.putString(PASSWORD_TAG, password)
            args.putParcelable(ACCOUNT_JID_TAG, jid)

            fragment.arguments = args
            return fragment
        }
    }
}

interface OnKeyboardVisibilityListener {
    fun onVisibilityChanged(visible: Boolean)
}