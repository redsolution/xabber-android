package com.xabber.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.xabber.android.R
import com.xabber.android.ui.color.ColorManager

class DiscoverFragment : Fragment(){
    lateinit var toolbar : View
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_discover, container, false)
        toolbar = view.findViewById(R.id.toolbar_discover)
        //toolbar.setBackgroundColor(ColorManager.getInstance().accountPainter.
        return view
    }



    companion object{ fun newInstance() : DiscoverFragment = DiscoverFragment() }


}
