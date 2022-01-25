package com.xabber.android.presentation.signin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.xabber.android.R
import com.xabber.android.databinding.FragmentSigninBinding
import com.xabber.android.presentation.base.BaseFragment
import com.xabber.android.presentation.main.MainActivity
import com.xabber.android.presentation.signin.feature.FeatureAdapter
import com.xabber.android.presentation.signin.feature.State
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val HOST_TAG = "HOST_TAG"

class SigninFragment : BaseFragment(R.layout.fragment_signin) {

    private val binding by viewBinding(FragmentSigninBinding::bind)
    private val viewModel = SigninViewModel()
    private var featureAdapter: FeatureAdapter? = null
    private val compositeDisposable = CompositeDisposable()
    lateinit var host: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        host = requireArguments().getString(HOST_TAG) ?: ""
        if (host.isEmpty())
            remove(this)

        (activity as MainActivity).setToolbarTitle(R.string.signin_toolbar_title_1)
        with(binding) {
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
                    signinSubtitle1.text = resources.getString(R.string.signin_subtitle_label_1)
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
                    signinSubtitle1.text = resources.getString(R.string.signin_subtitle_label_1)
                }
            })
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
                                signinTitle.text = String.format(
                                    resources.getString(R.string.signin_title_label_template_2),
                                    host
                                )
                                signinJidEditText.isVisible = false
                                signinPasswordEditText.isVisible = false
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
                                    featureAdapter?.notifyDataSetChanged()
                                }
                                if (list.filter { it.nameResId == R.string.feature_name_10 }
                                        .count() == 1) {
                                    divider.isVisible = true
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

    override fun onDestroy() {
        featureAdapter = null
        compositeDisposable.clear()
        super.onDestroy()
    }
}