package dev.ixpu.cullingames.config;

import dev.ixpu.cullingames.CullingGamesPlugin;
import dev.ixpu.cullingames.wave.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ConfigManager {

    private final CullingGamesPlugin plugin;
    private FileConfiguration config;
    private long waveDuration = 300; // 5 minutes default
    private MobSpawnData babyCreeper;
    private List<WaveDefinition> waves = new ArrayList<>();

    public ConfigManager(CullingGamesPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        waveDuration = config.getLong("waves.wave-duration", 300);
        
        loadWaves();
    }

    public List<WaveDefinition> loadWaves() {
        waves.clear();
        List<WaveDefinition> loadedWaves = new ArrayList<>();
        
        if (!config.contains("waves")) {
            plugin.getLogger().warning("No waves configured in config.yml");
            return loadedWaves;
        }
        
        for (String waveKey : config.getConfigurationSection("waves").getKeys(false)) {
            if (waveKey.equals("wave-duration") || waveKey.equals("enabled") || waveKey.equals("general")) {
                continue;
            }
            
            String wavePath = "waves." + waveKey;
            if (!config.contains(wavePath + ".mobs")) {
                continue;
            }
            
            String waveName = config.getString(wavePath + ".name", waveKey);
            int waveOrder = config.getInt(wavePath + ".order", loadedWaves.size());
            
            List<MobSpawn> mobSpawns = new ArrayList<>();
            ConfigurationSection mobsSection = config.getConfigurationSection(wavePath + ".mobs");
            if (mobsSection != null) {
                for (String mobKey : mobsSection.getKeys(false)) {
                    String mobPath = wavePath + ".mobs." + mobKey;
                    MobSpawnData mobData = parseMobData(mobKey, mobPath);
                    if (mobData != null) {
                        mobSpawns.add(new MobSpawn(mobData));
                    }
                }
            }
            
            if (!mobSpawns.isEmpty()) {
                loadedWaves.add(new WaveDefinition(waveName, waveOrder, mobSpawns));
            }
        }
        
        // Load baby creeper general mob
        if (config.contains("waves.general.baby_creeper")) {
            String bcPath = "waves.general.baby_creeper";
            babyCreeper = parseMobData("baby_creeper", bcPath);
        }
        
        // Sort by order
        loadedWaves.sort(Comparator.comparingInt(WaveDefinition::getOrder));
        waves = loadedWaves;
        
        plugin.getLogger().info(() -> "Loaded " + waves.size() + " waves from config");
        return waves;
    }

    // Parses a mob configuration into a MobSpawnData object
    private MobSpawnData parseMobData(String mobId, String mobPath) {
        try {
            int amount = config.getInt(mobPath + ".amount", 1);
            int points = config.getInt(mobPath + ".points", 0);
            String entityType = config.getString(mobPath + ".type", "");
            String customName = config.getString(mobPath + ".mob-name", "");

            if (entityType.isEmpty()) {
                plugin.getLogger().warning(() -> "Mob " + mobId + " missing 'type' field");
                return null;
            }

            // Parse equipment
            Equipment equipment = parseEquipment(mobPath);

            // Parse attributes
            Map<String, Double> attributes = parseAttributes(mobPath);

            // Parse custom effects
            Map<String, Integer> customEffects = parseCustomEffects(mobPath);

            return new MobSpawnData(
                    mobId,
                    amount,
                    points,
                    entityType,
                    customName,
                    equipment,
                    attributes,
                    customEffects
            );

        } catch (Exception e) {
            plugin.getLogger().warning(() -> "Failed to parse mob " + mobId + ": " + e.getMessage());
            return null;
        }
    }

    // Parses armor and weapon equipment
    private Equipment parseEquipment(String mobPath) {
        Equipment.Builder builder = new Equipment.Builder();

        // Parse armor
        String armorType = config.getString(mobPath + ".armor", "");
        if (!armorType.isEmpty()) {
            builder.armorType(armorType);

            // Parse armor enchantments
            List<?> armorEnchants = config.getList(mobPath + ".armor-enchants");
            if (armorEnchants != null) {
                for (Object enchantObj : armorEnchants) {
                    if (enchantObj instanceof String) {
                        Enchantment enchant = Enchantment.parse((String) enchantObj);
                        if (enchant != null) {
                            builder.addArmorEnchant(enchant);
                        }
                    }
                }
            }
        }

        // Parse weapon
        String weapon = config.getString(mobPath + ".weapon", "");
        if (!weapon.isEmpty()) {
            builder.weapon(weapon);

            // Parse weapon enchantments
            List<?> weaponEnchants = config.getList(mobPath + ".weapon-enchants");
            if (weaponEnchants != null) {
                for (Object enchantObj : weaponEnchants) {
                    if (enchantObj instanceof String) {
                        Enchantment enchant = Enchantment.parse((String) enchantObj);
                        if (enchant != null) {
                            builder.addWeaponEnchant(enchant);
                        }
                    }
                }
            }
        }

        return builder.build();
    }

    // Parses attributes like health, speed, damage, etc.
    private Map<String, Double> parseAttributes(String mobPath) {
        Map<String, Double> attributes = new HashMap<>();
        ConfigurationSection attrSection = config.getConfigurationSection(mobPath + ".attributes");

        if (attrSection != null) {
            for (String key : attrSection.getKeys(false)) {
                double value = attrSection.getDouble(key);
                attributes.put(key, value);
            }
        }

        return attributes;
    }

    // Parses custom effects (potion effects)
    private Map<String, Integer> parseCustomEffects(String mobPath) {
        Map<String, Integer> effects = new HashMap<>();
        ConfigurationSection effectSection = config.getConfigurationSection(mobPath + ".custom-effects");

        if (effectSection != null) {
            for (String key : effectSection.getKeys(false)) {
                int amplifier = effectSection.getInt(key);
                effects.put(key, amplifier);
            }
        }

        return effects;
    }

    public long getWaveDuration() {
        return waveDuration;
    }

    public MobSpawnData getBabyCreeper() {
        return babyCreeper;
    }

    public List<WaveDefinition> getWaves() {
        return new ArrayList<>(waves);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Reload the configuration from file
     */
    public void reload() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        waveDuration = config.getLong("waves.wave-duration", 300);
        
        loadWaves();
        plugin.getLogger().info("Configuration reloaded successfully!");
    }
}
