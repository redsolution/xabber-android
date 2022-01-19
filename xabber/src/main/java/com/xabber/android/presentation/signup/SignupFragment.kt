package com.xabber.android.presentation.signup

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.xabber.android.R
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.log.LogManager
import com.xabber.android.databinding.FragmentSignupBinding
import com.xabber.android.presentation.avatar.AvatarBottomSheet
import com.xabber.android.presentation.base.BaseFragment
import com.xabber.android.presentation.base.FragmentTag
import com.xabber.android.presentation.main.MainActivity
import com.xabber.android.presentation.toolbar.ToolbarFragment
import com.xabber.android.util.dp
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.*
import org.jxmpp.jid.parts.Resourcepart
import retrofit2.HttpException
import kotlin.properties.Delegates


class SignupFragment : BaseFragment(R.layout.fragment_signup), OnKeyboardVisibilityListener {

    private val binding by viewBinding(FragmentSignupBinding::bind)

    private var stepCounter by Delegates.notNull<Int>()
    private var username by Delegates.notNull<String>()
    private var host by Delegates.notNull<String>()
    private var password by Delegates.notNull<String>()
    private val compositeDisposable = CompositeDisposable()
    private val viewModel = SignupViewModel()

    override fun onVisibilityChanged(visible: Boolean) {
        if (visible)
            binding.signupEditText.requestFocus()
        else
            binding.signupEditText.clearFocus()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setKeyboardVisibilityListener(this@SignupFragment)

        stepCounter = requireArguments().getInt(STEP_COUNTER_TAG)
        username = requireArguments().getString(USERNAME_TAG) ?: ""
        host = requireArguments().getString(HOST_TAG) ?: ""
        password = requireArguments().getString(PASSWORD_TAG) ?: ""
        when (stepCounter) {
            1 -> (activity as MainActivity).setToolbarTitle(R.string.signup_toolbar_title_1)
            2 -> (activity as MainActivity).setToolbarTitle(R.string.signup_toolbar_title_2)
            3 -> (activity as MainActivity).setToolbarTitle(R.string.signup_toolbar_title_3)
            4 -> (activity as MainActivity).setToolbarTitle(R.string.signup_toolbar_title_4)
        }
    }

    override fun onStart() {
        super.onStart()

        with(binding) {
            changeSubtitleColor(R.color.grey_600)

            when (stepCounter) {
                1 -> {
                    signupEditText.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                        override fun afterTextChanged(p0: Editable?) {
                            val name: String = p0.toString()
                            btnNext.isEnabled = name.length > 1
                        }
                    })

                    btnNext.setOnClickListener {
                        closeKeyboard()
                        add(newInstance(2, signupEditText.text.toString(), host), FragmentTag.Signup2.toString())
                        hide(this@SignupFragment)
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
                            compositeDisposable.clear()
                            val username: String = p0.toString()
                            if (username.length > 3) {
                                (activity as MainActivity).setProgressBarAnimation(true)
                                compositeDisposable.add(
                                    viewModel.checkIfNameAvailable(username, host)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .doFinally {
                                            (activity as MainActivity).setProgressBarAnimation(false)
                                        }
                                        .subscribe({
                                            signupSubtitle.text = resources.getString(R.string.signup_success_subtitle_2)
                                            changeSubtitleColor(R.color.blue_600)
                                            btnNext.isEnabled = true
                                        }, {
                                            signupSubtitle.text = resources.getString(R.string.signup_error_subtitle_2)
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
                        add(newInstance(3, username, host), FragmentTag.Signup3.toString())
                        hide(this@SignupFragment)
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
                            btnNext.isEnabled = password.length > 4
                        }
                    })

                    btnNext.setOnClickListener {
//                        (activity as MainActivity).setProgressBarAnimation(true)
//                        compositeDisposable.add(
//                            viewModel.registerAccount(username, host, password)
//                                .subscribeOn(Schedulers.io())
//                                .observeOn(AndroidSchedulers.mainThread())
//                                .doFinally {
//                                    (activity as MainActivity).setProgressBarAnimation(false)
//                                }
//                                .subscribe({
//                                    (activity as MainActivity).accountJid =
//                                        AccountJid.from(it.username, it.domain, Resourcepart.EMPTY)
//                                    closeKeyboard()
//                                }, {
//                                    logError(it)
//                                })
//                        )
                        add(newInstance(4, username, host), FragmentTag.Signup4.toString())
                        hide(this@SignupFragment)
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

                    // clearBackstack<SignupFragment>()
                }
            }
        }
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
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
            profileImageEmoji.setPadding(0.dp)
            Glide.with(this@SignupFragment)
                .load(uri)
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
        binding.signupSubtitle.setTextColor(ResourcesCompat.getColor(resources, colorId, requireContext().theme))
    }

    companion object {

        private const val STEP_COUNTER_TAG = "STEP_COUNTER_TAG"
        private const val USERNAME_TAG = "USERNAME_TAG"
        private const val HOST_TAG = "HOST_TAG"
        private const val PASSWORD_TAG = "PASSWORD_TAG"

        @JvmStatic
        fun newInstance(
            stepCounter: Int = 1,
            username: String = "",
            host: String = "",
            password: String = ""
        ): SignupFragment {
            val args = Bundle()
            val fragment = SignupFragment()

            args.putInt(STEP_COUNTER_TAG, stepCounter)
            args.putString(USERNAME_TAG, username)
            args.putString(HOST_TAG, host)
            args.putString(PASSWORD_TAG, password)

            fragment.arguments = args
            return fragment
        }
    }
}

interface OnKeyboardVisibilityListener {
    fun onVisibilityChanged(visible: Boolean)
}