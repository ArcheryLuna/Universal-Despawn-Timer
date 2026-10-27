package dev.seb.despawntimer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DespawnDurationTest {
    @Test
    void parsesSupportedUnits() {
        assertEquals(50L, DespawnDuration.parse("1t").milliseconds());
        assertEquals(1L, DespawnDuration.parse("1t").ticks());
        assertEquals(30_000L, DespawnDuration.parse("30s").milliseconds());
        assertEquals(900_000L, DespawnDuration.parse("15m").milliseconds());
        assertEquals(18_000L, DespawnDuration.parse("15m").ticks());
        assertEquals(7_200_000L, DespawnDuration.parse("2h").milliseconds());
        assertEquals(86_400_000L, DespawnDuration.parse("1d").milliseconds());
        assertEquals(5_000L, DespawnDuration.parse(5).milliseconds());
    }

    @Test
    void parsesSpecialModes() {
        assertEquals(DespawnDuration.Mode.DEFAULT, DespawnDuration.parse("default").mode());
        assertEquals(DespawnDuration.Mode.NEVER, DespawnDuration.parse("never").mode());
    }

    @Test
    void rejectsInvalidOrZeroDurations() {
        assertThrows(IllegalArgumentException.class, () -> DespawnDuration.parse("0s"));
        assertThrows(IllegalArgumentException.class, () -> DespawnDuration.parse("soon"));
        assertThrows(IllegalArgumentException.class, () -> DespawnDuration.parse("999999999999999999999d"));
    }

    @Test
    void specialModesDoNotExposeTicks() {
        assertThrows(IllegalStateException.class, () -> DespawnDuration.parse("default").ticks());
        assertThrows(IllegalStateException.class, () -> DespawnDuration.parse("never").ticks());
    }
}
