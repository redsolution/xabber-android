package com.xabber.android.presentation.signin

import android.content.Context
import android.os.Bundle
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.xabber.android.R
import com.xabber.android.databinding.FragmentSigninBinding
import com.xabber.android.presentation.base.BaseFragment
import com.xabber.android.presentation.base.FragmentTag
import com.xabber.android.presentation.main.MainActivity
import com.xabber.android.presentation.signin.feature.FeatureAdapter
import com.xabber.android.presentation.signin.feature.State
import com.xabber.android.presentation.signup.SignupFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val HOST_TAG = "HOST_TAG"

class SigninFragment : BaseFragment(R.layout.fragment_signin) {

    private val binding by viewBinding(FragmentSigninBinding::bind)
    private val viewModel = SigninViewModel()
    private var featureAdapter: FeatureAdapter? = null
    var host: String = "dev.xabber.org"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        compositeDisposable.add(viewModel.getHost()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ host ->
                this.host = host.list[0].name
            }, this@SigninFragment::logError)
        )

        (activity as MainActivity).setToolbarTitle(R.string.signin_toolbar_title_1)
        with(binding) {
            signinSubtitle1.text = getSubtitleClickableSpan()
            signinSubtitle1.movementMethod = LinkMovementMethod.getInstance()
            signinJidEditText.setOnFocusChangeListener { _, hasFocused ->
                when {
                    hasFocused ->
                        signinJidEditText.hint = ""
                    !hasFocused && signinJidEditText.text.isNotEmpty() ->
                        signinJidEditText.hint = ""
                    else ->
                        signinJidEditText.hint =
                            resources.getString(R.string.signin_edit_text_jid_label)
                }
            }
            signinPasswordEditText.setOnFocusChangeListener { _, hasFocused ->
                when {
                    hasFocused ->
                        signinPasswordEditText.hint = ""
                    !hasFocused && signinPasswordEditText.text.isNotEmpty() ->
                        signinPasswordEditText.hint = ""
                    else ->
                        signinPasswordEditText.hint =
                            resources.getString(R.string.signin_edit_text_password_label)
                }
            }

            signinJidEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun afterTextChanged(p0: Editable?) {
                    var jidText = p0.toString()
                    if (!jidText.contains('@'))
                        jidText += "@$host"
                    btnConnect.isEnabled =
                        viewModel.isJidValid(jidText) && signinPasswordEditText.text.length > 5
                    signinSubtitle1.setTextColor(
                        ResourcesCompat.getColor(
                            resources,
                            R.color.grey_600,
                            requireContext().theme
                        )
                    )
                    signinSubtitle1.text = getSubtitleClickableSpan()
                    signinSubtitle1.movementMethod = LinkMovementMethod.getInstance()
                }
            })
            signinPasswordEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun afterTextChanged(p0: Editable?) {
                    var jidText = signinJidEditText.text.toString()
                    if (!jidText.contains('@'))
                        jidText += "@$host"
                    btnConnect.isEnabled = p0.toString().length > 5 && viewModel.isJidValid(jidText)
                    signinSubtitle1.setTextColor(
                        ResourcesCompat.getColor(
                            resources,
                            R.color.grey_600,
                            requireContext().theme
                        )
                    )
                    signinSubtitle1.text = getSubtitleClickableSpan()
                    signinSubtitle1.movementMethod = LinkMovementMethod.getInstance()
                }
            })
            signinPasswordEditText.setOnEditorActionListener { _, i, _ ->
                if (i == EditorInfo.IME_ACTION_DONE) {
                    btnConnect.performClick()
                    closeKeyboard()
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }
            with(rvFeature) {
                adapter = FeatureAdapter()
                    .also { featureAdapter = it }
                featureAdapter?.submitList(listOf())
            }
            btnConnect.setOnClickListener {
                btnConnect.isEnabled = false
                if (viewModel.isJidValid(signinJidEditText.text.toString()) || signinPasswordEditText.text.length > 5) {
                    compositeDisposable.add(viewModel.features
                        .doOnNext { list ->
                            if (list.filter { it.nameResId == R.string.feature_name_4 }
                                    .count() == 1) {
                                (activity as MainActivity).setToolbarTitle(R.string.signin_toolbar_title_2)
                                (activity as MainActivity).showToolbarBackButton(false)
                                signinTitle.text = String.format(
                                    resources.getString(R.string.signin_title_label_template_2),
                                    host
                                )
                                signinJidEditText.visibility = View.GONE
                                signinPasswordEditText.visibility = View.GONE
                                signinSubtitle1.isVisible = false
                                btnConnect.isVisible = false
                            }
                            if (list.all { it.state != State.Error } || viewModel.isServerFeatures) {
                                featureAdapter?.submitList(list)
                                featureAdapter?.notifyItemChanged(list.lastIndex)
                            }
                            lifecycleScope.launch {
                                delay(300)
                                list[list.lastIndex].state =
//                                if ((0..3).random() > 1)
                                    State.Success
//                                else
//                                    State.Error
                                if (viewModel.isServerFeatures) {
                                    featureAdapter?.submitList(list)
                                    featureAdapter?.notifyItemChanged(list.lastIndex)
                                }
                                if (list.filter { it.nameResId == R.string.feature_name_10 }
                                        .count() == 1) {
                                    signinSubtitle2.isVisible = true
                                    btnRock.isVisible = true
                                    btnRock.setOnClickListener {
                                        Toast.makeText(
                                            requireContext(),
                                            resources.getString(R.string.feature_not_created),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                if (viewModel._features.filter { it.state == State.Error }
                                        .count() <= 1 &&
                                    viewModel._features[list.lastIndex].state == State.Error
                                ) {
                                    featureAdapter?.submitList(list)
                                    featureAdapter?.notifyItemChanged(list.lastIndex)
                                }
                                if (viewModel._features[list.lastIndex].state == State.Success &&
                                    viewModel._features.filter { it.state == State.Error }
                                        .count() == 0
                                ) {
                                    featureAdapter?.submitList(list)
                                    featureAdapter?.notifyItemChanged(list.lastIndex)
                                }
                            }
                        }
                        .subscribe({}, {})
                    )
                } else {
                    signinSubtitle1.setTextColor(
                        ResourcesCompat.getColor(
                            resources,
                            R.color.red_600,
                            requireContext().theme
                        )
                    )
                    signinSubtitle1.text =
                        resources.getString(R.string.signin_subtitle_error_message)
                }
            }
        }
    }

    private fun closeKeyboard() {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            binding.signinPasswordEditText.windowToken,
            0
        )
    }

    private fun getSubtitleClickableSpan(): Spannable {
        val spannable =
            SpannableStringBuilder(resources.getString(R.string.signin_subtitle_label_1))
        spannable.setSpan(
            ForegroundColorSpan(
                ResourcesCompat.getColor(
                    resources,
                    R.color.blue_600,
                    requireContext().theme
                )
            ),
            34,
            44,
            Spannable.SPAN_EXCLUSIVE_INCLUSIVE
        )
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(p0: View) {
                    parentFragmentManager.popBackStack()
                    replace(
                        SignupFragment.newInstance(
                            stepCounter = 1,
                            host = host
                        ), FragmentTag.Signup1.toString()
                    )
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            },
            34,
            44,
            Spannable.SPAN_EXCLUSIVE_INCLUSIVE
        )
        return spannable
    }

    override fun onDestroy() {
        featureAdapter = null
        super.onDestroy()
    }
}