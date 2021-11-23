package com.xabber.android.data.extension.references.mutable.geo.thumbnails

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.net.Uri
import android.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.xabber.android.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.roundToInt
import org.osmdroid.views.drawing.MapSnapshot
import org.osmdroid.views.drawing.MapSnapshot.MapSnapshotable


internal class GeolocationThumbnailCreator(private val context: Context) {
    private val displayWidth = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
        ?.defaultDisplay?.width ?: 0

    private val mapWidth = displayWidth * MESSAGE_WIDTH_TO_DISPLAY_WIDTH_PROPORTION * MAP_WIDTH_TO_MESSAGE_WIDTH_PROPORTION
    private val mapHeight = mapWidth * MAP_HEIGHT_TO_MAP_WIDTH_PROPORTION

    fun createThumbnailBitmap(
        lon: Double, lat: Double, pointerColor: Int,
        onBitmapCreated: MapSnapshotable
    ) = takeSnapshot(createMapView(lon, lat, pointerColor), onBitmapCreated)

    private fun takeSnapshot(mapView: MapView, onBitmapCreated: MapSnapshotable) {
        val mapSnapshot = MapSnapshot(
            MapSnapshotable { pMapSnapshot ->
                if (pMapSnapshot.status != MapSnapshot.Status.CANVAS_OK) {
                    return@MapSnapshotable
                }
                onBitmapCreated.callback(pMapSnapshot)
            },
            MapSnapshot.INCLUDE_FLAGS_ALL,
            mapView
        )
        Thread(mapSnapshot).start()
    }

    private fun loadBitmapFromView(v: View): Bitmap {
        val b = Bitmap.createBitmap(
            v.layoutParams.width,
            v.layoutParams.height,
            Bitmap.Config.ARGB_8888
        )
        val c = Canvas(b)
        v.layout(v.left, v.top, v.right, v.bottom)
        v.draw(c)
        return b
    }

    private fun createMapView(lon: Double, lat: Double, pointerColor: Int): MapView {
        val geoPoint = GeoPoint(lat, lon)
        Configuration.getInstance().load(
            context, PreferenceManager.getDefaultSharedPreferences(context)
        )

        val mapView = MapView(context).apply {
            setOnClickListener {
                context.startActivity(
                    Intent().apply {
                        action = Intent.ACTION_VIEW
                        data = Uri.parse("geo:$lat,$lon?q=\"$lat, $lon\"")
                    }
                )
            }

            overlays.add(
                Marker(this, context).apply {
                    icon = context.resources.getDrawable(R.drawable.ic_location).apply {
                        setColorFilter(pointerColor, PorterDuff.Mode.MULTIPLY)
                    }
                    position = geoPoint
                }
            )

            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setMultiTouchControls(false)

            visibility = View.VISIBLE
            setTileSource(TileSourceFactory.MAPNIK)

            controller?.apply {
                setZoom(15.0)
                setCenter(geoPoint)
            }

            layoutParams = ViewGroup.LayoutParams(mapWidth.roundToInt(), mapHeight.roundToInt())

        }.also {
            it.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        mapView.tileProvider.tileCache

        return mapView
    }

    companion object {
        private const val MESSAGE_WIDTH_TO_DISPLAY_WIDTH_PROPORTION = 0.8
        private const val MAP_WIDTH_TO_MESSAGE_WIDTH_PROPORTION = 0.6
        private const val MAP_HEIGHT_TO_MAP_WIDTH_PROPORTION = 0.75
    }
}