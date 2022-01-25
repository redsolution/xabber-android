package com.xabber.android.presentation.start

import android.os.Bundle
import android.view.View
import by.kirich1409.viewbindingdelegate.viewBinding
import com.xabber.android.R
import com.xabber.android.databinding.FragmentStartBinding
import com.xabber.android.presentation.base.BaseFragment
import com.xabber.android.presentation.base.FragmentTag
import com.xabber.android.presentation.main.MainActivity
import com.xabber.android.presentation.signin.HOST_TAG
import com.xabber.android.presentation.signin.SigninFragment
import com.xabber.android.presentation.signup.SignupFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class StartFragment: BaseFragment(R.layout.fragment_start) {

    private val binding by viewBinding(FragmentStartBinding::bind)
    private val viewModel = StartViewModel()
    private val compositeDisposable = CompositeDisposable()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            btnLogin.setOnClickListener {
                (activity as MainActivity).setProgressBarAnimation(true)
                compositeDisposable.add(viewModel.getHost()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ host ->
                        (activity as MainActivity).setProgressBarAnimation(false)
                        val fragment = SigninFragment()
                        val args = Bundle()
                        args.putString(HOST_TAG, host.list[0].name)
                        fragment.arguments = args
                        replace(fragment, FragmentTag.Signin.toString())
                    }, this@StartFragment::logError)
                )
            }
            btnSignup.setOnClickListener {
                (activity as MainActivity).setProgressBarAnimation(true)
                compositeDisposable.add(viewModel.getHost()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ host ->
                        (activity as MainActivity).setProgressBarAnimation(false)
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

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }
}