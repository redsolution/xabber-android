package com.xabber.android.ui.activity

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doOnTextChanged
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.createAccountIntent
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.getAccountJid
import com.xabber.android.databinding.PickGeolocationActivityBinding
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.color.StatusBarPainter
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class PickGeolocationActivity: ManagedActivity() {

    private lateinit var binding: PickGeolocationActivityBinding
    private var pickMarker: Marker? = null
    private var pointerColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = PickGeolocationActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent?.getAccountJid()?.let { accountJid ->
            pointerColor = ColorManager.getInstance().accountPainter.getAccountColorWithTint(
                accountJid, 500
            )
            binding.pickgeolocationLocationSendButton.setColorFilter(pointerColor)
            setToolbarColor(accountJid)
            setStatusBarColor(accountJid)
        }

        binding.pickgeolocationToolbarBackButton.setOnClickListener { finish() }
        binding.pickgeolocationToolbarSearchButton.setOnClickListener {
            binding.pickgeolocationtoolbarGreetingsView.visibility = View.GONE
            binding.pickgeolocationToolbarSearchView.visibility = View.VISIBLE
            binding.pickgeolocationToolbarEdittext.requestFocus()
            (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.showSoftInput(
                binding.pickgeolocationToolbarEdittext, InputMethodManager.SHOW_IMPLICIT
            )
        }
        binding.pickgeolocationToolbarClearButton.setOnClickListener {
            binding.pickgeolocationToolbarEdittext.setText("")
        }
        binding.pickgeolocationToolbarEdittext.doOnTextChanged { text, start, before, count ->
            if (text.isNullOrEmpty()) {
                binding.pickgeolocationToolbarClearButton.visibility = View.GONE
            } else {
                binding.pickgeolocationToolbarClearButton.visibility = View.VISIBLE
                //todo make request
            }
        }
        setupMap()
        super.onCreate(savedInstanceState)
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

    private fun setToolbarColor(accountJid: AccountJid) {
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            binding.root.setBackgroundColor(
                ColorManager.getInstance().accountPainter.getAccountRippleColor(
                    accountJid
                )
            )
        } else {
            val typedValue = TypedValue()
            this.theme.resolveAttribute(R.attr.bars_color, typedValue, true)
            binding.root.setBackgroundColor(typedValue.data)
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
        // todo load place info from api
        // todo change to use not only long lat
        if (location != null) {
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
    }

}