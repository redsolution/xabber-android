package com.xabber.android.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.http.Place

class FoundPlacesRecyclerViewAdapter(
    var placesList: List<Place> = listOf()
): RecyclerView.Adapter<FoundPlacesRecyclerViewAdapter.PlaceVH>() {

    override fun onBindViewHolder(holder: PlaceVH, position: Int) {
        holder.itemView.findViewById<TextView>(android.R.id.text1).text =
            placesList[position].displayName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceVH {
        return PlaceVH(
            LayoutInflater.from(parent.context).inflate(
                android.R.layout.activity_list_item, parent, false
            ).apply {
                setBackgroundColor(resources.getColor(R.color.white))
            }
        )
    }

    override fun getItemCount(): Int = placesList.size

    class PlaceVH(itemView: View): RecyclerView.ViewHolder(itemView)
}