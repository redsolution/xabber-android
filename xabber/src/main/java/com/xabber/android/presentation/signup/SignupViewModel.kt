package com.xabber.android.presentation.signup

import androidx.lifecycle.ViewModel
import com.xabber.android.data.dto.HostDto
import com.xabber.android.data.dto.HostListDto
import com.xabber.android.data.dto.XabberAccountDto
import com.xabber.android.data.repository.AccountRepository
import com.xabber.android.util.AppConstants
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class SignupViewModel : ViewModel() {

    val accountRepository = AccountRepository()

    fun checkIfNameAvailable(username: String, host: String): Single<Any> =
        accountRepository.checkIfNameAvailable(mapOf(
            "username" to username,
            "host" to host,
            "no_captcha_key" to AppConstants.NO_CAPTCHA_KEY,
        ))

    fun registerAccount(username: String, host: String, password: String): Single<XabberAccountDto> =
        accountRepository.registerAccount(mapOf(
            "username" to username,
            "host" to host,
            "password" to password,
            "no_captcha_key" to AppConstants.NO_CAPTCHA_KEY
        ))
}