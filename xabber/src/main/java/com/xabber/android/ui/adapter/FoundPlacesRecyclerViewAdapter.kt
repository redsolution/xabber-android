package com.xabber.android.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.http.Place
import com.xabber.android.data.http.prettyName

class FoundPlacesRecyclerViewAdapter(
    var placesList: List<Place> = listOf(), val onPlaceClickListener: (Place) -> Unit,
): RecyclerView.Adapter<FoundPlacesRecyclerViewAdapter.PlaceVH>() {

    override fun onBindViewHolder(holder: PlaceVH, position: Int) {
        holder.itemView.findViewById<TextView>(R.id.text).apply {
            val place = placesList[position]
            text = place.prettyName
            setOnClickListener {
                onPlaceClickListener(place)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceVH {
        return PlaceVH(
            LayoutInflater.from(parent.context).inflate(
                R.layout.found_places_item, parent, false
            ).apply {
                setBackgroundColor(resources.getColor(R.color.white))
            }
        )
    }

    override fun getItemCount(): Int = placesList.size

    class PlaceVH(itemView: View): RecyclerView.ViewHolder(itemView)
}