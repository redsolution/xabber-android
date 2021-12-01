package com.xabber.android.ui.helper

import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider

/**
 * Default GpsMyLocationProvider that provided by OsmDroid has no opportunity to send
 * LocationManager updates to external observers, but we need these updates to draw UI
 * that corresponding to location status, so we extends it.
 * And yep, it's not right way to notify listeners based on this data.
 * TODO rewrite to retrieve proper location status data
 */
class ObservableOsmLocationProvider(context: Context): GpsMyLocationProvider(context) {
    private val _stateLiveData = MutableLiveData<LocationState>()
    val stateLiveData: LiveData<LocationState> get() = _stateLiveData

    override fun onLocationChanged(location: Location) {
        _stateLiveData.value = LocationState.LocationReceived
        super.onLocationChanged(location)
    }

    override fun onProviderDisabled(provider: String) {
        super.onProviderDisabled(provider)
        _stateLiveData.value = LocationState.LocationNotFound
    }

    enum class LocationState {
        LocationReceived, LocationNotFound
    }
}