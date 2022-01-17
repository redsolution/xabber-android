package com.xabber.android.presentation.start

import android.os.Bundle
import android.view.View
import android.widget.Toast
import by.kirich1409.viewbindingdelegate.viewBinding
import com.xabber.android.R
import com.xabber.android.databinding.FragmentStartBinding
import com.xabber.android.presentation.base.BaseFragment
import com.xabber.android.presentation.base.FragmentTag
import com.xabber.android.presentation.main.MainActivity
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
                Toast.makeText(
                    requireContext(),
                    resources.getString(R.string.feature_not_created)
                    , Toast.LENGTH_SHORT
                ).show()
            }
            btnSignup.setOnClickListener {
                (activity as MainActivity).setProgressBarAnimation(true)
                compositeDisposable.add(viewModel.getHost()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ host ->
                        (activity as MainActivity).setProgressBarAnimation(false)
                        add(SignupFragment.newInstance(stepCounter = 1, host = host.list[0].name), FragmentTag.Signup1.toString())
                        hide(this@StartFragment)
                    }, this@StartFragment::logError))
            }
        }
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }
}