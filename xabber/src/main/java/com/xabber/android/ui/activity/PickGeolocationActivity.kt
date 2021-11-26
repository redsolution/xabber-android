package com.xabber.android.ui.activity

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xabber.android.R
import com.xabber.android.data.createAccountIntent
import com.xabber.android.data.entity.AccountJid

class PickGeolocationActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_with_toolbar_progress_and_container)
        super.onCreate(savedInstanceState)
    }

    companion object {
        fun createIntent(context: Context, accountJid: AccountJid) =
            createAccountIntent(context, PickGeolocationActivity::class.java, accountJid)
    }
}