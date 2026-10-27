package dev.seb.despawntimer;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniversalDespawnTimerIntegrationTest {
    private ServerMock server;
    private WorldMock world;
    private UniversalDespawnTimerPlugin plugin;
    private Location dropLocation;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        plugin = MockBukkit.load(UniversalDespawnTimerPlugin.class);
        dropLocation = new Location(world, 0.5, 65, 0.5);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void finiteItemExpiresOnTheExactConfiguredServerTick() {
        setDefaultRule("3t", false);
        Item item = drop(Material.DIAMOND);

        assertEquals(1, plugin.getTrackedItemCount());
        server.getScheduler().performTicks(2L);
        assertTrue(item.isValid());

        server.getScheduler().performOneTick();
        assertFalse(item.isValid());
        assertEquals(0, plugin.getTrackedItemCount());
    }

    @Test
    void defaultItemsAreNotKeptInTheActiveTimerIndex() {
        Item item = drop(Material.COBBLESTONE);

        assertTrue(item.isValid());
        assertEquals(0, plugin.getTrackedItemCount());
    }

    @Test
    void timerPausesWhileChunkIsUnloaded() {
        setDefaultRule("5t", false);
        Item item = drop(Material.DIAMOND);
        server.getScheduler().performTicks(2L);

        server.getPluginManager().callEvent(new ChunkUnloadEvent(item.getChunk()));
        assertEquals(0, plugin.getTrackedItemCount());
        server.getScheduler().performTicks(100L);
        assertTrue(item.isValid());

        server.getPluginManager().callEvent(new ChunkLoadEvent(item.getChunk(), false));
        server.getScheduler().performTicks(2L);
        assertTrue(item.isValid());
        server.getScheduler().performOneTick();
        assertFalse(item.isValid());
    }

    @Test
    void mergedStackKeepsTheOlderItemsElapsedLifetime() {
        setDefaultRule("10t", false);
        Item older = drop(Material.DIAMOND);
        server.getScheduler().performTicks(4L);
        Item newer = drop(Material.DIAMOND);

        server.getPluginManager().callEvent(new ItemMergeEvent(older, newer));
        older.remove(); // The real server removes the source after the event.

        server.getScheduler().performTicks(5L);
        assertTrue(newer.isValid());
        server.getScheduler().performOneTick();
        assertFalse(newer.isValid());
    }

    @Test
    void reloadPreservesElapsedTicksAndAppliesTheNewDuration() {
        setDefaultRule("10t", false);
        Item item = drop(Material.DIAMOND);
        server.getScheduler().performTicks(4L);

        setDefaultRule("6t", false);
        server.getScheduler().performOneTick();
        assertTrue(item.isValid());
        server.getScheduler().performOneTick();
        assertFalse(item.isValid());
    }

    @Test
    void mobPickupIsCancelledButPlayerIndependentTimerStateIsUntouched() {
        setDefaultRule("never", true);
        Item item = drop(Material.DIAMOND);
        Zombie zombie = world.spawn(dropLocation, Zombie.class);
        EntityPickupItemEvent event = new EntityPickupItemEvent(zombie, item, 0);

        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
        assertTrue(item.isValid());
    }

    @Test
    void disablingPluginDoesNotLeaveOrdinaryItemsWithUnlimitedLifetime() {
        setDefaultRule("15m", false);
        Item item = drop(Material.DIAMOND);
        assertFalse(item.isUnlimitedLifetime());

        server.getPluginManager().disablePlugin(plugin);

        assertFalse(item.isUnlimitedLifetime());
    }

    private Item drop(Material material) {
        return world.dropItem(dropLocation, new ItemStack(material));
    }

    private void setDefaultRule(String duration, boolean preventMobPickup) {
        plugin.getConfig().set("defaults.despawn-time", duration);
        plugin.getConfig().set("defaults.prevent-mob-pickup", preventMobPickup);
        plugin.saveConfig();
        plugin.reloadPlugin();
    }
}
