package com.xabber.android.presentation.base

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.xabber.android.R
import com.xabber.android.data.log.LogManager
import io.reactivex.rxjava3.disposables.CompositeDisposable

const val APP_FM_BACKSTACK = "APP_FM_BACKSTACK"
const val APP_FM_BACKSTACK_NONE = "APP_FM_BACKSTACK_NONE"
const val CONTENT_CONTAINER_ID = R.id.content_container

enum class FragmentTag(fragmentTag: String) {
    Toolbar("Toolbar"),
    Start("Start"),
    Signup1("SignUp_1"),
    Signup2("SignUp_2"),
    Signup3("SignUp_3"),
    Signup4("SignUp_4"),
    Signin("Signin"),
}

abstract class BaseFragment(@LayoutRes contentLayoutId: Int) : Fragment(contentLayoutId) {

    protected val compositeDisposable = CompositeDisposable()

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

//    protected fun <T : Fragment> add(
//        fragment: T,
//        tag: String? = null,
//        backstackTag: String? = APP_FM_BACKSTACK,
//        containerId: Int = CONTENT_CONTAINER_ID
//    ) = parentFragmentManager.beginTransaction()
//        .add(containerId, fragment, tag)
//        .addToBackStack(backstackTag)
//        .commit()
//
//    protected fun <T : Fragment> hide(
//        fragment: T
//    ) = parentFragmentManager.beginTransaction()
//        .hide(fragment)
//        .commit()

    protected fun <T : Fragment> replace(
        fragment: T,
        tag: String? = null,
        backstackTag: String? = APP_FM_BACKSTACK,
        containerId: Int = CONTENT_CONTAINER_ID
    ) {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(containerId, fragment, tag)
        if (backstackTag != APP_FM_BACKSTACK_NONE)
            transaction.addToBackStack(backstackTag)
        transaction.commit()
    }

    protected fun clearBackstack(backstackTag: String? = APP_FM_BACKSTACK) {
        for (i: Int in 0..parentFragmentManager.backStackEntryCount)
            parentFragmentManager.popBackStack(
                backstackTag,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
    }

    protected fun <T : Fragment> remove(fragment: T, backstackTag: String? = APP_FM_BACKSTACK) {
        parentFragmentManager.beginTransaction()
            .remove(fragment)
            .commit()
        parentFragmentManager.popBackStackImmediate(
            backstackTag,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
    }

    protected fun logError(e: Throwable) {
        // Toast.makeText(context, "Произошла ошибка", Toast.LENGTH_SHORT).show()
        LogManager.e("ERR", e.stackTraceToString())
    }
}