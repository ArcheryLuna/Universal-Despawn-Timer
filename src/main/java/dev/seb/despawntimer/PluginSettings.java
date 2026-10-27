package dev.seb.despawntimer;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public record PluginSettings(
        ItemRule defaults,
        boolean lifeStealHeartsEnabled,
        ItemRule lifeStealHeartRule,
        Map<Material, ItemRule> materialRules,
        long checkIntervalTicks
) {
    public static PluginSettings load(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();
        Logger logger = plugin.getLogger();

        ItemRule defaults = readRule(config.getConfigurationSection("defaults"),
                new ItemRule(DespawnDuration.parse("default"), false), "defaults");
        ItemRule heartRule = readRule(config.getConfigurationSection("lifestealz-hearts"),
                defaults, "lifestealz-hearts");

        Map<Material, ItemRule> materialRules = new EnumMap<>(Material.class);
        ConfigurationSection overrides = config.getConfigurationSection("material-overrides");
        if (overrides != null) {
            for (String key : overrides.getKeys(false)) {
                Material material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
                if (material == null || material.isAir()) {
                    logger.warning("Ignoring unknown material-overrides entry: " + key);
                    continue;
                }
                ConfigurationSection section = overrides.getConfigurationSection(key);
                if (section == null) {
                    throw new IllegalArgumentException("material-overrides." + key + " must be a section");
                }
                materialRules.put(material, readRule(section, defaults, "material-overrides." + key));
            }
        }

        long interval = config.getLong("check-interval-ticks", 20L);
        if (interval < 1L) {
            throw new IllegalArgumentException("check-interval-ticks must be at least 1");
        }

        return new PluginSettings(
                defaults,
                config.getBoolean("lifestealz-hearts.enabled", true),
                heartRule,
                Map.copyOf(materialRules),
                interval
        );
    }

    private static ItemRule readRule(ConfigurationSection section, ItemRule fallback, String path) {
        if (section == null) {
            return fallback;
        }
        DespawnDuration duration = section.contains("despawn-time")
                ? DespawnDuration.parse(section.get("despawn-time"))
                : fallback.duration();
        boolean pickup = section.contains("prevent-mob-pickup")
                ? section.getBoolean("prevent-mob-pickup")
                : fallback.preventMobPickup();
        if (section.contains("despawn-time") && section.get("despawn-time") == null) {
            throw new IllegalArgumentException(path + ".despawn-time cannot be null");
        }
        return new ItemRule(duration, pickup);
    }
}
