package dev.seb.despawntimer;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

final class ItemTimerListener implements Listener {
    private final Plugin plugin;
    private final LifeStealZHook lifeStealZ;
    private final NamespacedKey bornAtKey;
    private final NamespacedKey expiresAtKey;
    private final NamespacedKey originalUnlimitedKey;
    private final NamespacedKey elapsedTicksKey;
    private final NamespacedKey durationTicksKey;
    private final Map<UUID, TrackedItem> trackedItems = new HashMap<>();
    private final PriorityQueue<Expiry> expiryQueue = new PriorityQueue<>(Comparator.comparingLong(Expiry::dueTick));
    private volatile PluginSettings settings;
    private long serverTick;
    private long ticksUntilMaintenance;

    ItemTimerListener(Plugin plugin, LifeStealZHook lifeStealZ, PluginSettings settings) {
        this.plugin = plugin;
        this.lifeStealZ = lifeStealZ;
        this.settings = settings;
        this.bornAtKey = new NamespacedKey(plugin, "born_at");
        this.expiresAtKey = new NamespacedKey(plugin, "expires_at");
        this.originalUnlimitedKey = new NamespacedKey(plugin, "original_unlimited");
        this.elapsedTicksKey = new NamespacedKey(plugin, "elapsed_ticks");
        this.durationTicksKey = new NamespacedKey(plugin, "duration_ticks");
        this.ticksUntilMaintenance = settings.checkIntervalTicks();
    }

    void setSettings(PluginSettings settings) {
        pauseAllTrackedItems();
        this.settings = settings;
        this.ticksUntilMaintenance = settings.checkIntervalTicks();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        apply(event.getEntity(), true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player) && resolve(event.getItem()).preventMobPickup()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        PersistentDataContainer data = event.getEntity().getPersistentDataContainer();
        Long duration = data.get(durationTicksKey, PersistentDataType.LONG);
        Long elapsed = currentElapsedTicks(event.getEntity());
        if (duration != null && elapsed != null && (duration < 0L || elapsed < duration)) {
            // Bukkit grants another vanilla lifetime when this event is cancelled.
            // Our tick counter continues independently and removes the item on time.
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemMerge(ItemMergeEvent event) {
        PersistentDataContainer source = event.getEntity().getPersistentDataContainer();
        PersistentDataContainer target = event.getTarget().getPersistentDataContainer();
        Long sourceElapsed = currentElapsedTicks(event.getEntity());
        Long targetElapsed = currentElapsedTicks(event.getTarget());
        if (sourceElapsed != null && (targetElapsed == null || sourceElapsed > targetElapsed)) {
            target.set(elapsedTicksKey, PersistentDataType.LONG, sourceElapsed);
        }
        Long sourceDuration = source.get(durationTicksKey, PersistentDataType.LONG);
        if (sourceDuration != null && !target.has(durationTicksKey, PersistentDataType.LONG)) {
            target.set(durationTicksKey, PersistentDataType.LONG, sourceDuration);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void afterItemMerge(ItemMergeEvent event) {
        untrack(event.getEntity(), false);
        rescheduleFromData(event.getTarget());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void afterItemPickup(EntityPickupItemEvent event) {
        untrack(event.getItem(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        // Items in this chunk may have existed before the plugin was installed or
        // may not have been loaded when the config was last reloaded.
        processChunk(event.getChunk(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Item item) {
                untrack(item, true);
            }
        }
    }

    void processLoadedItems() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                apply(item, false);
            }
        }
    }

    void tick() {
        serverTick++;
        expireDueItems();

        ticksUntilMaintenance--;
        if (ticksUntilMaintenance <= 0L) {
            persistAndPruneTrackedItems();
            ticksUntilMaintenance = settings.checkIntervalTicks();
        }
    }

    void pauseAllTrackedItems() {
        for (TrackedItem tracked : trackedItems.values()) {
            persistElapsed(tracked);
        }
        trackedItems.clear();
        expiryQueue.clear();
    }

    ItemRule resolve(Item item) {
        PluginSettings current = settings;
        if (current.lifeStealHeartsEnabled() && lifeStealZ.isHeart(item.getItemStack())) {
            return current.lifeStealHeartRule();
        }
        return current.materialRules().getOrDefault(item.getItemStack().getType(), current.defaults());
    }

    private void processChunk(Chunk chunk, boolean reconfigure) {
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Item item) {
                if (reconfigure) {
                    apply(item, false);
                }
            }
        }
    }

    private void apply(Item item, boolean newlySpawned) {
        PersistentDataContainer data = item.getPersistentDataContainer();
        Long elapsed = data.get(elapsedTicksKey, PersistentDataType.LONG);

        // Migrate items created by version 1.0.0. Restoring their original
        // lifetime first prevents old unloaded items from remaining immortal.
        if (data.has(bornAtKey, PersistentDataType.LONG)) {
            restoreOriginalLifetime(item, data);
            elapsed = Math.max(0L, item.getTicksLived());
        } else if (newlySpawned) {
            elapsed = 0L;
        } else if (elapsed == null) {
            elapsed = Math.max(0L, item.getTicksLived());
        }

        ItemRule rule = resolve(item);

        switch (rule.duration().mode()) {
            case DEFAULT -> {
                data.remove(elapsedTicksKey);
                data.remove(durationTicksKey);
                untrack(item, false);
            }
            case NEVER -> {
                data.set(elapsedTicksKey, PersistentDataType.LONG, elapsed);
                data.set(durationTicksKey, PersistentDataType.LONG, -1L);
                untrack(item, false);
            }
            case FINITE -> {
                long duration = rule.duration().ticks();
                data.set(elapsedTicksKey, PersistentDataType.LONG, elapsed);
                data.set(durationTicksKey, PersistentDataType.LONG, duration);
                if (elapsed >= duration) {
                    untrack(item, false);
                    item.remove();
                } else {
                    track(item, elapsed, duration);
                }
            }
        }
    }

    private void restoreOriginalLifetime(Item item, PersistentDataContainer data) {
        Byte original = data.get(originalUnlimitedKey, PersistentDataType.BYTE);
        item.setUnlimitedLifetime(original != null && original != 0);
        data.remove(bornAtKey);
        data.remove(expiresAtKey);
        data.remove(originalUnlimitedKey);
    }

    private void rescheduleFromData(Item item) {
        PersistentDataContainer data = item.getPersistentDataContainer();
        Long elapsed = data.get(elapsedTicksKey, PersistentDataType.LONG);
        Long duration = data.get(durationTicksKey, PersistentDataType.LONG);
        if (elapsed == null || duration == null || duration < 0L) {
            untrack(item, false);
            return;
        }
        track(item, elapsed, duration);
    }

    private void track(Item item, long elapsed, long duration) {
        long remaining = Math.max(0L, duration - elapsed);
        long dueTick;
        try {
            dueTick = Math.addExact(serverTick, remaining);
        } catch (ArithmeticException exception) {
            dueTick = Long.MAX_VALUE;
        }
        TrackedItem tracked = new TrackedItem(item, serverTick, elapsed, duration, dueTick);
        trackedItems.put(item.getUniqueId(), tracked);
        expiryQueue.add(new Expiry(item.getUniqueId(), dueTick));
    }

    private void expireDueItems() {
        while (!expiryQueue.isEmpty() && expiryQueue.peek().dueTick() <= serverTick) {
            Expiry expiry = expiryQueue.poll();
            TrackedItem tracked = trackedItems.get(expiry.itemId());
            if (tracked == null || tracked.dueTick() != expiry.dueTick()) {
                continue;
            }
            trackedItems.remove(expiry.itemId());
            if (tracked.item().isValid()) {
                tracked.item().getPersistentDataContainer().set(
                        elapsedTicksKey, PersistentDataType.LONG, tracked.duration());
                tracked.item().remove();
            }
        }
    }

    private void persistAndPruneTrackedItems() {
        Iterator<Map.Entry<UUID, TrackedItem>> iterator = trackedItems.entrySet().iterator();
        while (iterator.hasNext()) {
            TrackedItem tracked = iterator.next().getValue();
            if (!tracked.item().isValid()) {
                iterator.remove();
            } else {
                persistElapsed(tracked);
            }
        }

        // Item merges and pickups deliberately use lazy queue invalidation.
        // Periodically rebuilding keeps stale queue entries bounded without
        // making those frequent events perform an O(n) queue removal.
        expiryQueue.clear();
        for (Map.Entry<UUID, TrackedItem> entry : trackedItems.entrySet()) {
            expiryQueue.add(new Expiry(entry.getKey(), entry.getValue().dueTick()));
        }
    }

    private void untrack(Item item, boolean persist) {
        TrackedItem tracked = trackedItems.remove(item.getUniqueId());
        if (persist && tracked != null) {
            persistElapsed(tracked);
        }
    }

    private Long currentElapsedTicks(Item item) {
        TrackedItem tracked = trackedItems.get(item.getUniqueId());
        if (tracked != null) {
            return elapsedAtCurrentTick(tracked);
        }
        return item.getPersistentDataContainer().get(elapsedTicksKey, PersistentDataType.LONG);
    }

    private void persistElapsed(TrackedItem tracked) {
        if (tracked.item().isValid()) {
            tracked.item().getPersistentDataContainer().set(
                    elapsedTicksKey, PersistentDataType.LONG, elapsedAtCurrentTick(tracked));
        }
    }

    private long elapsedAtCurrentTick(TrackedItem tracked) {
        long runningTicks = Math.max(0L, serverTick - tracked.startedAtTick());
        try {
            return Math.min(tracked.duration(), Math.addExact(tracked.baseElapsed(), runningTicks));
        } catch (ArithmeticException exception) {
            return tracked.duration();
        }
    }

    int trackedItemCount() {
        return trackedItems.size();
    }

    private record TrackedItem(
            Item item,
            long startedAtTick,
            long baseElapsed,
            long duration,
            long dueTick
    ) {
    }

    private record Expiry(UUID itemId, long dueTick) {
    }
}
