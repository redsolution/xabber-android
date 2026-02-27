package com.xabber.android.presentation.signin.feature

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.xabber.android.databinding.ItemFeatureBinding

class FeatureAdapter : ListAdapter<Feature, FeatureViewHolder>(DiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFeatureBinding.inflate(inflater, parent, false)
        return FeatureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) =
        holder.bind(getItem(position))
}

private object DiffUtilCallback : DiffUtil.ItemCallback<Feature>() {

    override fun areItemsTheSame(oldItem: Feature, newItem: Feature) =
        oldItem.nameResId == newItem.nameResId && oldItem.state == newItem.state

    override fun areContentsTheSame(oldItem: Feature, newItem: Feature) =
        oldItem.nameResId == newItem.nameResId && oldItem.state == newItem.state
}