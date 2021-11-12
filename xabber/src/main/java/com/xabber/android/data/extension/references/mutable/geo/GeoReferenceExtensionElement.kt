package com.xabber.android.data.extension.references.mutable.geo

import com.xabber.android.data.extension.references.mutable.Mutable
import org.jivesoftware.smack.util.XmlStringBuilder
import org.jivesoftware.smackx.geoloc.packet.GeoLocation

class GeoReferenceExtensionElement(
    val geoLocationElements: List<GeoLocation>, start: Int, end: Int
) : Mutable(start, end) {

    override fun appendToXML(xml: XmlStringBuilder?) {
        xml?.apply {
            for (location in geoLocationElements) {
                append(location.toXML())
            }
        }
    }

    companion object {
        private fun createBodyFromTemplate(
            longitude: Double, latitude: Double
        ) = "geo:$latitude,$longitude"

        fun createFromLongitudeAndLatitude(
            longitude: Double,
            latitude: Double,
            start: Int = 0,
            end: Int = start + createBodyFromTemplate(longitude, latitude).length
        ): GeoReferenceExtensionElement {
            val geoLocation = GeoLocation.builder().apply {
                setLon(longitude)
                setLat(latitude)
            }.build()
            return GeoReferenceExtensionElement(listOf(geoLocation), start, end)
        }
    }

}