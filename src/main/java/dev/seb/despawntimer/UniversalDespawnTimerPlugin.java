package dev.seb.despawntimer;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class UniversalDespawnTimerPlugin extends JavaPlugin {
    private LifeStealZHook lifeStealZ;
    private ItemTimerListener listener;
    private BukkitTask scannerTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PluginSettings settings;
        try {
            settings = PluginSettings.load(this);
        } catch (IllegalArgumentException exception) {
            getLogger().severe("Invalid config.yml: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        lifeStealZ = new LifeStealZHook(this);
        lifeStealZ.reload();
        listener = new ItemTimerListener(this, lifeStealZ, settings);
        getServer().getPluginManager().registerEvents(listener, this);

        DespawnTimerCommand commandHandler = new DespawnTimerCommand(this);
        PluginCommand command = getCommand("despawntimer");
        if (command == null) {
            throw new IllegalStateException("despawntimer command is missing from plugin.yml");
        }
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        scheduleTimer();
        listener.processLoadedItems();
        getLogger().info("UniversalDespawnTimer enabled.");
    }

    @Override
    public void onDisable() {
        if (scannerTask != null) {
            scannerTask.cancel();
            scannerTask = null;
        }
        if (listener != null) {
            listener.pauseAllTrackedItems();
        }
    }

    void reloadPlugin() {
        reloadConfig();
        PluginSettings newSettings = PluginSettings.load(this);
        lifeStealZ.reload();
        listener.setSettings(newSettings);
        listener.processLoadedItems();
    }

    boolean isLifeStealConnected() {
        return lifeStealZ != null && lifeStealZ.isConnected();
    }

    int getLifeStealHeartTypeCount() {
        return lifeStealZ == null ? 0 : lifeStealZ.heartTypeCount();
    }

    String getLifeStealStatus() {
        return lifeStealZ == null ? "not initialized" : lifeStealZ.statusDescription();
    }

    int getTrackedItemCount() {
        return listener == null ? 0 : listener.trackedItemCount();
    }

    private void scheduleTimer() {
        if (scannerTask != null) {
            scannerTask.cancel();
        }
        scannerTask = getServer().getScheduler().runTaskTimer(this,
                listener::tick, 1L, 1L);
    }
}
