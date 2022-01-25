package com.xabber.android.presentation.toolbar

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import by.kirich1409.viewbindingdelegate.viewBinding
import com.xabber.android.R
import com.xabber.android.databinding.FragmentToolbarBinding
import com.xabber.android.presentation.base.BaseFragment
import com.xabber.android.presentation.main.MainActivity

class ToolbarFragment: BaseFragment(R.layout.fragment_toolbar) {

    private val binding by viewBinding(FragmentToolbarBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            (activity as MainActivity).popBackStackFragment()
        }

        binding.btnSkip.setOnClickListener {
            Toast.makeText(
                requireContext(),
                resources.getString(R.string.feature_not_created), Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun showBackButton(flag: Boolean) {
        binding.btnBack.isVisible = flag
    }

    fun setToolbarTitle(titleId: Int) {
        binding.toolbarTitleTextview.text = resources.getString(titleId)
    }

    fun showSkipButton(flag: Boolean) {
        binding.btnSkip.isVisible = flag
    }
}