package es.joshluq.kmsafe.ui.navigation

import android.content.Intent
import android.net.Uri
import es.joshluq.kmsafe.core.navigation.Destination
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkParserTest {

    @Test
    fun `given null intent then returns null`() {
        assertNull(DeepLinkParser.parse(null))
    }

    @Test
    fun `given intent with null data then returns null`() {
        val intent: Intent = mockk { every { data } returns null }
        assertNull(DeepLinkParser.parse(intent))
    }

    @Test
    fun `given intent with invalid host then returns null`() {
        val uri: Uri = mockk {
            every { scheme } returns "https"
            every { host } returns "other.com"
        }
        val intent: Intent = mockk { every { data } returns uri }
        assertNull(DeepLinkParser.parse(intent))
    }

    @Test
    fun `given empty path then resolves to Destination Dashboard`() {
        val uri: Uri = mockk {
            every { scheme } returns "https"
            every { host } returns "kmsafe.app"
            every { pathSegments } returns emptyList()
        }
        val intent: Intent = mockk { every { data } returns uri }

        val dest = DeepLinkParser.parse(intent)
        assertEquals(Destination.Dashboard, dest)
    }

    @Test
    fun `given expenses uri then resolves to Destination Expenses`() {
        val uri: Uri = mockk {
            every { scheme } returns "https"
            every { host } returns "kmsafe.app"
            every { pathSegments } returns listOf("expenses")
            every { getQueryParameter("stationId") } returns "st-1"
            every { getBooleanQueryParameter("autoOpen", false) } returns true
            every { getBooleanQueryParameter("autoOpenAdd", false) } returns false
            every { getBooleanQueryParameter("priceReportMode", false) } returns false
        }
        val intent: Intent = mockk { every { data } returns uri }

        val dest = DeepLinkParser.parse(intent)
        assertEquals(
            Destination.Expenses(stationId = "st-1", autoOpenAdd = true, priceReportMode = false),
            dest
        )
    }

    @Test
    fun `given expenses uri with autoOpenAdd then resolves to Destination Expenses with autoOpenAdd true`() {
        val uri: Uri = mockk {
            every { scheme } returns "https"
            every { host } returns "kmsafe.app"
            every { pathSegments } returns listOf("expenses")
            every { getQueryParameter("stationId") } returns "st-99"
            every { getBooleanQueryParameter("autoOpen", false) } returns false
            every { getBooleanQueryParameter("autoOpenAdd", false) } returns true
            every { getBooleanQueryParameter("priceReportMode", false) } returns false
        }
        val intent: Intent = mockk { every { data } returns uri }

        val dest = DeepLinkParser.parse(intent)
        assertEquals(
            Destination.Expenses(stationId = "st-99", autoOpenAdd = true, priceReportMode = false),
            dest
        )
    }

    @Test
    fun `given vehicle uri then resolves to Destination VehicleDetail`() {
        val uri: Uri = mockk {
            every { scheme } returns "https"
            every { host } returns "kmsafe.app"
            every { pathSegments } returns listOf("vehicle", "car-99")
        }
        val intent: Intent = mockk { every { data } returns uri }

        val dest = DeepLinkParser.parse(intent)
        assertEquals(Destination.VehicleDetail("car-99"), dest)
    }

    @Test
    fun `given record uri then resolves to Destination RecordDetail`() {
        val uri: Uri = mockk {
            every { scheme } returns "https"
            every { host } returns "kmsafe.app"
            every { pathSegments } returns listOf("record", "rec-42")
        }
        val intent: Intent = mockk { every { data } returns uri }

        val dest = DeepLinkParser.parse(intent)
        assertEquals(Destination.RecordDetail("rec-42"), dest)
    }

    @Test
    fun `given station uri then resolves to Destination StationDetail`() {
        val uri: Uri = mockk {
            every { scheme } returns "https"
            every { host } returns "kmsafe.app"
            every { pathSegments } returns listOf("station", "st-55")
        }
        val intent: Intent = mockk { every { data } returns uri }

        val dest = DeepLinkParser.parse(intent)
        assertEquals(Destination.StationDetail("st-55"), dest)
    }

    @Test
    fun `given premium uri then resolves to Destination PremiumPaywall`() {
        val uri: Uri = mockk {
            every { scheme } returns "https"
            every { host } returns "kmsafe.app"
            every { pathSegments } returns listOf("premium")
        }
        val intent: Intent = mockk { every { data } returns uri }

        val dest = DeepLinkParser.parse(intent)
        assertEquals(Destination.PremiumPaywall(source = "deeplink"), dest)
    }

    @Test
    fun `given preferences uri then resolves to Destination Preferences`() {
        val uri: Uri = mockk {
            every { scheme } returns "https"
            every { host } returns "kmsafe.app"
            every { pathSegments } returns listOf("preferences")
        }
        val intent: Intent = mockk { every { data } returns uri }

        val dest = DeepLinkParser.parse(intent)
        assertEquals(Destination.Preferences, dest)
    }

    @Test
    fun `given unknown path then returns null`() {
        val uri: Uri = mockk {
            every { scheme } returns "https"
            every { host } returns "kmsafe.app"
            every { pathSegments } returns listOf("unknown_path")
        }
        val intent: Intent = mockk { every { data } returns uri }

        assertNull(DeepLinkParser.parse(intent))
    }
}
