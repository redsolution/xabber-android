package com.xabber.android.ui.activity

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import com.xabber.android.R
import com.xabber.android.ui.color.BarPainter
import com.xabber.android.ui.fragment.AddFragment

class AddActivity : ManagedActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.add_activity)
        setupToolbar()
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, AddFragment()).commit()
    }

    private fun setupToolbar(){
        val toolbar = findViewById<Toolbar>(R.id.toolbar_default)
        BarPainter(this, toolbar).setDefaultColor()

        findViewById<ImageView>(R.id.toolbar_arrow_back_iv).setOnClickListener {
            NavUtils.navigateUpFromSameTask(this)
        }
        setTitle(findViewById<TextView>(R.id.tvTitle))
    }

    private fun setTitle(title: TextView){
        val oneliners = getString(R.string.motivating_oneliner).split("\n")
        title.text = oneliners.random().trim()
        title.post {
            if (title.lineCount > 1){
                setTitle(title)
            }
        }
    }

}