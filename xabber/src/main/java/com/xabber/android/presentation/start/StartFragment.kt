package com.xabber.android.presentation.start

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import by.kirich1409.viewbindingdelegate.viewBinding
import com.xabber.android.R
import com.xabber.android.databinding.FragmentStartBinding
import com.xabber.android.presentation.base.BaseFragment
import com.xabber.android.presentation.base.FragmentTag
import com.xabber.android.presentation.signin.SigninFragment
import com.xabber.android.presentation.signup.SignupFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers

class StartFragment: BaseFragment(R.layout.fragment_start) {

    private val binding by viewBinding(FragmentStartBinding::bind)
    private val viewModel = StartViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            btnLogin.setOnClickListener {
                replace(SigninFragment(), FragmentTag.Signin.toString())
            }
            btnSignup.setOnClickListener {
                progressBar.isVisible = true
                btnLogin.isVisible = false
                btnSignup.isVisible = false
                compositeDisposable.add(
                    viewModel.getHost()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ host ->
                            progressBar.isVisible = false
                            replace(
                                SignupFragment.newInstance(
                                    stepCounter = 1,
                                    host = host.list[0].name
                                ), FragmentTag.Signup1.toString()
                        )
                    }, this@StartFragment::logError))
            }
        }
    }
}