package com.xabber.android.ui.activity

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.TypedValue
import android.view.View
import androidx.annotation.DrawableRes
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.createAccountIntent
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.getAccountJid
import com.xabber.android.data.http.NominatimRetrofitModule
import com.xabber.android.data.http.Place
import com.xabber.android.data.http.prettyName
import com.xabber.android.data.log.LogManager
import com.xabber.android.databinding.PickGeolocationActivityBinding
import com.xabber.android.ui.adapter.FoundPlacesRecyclerViewAdapter
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.color.StatusBarPainter
import com.xabber.android.ui.helper.ObservableOsmLocationProvider
import com.xabber.android.ui.helper.PermissionsRequester
import com.xabber.android.ui.widget.SearchToolbar
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*

class PickGeolocationActivity: ManagedActivity() {

    private lateinit var binding: PickGeolocationActivityBinding

    private var pickMarker: Marker? = null
    private var pointerColor: Int = 0

    private val foundPlacesAdapter = FoundPlacesRecyclerViewAdapter()

    private var myLocationOverlay: MyLocationNewOverlay? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = PickGeolocationActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent?.getAccountJid()?.let { accountJid ->
            pointerColor = ColorManager.getInstance().accountPainter.getAccountColorWithTint(
                accountJid, 500
            )
            binding.pickgeolocationLocationSendButton.setColorFilter(pointerColor)

            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                binding.searchToolbar.color =
                    ColorManager.getInstance().accountPainter.getAccountRippleColor(
                        accountJid
                    )
            } else {
                val typedValue = TypedValue()
                this.theme.resolveAttribute(R.attr.bars_color, typedValue, true)
                binding.searchToolbar.color = typedValue.data
            }

            setStatusBarColor(accountJid)
        }

        binding.searchToolbar.onBackPressedListener = SearchToolbar.OnBackPressedListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        binding.pickgeolocationRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PickGeolocationActivity)
            adapter = foundPlacesAdapter
        }

        binding.searchToolbar.onTextChangedListener = SearchToolbar.OnTextChangedListener { searchString ->
            lifecycleScope.launch {
                binding.pickgeolocationProgressbar.visibility = View.VISIBLE
                val foundPlacesList = NominatimRetrofitModule.api.search(searchString)
                setupSearchList(foundPlacesList)
                binding.pickgeolocationProgressbar.visibility = View.INVISIBLE
            }
        }

        binding.pickgeolocationMyGeolocation.setOnClickListener {
            tryToGetMyLocation()
        }

        binding.pickgeolocationLocationBottomRoot.setOnClickListener {
            /* ignore to avoid interception of clicks by mapview */
        }

        binding.searchToolbar.title = getString(R.string.chat_screen__dialog_title__pick_location)

        setupMap()
        super.onCreate(savedInstanceState)
    }

    private fun setupSearchList(list: List<Place>) {
        if (list.isEmpty()) {
            binding.pickgeolocationRecyclerView.visibility = View.GONE
        } else {
            binding.pickgeolocationRecyclerView.visibility = View.VISIBLE
            foundPlacesAdapter.placesList = list
            foundPlacesAdapter.notifyDataSetChanged()
        }
    }

    private fun tryToGetMyLocation() {
        fun createMyLocationsOverlay() {
            val locationsProvider = ObservableOsmLocationProvider(binding.pickgeolocationMapView.context)
            locationsProvider.stateLiveData.observe(this) { state -> updateMyLocationButton(state) }
            myLocationOverlay = MyLocationNewOverlay(locationsProvider, binding.pickgeolocationMapView)
            myLocationOverlay?.enableMyLocation()
            myLocationOverlay?.isDrawAccuracyEnabled
            binding.pickgeolocationMapView.overlays.add(myLocationOverlay)
        }

        fun centerOnMyLocation() {
            lifecycleScope.launch {
                repeat(15) {
                    if (myLocationOverlay?.myLocation != null) {
                        myLocationOverlay?.enableFollowLocation()
                        binding.pickgeolocationMapView.controller.setZoom(16.5)
                        cancel()
                    }
                    delay(300)
                }
                //todo possible show error while location retrieving
            }
        }

        if (PermissionsRequester.requestLocationPermissionIfNeeded(this, REQUEST_LOCATION_PERMISSION_CODE)) {
            if (myLocationOverlay == null) {
                createMyLocationsOverlay()
            }
            centerOnMyLocation()
        }
    }

    override fun onStop() {
        myLocationOverlay?.disableFollowLocation()
        myLocationOverlay?.disableMyLocation()
        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tryToGetMyLocation()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun setStatusBarColor(accountJid: AccountJid) {
        StatusBarPainter.instanceUpdateWithDefaultColor(this)
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            StatusBarPainter.instanceUpdateWithAccountName(this, accountJid)
            StatusBarPainter.instanceUpdateWithDefaultColor(this)
        } else {
            val typedValue = TypedValue()
            this.theme.resolveAttribute(R.attr.bars_color, typedValue, true)
            StatusBarPainter.instanceUpdateWIthColor(this, typedValue.data)
        }
    }

    private fun setupMap() {
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        binding.pickgeolocationMapView.apply {
            overlays.add(
                MapEventsOverlay(
                    object: MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            p?.let { updatePickMarker(it) }
                            return true
                        }
                        override fun longPressHelper(p: GeoPoint?): Boolean = false
                    }
                )
            )

            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

            setMultiTouchControls(true)

            visibility = View.VISIBLE
            setTileSource(TileSourceFactory.MAPNIK)
            isTilesScaledToDpi = true

            controller.apply {
                setZoom(5.0)
                minZoomLevel = 3.5
            }

            setHasTransientState(true)
        }

        if (PermissionsRequester.hasLocationPermission()) {
            tryToGetMyLocation()
        }
    }

    private fun updateMyLocationButton(locationStatus: ObservableOsmLocationProvider.LocationState) {
        fun updateButton(@DrawableRes drawableId: Int) {
            binding.pickgeolocationMyGeolocation.setImageResource(drawableId)
        }
        updateButton(
            when (locationStatus) {
                ObservableOsmLocationProvider.LocationState.LocationReceived -> {
                    R.drawable.ic_crosshairs_gps
                }
                ObservableOsmLocationProvider.LocationState.LocationNotFound -> {
                    R.drawable.ic_crosshairs_question
                }
            }
        )
    }

    private fun updatePickMarker(location: GeoPoint) {
        if (pickMarker == null) {
            pickMarker = Marker(binding.pickgeolocationMapView).apply {
                icon = resources.getDrawable(R.drawable.ic_location).apply {
                    setColorFilter(pointerColor, PorterDuff.Mode.MULTIPLY)
                }
                /* Ignore just to avoid showing a strange standard osm bubble on marker click */
                setOnMarkerClickListener { _, _ -> false }
            }
            binding.pickgeolocationMapView.overlays.add(pickMarker)
        }
        pickMarker?.position = location
        binding.pickgeolocationMapView.invalidate()
        updateLocationInfoBubble(location)
    }

    private fun updateLocationInfoBubble(location: GeoPoint?) {
        if (location != null) {
            binding.pickgeolocationProgressbar.visibility = View.VISIBLE
            lifecycleScope.launch(CoroutineExceptionHandler { _, ex ->
                binding.pickgeolocationProgressbar.visibility = View.INVISIBLE
                binding.pickgeolocationLocationTitle.visibility = View.GONE
                LogManager.exception(this, ex)
            }) {
                val lang = Locale.getDefault().language
                val place = NominatimRetrofitModule.api.fromLonLat(
                    location.longitude, location.latitude, lang
                )
                binding.pickgeolocationLocationTitle.text = place.prettyName
                binding.pickgeolocationLocationTitle.visibility = View.VISIBLE
                binding.pickgeolocationProgressbar.visibility = View.INVISIBLE
            }
            val coordFormatString = "%.4f"
            binding.pickgeolocationLocationCoordinates.text =
                "${coordFormatString.format(location.longitude)}, ${coordFormatString.format(location.latitude)}"

            binding.pickgeolocationLocationSendButton.setOnClickListener {
                setResult(
                    RESULT_OK,
                    Intent().apply {
                        putExtra(LAT_RESULT, location.latitude)
                        putExtra(LON_RESULT, location.longitude)
                    }
                )
                finish()
            }
            binding.pickgeolocationLocationBottomRoot.visibility = View.VISIBLE
        } else {
            binding.pickgeolocationLocationBottomRoot.visibility = View.GONE
        }
    }

    companion object {
        fun createIntent(context: Context, accountJid: AccountJid) =
            createAccountIntent(context, PickGeolocationActivity::class.java, accountJid)
        const val LAT_RESULT = "com.xabber.android.ui.activity.LAT_RESULT"
        const val LON_RESULT = "com.xabber.android.ui.activity.LON_RESULT"

        private const val REQUEST_LOCATION_PERMISSION_CODE = 10
    }

}