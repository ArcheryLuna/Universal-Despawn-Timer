package dev.seb.despawntimer;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

final class LifeStealZHook {
    private final Plugin owner;
    private Plugin lifeStealZ;
    private Object api;
    private Method getCustomItemId;
    private Set<String> heartIds = Set.of();
    private boolean warned;
    private String status = "not installed";

    LifeStealZHook(Plugin owner) {
        this.owner = owner;
    }

    void reload() {
        clear();
        lifeStealZ = owner.getServer().getPluginManager().getPlugin("LifeStealZ");
        if (lifeStealZ == null || !lifeStealZ.isEnabled()) {
            status = "not installed or disabled";
            owner.getLogger().info("LifeStealZ was not found; heart integration is standing by.");
            return;
        }

        try {
            Method getApi = lifeStealZ.getClass().getMethod("getAPI");
            api = getApi.invoke(null);
            getCustomItemId = api.getClass().getMethod("getCustomItemID", ItemStack.class);
            heartIds = loadHeartIds(lifeStealZ);
            if (heartIds.isEmpty()) {
                api = null;
                getCustomItemId = null;
                status = "disabled: no heart item types found";
                owner.getLogger().warning("LifeStealZ integration was disabled because items.yml contains no "
                        + "customItemType: heart entries.");
                return;
            }
            status = "connected (" + heartIds.size() + " heart types)";
            owner.getLogger().info("LifeStealZ integration enabled (" + heartIds.size() + " heart item type(s)).");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            clear();
            status = "error (see server console)";
            owner.getLogger().log(Level.WARNING,
                    "LifeStealZ was found, but its custom item API could not be connected. Heart rules are disabled.",
                    exception);
        }
    }

    boolean isHeart(ItemStack stack) {
        if (api == null || getCustomItemId == null || heartIds.isEmpty()) {
            return false;
        }
        try {
            Object id = getCustomItemId.invoke(api, stack);
            return id != null && heartIds.contains(String.valueOf(id).toLowerCase(Locale.ROOT));
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            if (!warned) {
                warned = true;
                owner.getLogger().log(Level.WARNING,
                        "LifeStealZ item detection failed; further detection errors will be suppressed.", exception);
            }
            return false;
        }
    }

    boolean isConnected() {
        return api != null && !heartIds.isEmpty();
    }

    int heartTypeCount() {
        return heartIds.size();
    }

    String statusDescription() {
        return status;
    }

    private Set<String> loadHeartIds(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "items.yml");
        if (!file.isFile()) {
            throw new IllegalStateException("LifeStealZ items.yml does not exist at " + file.getPath());
        }
        YamlConfiguration items = YamlConfiguration.loadConfiguration(file);
        return findHeartIds(items);
    }

    static Set<String> findHeartIds(ConfigurationSection items) {
        Set<String> ids = new HashSet<>();
        for (String id : items.getKeys(false)) {
            ConfigurationSection section = items.getConfigurationSection(id);
            if (section != null && "heart".equalsIgnoreCase(section.getString("customItemType", ""))) {
                ids.add(id.toLowerCase(Locale.ROOT));
            }
        }
        return Set.copyOf(ids);
    }

    private void clear() {
        lifeStealZ = null;
        api = null;
        getCustomItemId = null;
        heartIds = Set.of();
        warned = false;
        status = "not installed";
    }
}
