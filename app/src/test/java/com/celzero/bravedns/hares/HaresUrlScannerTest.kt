package com.celzero.bravedns.hares

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HaresUrlScannerTest {
    @Test fun cleanHttpsUrlDoesNotProduceLocalWarning() {
        assertEquals(HaresThreatEvent.Level.LOW, HaresUrlScanner.scan("https://example.com").level)
    }

    @Test fun plainHttpUrlIsFlagged() {
        val result = HaresUrlScanner.scan("http://example.com")
        assertEquals(HaresThreatEvent.Level.MEDIUM, result.level)
        assertTrue(result.reasons.any { it.contains("HTTP") })
    }

    @Test fun embeddedCredentialsAreHighRisk() {
        assertEquals(HaresThreatEvent.Level.HIGH, HaresUrlScanner.scan("https://user:secret@example.com").level)
    }

    @Test fun shortenedUrlNeedsAdditionalVerification() {
        assertEquals(HaresThreatEvent.Level.MEDIUM, HaresUrlScanner.scan("https://bit.ly/example").level)
    }

    @Test fun malformedWhitespaceUrlIsHighRisk() {
        assertEquals(HaresThreatEvent.Level.HIGH, HaresUrlScanner.scan("https://example .com").level)
    }
}
