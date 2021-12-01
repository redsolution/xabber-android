package com.xabber.android.ui.activity

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.createAccountIntent
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.getAccountJid
import com.xabber.android.data.http.NominatimRetrofitModule
import com.xabber.android.data.log.LogManager
import com.xabber.android.databinding.PickGeolocationActivityBinding
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.color.StatusBarPainter
import com.xabber.android.ui.helper.PermissionsRequester
import com.xabber.android.ui.widget.SearchToolbar
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class PickGeolocationActivity: ManagedActivity() {

    private lateinit var binding: PickGeolocationActivityBinding
    private var pickMarker: Marker? = null
    private var pointerColor: Int = 0

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

        binding.searchToolbar.onTextChangedListener = SearchToolbar.OnTextChangedListener {
            //todo make request
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

    private fun tryToGetMyLocation(){
        if (PermissionsRequester.requestLocationPermissionIfNeeded(this, REQUEST_LOCATION_PERMISSION_CODE)) {
            myLocationOverlay = MyLocationNewOverlay(binding.pickgeolocationMapView).apply {
                enableMyLocation()
                enableFollowLocation()
            }
            binding.pickgeolocationMapView.apply {
                overlays.add(myLocationOverlay)
                Application.getInstance().runOnUiThreadDelay(1000) {
                    controller.setZoom(15.0)
                    binding.pickgeolocationMyGeolocation.setImageResource(R.drawable.ic_crosshairs_gps)
                    Application.getInstance().runOnUiThreadDelay(1500) {
                        addMapListener(
                            object: MapListener {
                                override fun onScroll(event: ScrollEvent?): Boolean {
                                    binding.pickgeolocationMyGeolocation.setImageResource(R.drawable.ic_crosshairs_question)
                                    removeMapListener(this)
                                    return true
                                }

                                override fun onZoom(event: ZoomEvent?): Boolean { return false }
                            }
                        )
                    }
                }
            }
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
            } else {
                Toast.makeText(this, "shit!", Toast.LENGTH_SHORT).show() //todo show Error
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
    }

    private fun updatePickMarker(location: GeoPoint) {
        if (pickMarker == null) {
            pickMarker = Marker(binding.pickgeolocationMapView).apply {
                icon = resources.getDrawable(R.drawable.ic_location).apply {
                    setColorFilter(pointerColor, PorterDuff.Mode.MULTIPLY)
                }
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
                val place = NominatimRetrofitModule.api.fromLonLat(location.longitude, location.latitude)
                binding.pickgeolocationLocationTitle.text = place.displayName
                binding.pickgeolocationLocationTitle.visibility = View.VISIBLE
                binding.pickgeolocationProgressbar.visibility = View.INVISIBLE
            }
            binding.pickgeolocationLocationCoordinates.text = "${location.longitude}, ${location.latitude}"
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