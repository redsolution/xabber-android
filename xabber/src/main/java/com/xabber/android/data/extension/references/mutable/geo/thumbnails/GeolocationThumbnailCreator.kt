package com.xabber.android.data.extension.references.mutable.geo.thumbnails

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.preference.PreferenceManager
import android.util.TypedValue
import android.view.WindowManager
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.log.LogManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.drawing.MapSnapshot
import org.osmdroid.views.drawing.MapSnapshot.MapSnapshotable
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import kotlin.math.roundToInt


internal class GeolocationThumbnailCreator(private val context: Context) {
    private val displayWidth = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
        ?.defaultDisplay?.width ?: 0

    private val mapWidth = displayWidth * MESSAGE_WIDTH_TO_DISPLAY_WIDTH_PROPORTION * MAP_WIDTH_TO_MESSAGE_WIDTH_PROPORTION
    private val mapHeight = mapWidth * MAP_HEIGHT_TO_MAP_WIDTH_PROPORTION

    private val markerColorlessDrawable = context.resources.getDrawable(R.drawable.ic_location)
    private val markerOffsetY = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        markerColorlessDrawable.intrinsicHeight.toFloat(),
        context.resources.displayMetrics
    ) * MARKER_Y_OFFSET_PROPORTION

    fun requestThumbnail(
        lon: Double, lat: Double, pointerColor: Int, onBitmapCreated: (Bitmap) -> Unit
    ) {
        val geoPoint = GeoPoint(lat, lon)

        LogManager.i(this, "RequestThumbnail(lon: $lon, lat: $lat; pointerColor: $pointerColor)")

        Application.getInstance().runOnUiThread {
            Configuration.getInstance().load(
                context, PreferenceManager.getDefaultSharedPreferences(context)
            )

            val overlays = arrayListOf<Overlay>(
                Marker(MapView(context)).apply {
                    icon = markerColorlessDrawable.apply {
                        setColorFilter(pointerColor, PorterDuff.Mode.MULTIPLY)
                    }
                    position = geoPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            )

            val tileProvider = MapTileProviderBasic(context).apply {
                tileSource = TileSourceFactory.MAPNIK
            }

            val projection = Projection(
                MAP_ZOOM,                 // zoom
                mapWidth.roundToInt(),    // width
                mapHeight.roundToInt(),   // height
                geoPoint,                 // center
                0.0F,                     // orientation
                true,                     // horizontal wrap enabled
                true,                     // vertical wrap enabled
                0,                        // map center offset X
                50                        // map center offset Y
            )

            val mapSnapshotable = MapSnapshotable { onBitmapCreated(Bitmap.createBitmap(it.bitmap)) }

            val snapshot = MapSnapshot(
                mapSnapshotable,
                MapSnapshot.INCLUDE_FLAG_UPTODATE,
                tileProvider,
                overlays,
                projection
            )

            Thread(snapshot).start()
        }
    }

    companion object {
        private const val MESSAGE_WIDTH_TO_DISPLAY_WIDTH_PROPORTION = 0.8
        private const val MAP_WIDTH_TO_MESSAGE_WIDTH_PROPORTION = 0.75
        private const val MAP_HEIGHT_TO_MAP_WIDTH_PROPORTION = 0.75
        private const val MARKER_Y_OFFSET_PROPORTION = 0.33

        private const val MAP_ZOOM = 16.5
    }

}