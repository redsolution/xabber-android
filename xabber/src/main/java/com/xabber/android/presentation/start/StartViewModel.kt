package com.xabber.android.presentation.start

import androidx.lifecycle.ViewModel
import com.xabber.android.data.dto.HostListDto
import com.xabber.android.data.repository.AccountRepository
import io.reactivex.rxjava3.core.Single

class StartViewModel: ViewModel() {

    val accountRepository = AccountRepository()

    fun getHost(): Single<HostListDto> = accountRepository.getHostList()
}