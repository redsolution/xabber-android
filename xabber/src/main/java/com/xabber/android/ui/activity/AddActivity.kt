package com.xabber.android.ui.activity

import android.os.Bundle
import com.xabber.android.R
import com.xabber.android.ui.color.BarPainter
import com.xabber.android.ui.fragment.AddFragment
import com.xabber.android.ui.helper.ToolbarHelper

class AddActivity : ManagedActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.add_activity_layout)

        val toolbar = ToolbarHelper.setUpDefaultToolbar(this,
                getString(R.string.add))
        BarPainter(this, toolbar).setDefaultColor()

        supportFragmentManager.beginTransaction().add(R.id.container, AddFragment()).commit()
    }
}