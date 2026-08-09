package com.holidate.app.data

import com.holidate.app.data.db.Converters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** [Converters] serializes the interests list via org.json, so it runs under Robolectric. */
@RunWith(RobolectricTestRunner::class)
class ConvertersTest {

    private val converters = Converters()

    @Test
    fun roundTripsInterests() {
        val interests = listOf("hiking", "live music", "cooking")
        val restored = converters.toInterests(converters.fromInterests(interests))
        assertEquals(interests, restored)
    }

    @Test
    fun handlesValuesWithCommasAndQuotes() {
        // Interests are user text; the JSON encoding must survive delimiters and quotes.
        val interests = listOf("wine, cheese & travel", "\"quoted\"", "50% off")
        val restored = converters.toInterests(converters.fromInterests(interests))
        assertEquals(interests, restored)
    }

    @Test
    fun emptyStringDecodesToEmptyList() {
        assertTrue(converters.toInterests("").isEmpty())
    }

    @Test
    fun emptyListRoundTrips() {
        val restored = converters.toInterests(converters.fromInterests(emptyList()))
        assertTrue(restored.isEmpty())
    }
}
