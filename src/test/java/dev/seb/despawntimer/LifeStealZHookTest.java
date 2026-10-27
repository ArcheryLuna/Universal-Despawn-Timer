package dev.seb.despawntimer;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifeStealZHookTest {
    @Test
    void findsEveryHeartTypeAndIgnoresOtherCustomItems() {
        YamlConfiguration items = YamlConfiguration.loadConfiguration(new StringReader("""
                defaultheart:
                  customItemType: heart
                powerfulHeart:
                  customItemType: HEART
                revive:
                  customItemType: revivebeacon
                malformed: not-a-section
                """));

        assertEquals(Set.of("defaultheart", "powerfulheart"), LifeStealZHook.findHeartIds(items));
    }

    @Test
    void reportsNoHeartsForEmptyOrUnrelatedConfiguration() {
        YamlConfiguration items = YamlConfiguration.loadConfiguration(new StringReader("""
                revive:
                  customItemType: revive
                """));

        assertTrue(LifeStealZHook.findHeartIds(items).isEmpty());
    }
}
