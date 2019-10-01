package com.xabber.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.xabber.android.R

class CallsFragment : Fragment(){

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    = inflater.inflate(R.layout.fragment_calls, container, false)

    companion object{ fun newInstance() : CallsFragment = CallsFragment() }
}