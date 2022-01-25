package com.xabber.android.presentation.signin.feature

import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.databinding.ItemFeatureBinding
import com.xabber.android.presentation.signin.feature.State.*

class FeatureViewHolder(private val binding: ItemFeatureBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: Feature) {
        with(binding) {
            featureName.text = itemView.resources.getString(item.nameResId)

            when (item.state) {
                Loading -> {
                    featureName.setTextColor(
                        ResourcesCompat.getColor(
                            itemView.resources,
                            R.color.grey_400,
                            itemView.context.theme
                        )
                    )
                    binding.featureLoad.visibility = View.VISIBLE
                    binding.featureResult.visibility = View.GONE
                }
                Success -> {
                    featureName.setTextColor(
                        ResourcesCompat.getColor(
                            itemView.resources,
                            R.color.black_text,
                            itemView.context.theme
                        )
                    )
                    featureLoad.visibility = View.GONE
                    featureResult.setBackgroundResource(R.drawable.ic_material_check_circle_24)
                    featureResult.visibility = View.VISIBLE
                }
                Error -> {
                    featureName.setTextColor(
                        ResourcesCompat.getColor(
                            itemView.resources,
                            R.color.black_text,
                            itemView.context.theme
                        )
                    )
                    featureLoad.visibility = View.GONE
                    featureResult.setBackgroundResource(R.drawable.ic_material_alert_circle_24)
                    featureResult.visibility = View.VISIBLE
                }
            }
        }
    }
}