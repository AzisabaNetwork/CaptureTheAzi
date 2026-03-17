package net.azisaba.capturetheazi.config;

import net.azisaba.capturetheazi.CTAPlugin;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MapConfigLoader {
    private final @NotNull CTAPlugin plugin;

    public MapConfigLoader(@NotNull CTAPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads all map configurations from the "maps" directory.
     * This method scans the directory for files with the ".yml" extension,
     * parses them, and creates a list of {@link MapConfig} objects.
     *
     * @return a list of {@link MapConfig} objects representing the loaded map configurations
     * @throws IOException if an I/O error occurs during the loading of map configurations
     */
    @Contract(pure = true)
    public @NotNull List<@NotNull MapConfig> loadAll() throws IOException {
        File mapsDir = new File(plugin.getDataFolder(), "maps");
        //noinspection ResultOfMethodCallIgnored
        mapsDir.mkdirs();
        List<MapConfig> configs = new ArrayList<>();
        for (File file : Objects.requireNonNull(mapsDir.listFiles(file -> Objects.requireNonNull(file.getName()).endsWith(".yml")), "Maps directory does not exist or is error")) {
            configs.add(load(file.getName().substring(0, file.getName().length() - ".yml".length())));
        }
        return configs;
    }

    /**
     * Loads a map configuration file based on the given map ID.
     * The method looks for a YAML configuration file in the "maps" directory, parses it,
     * and creates a {@link MapConfig} object representing the configuration.
     *
     * @param id the unique identifier of the map whose configuration is to be loaded
     * @return a {@link MapConfig} object containing the loaded configuration data
     * @throws IOException if an I/O error occurs while accessing the map configuration file
     * @throws FileNotFoundException if the specified map configuration file does not exist
     * @throws RuntimeException if the configuration file is invalid or missing required fields
     */
    @Contract(pure = true)
    public @NotNull MapConfig load(@NotNull String id) throws IOException {
        File file = new File(plugin.getDataFolder(), "maps/" + id + ".yml");
        if (!file.exists()) {
            throw new FileNotFoundException("Map config not found: " + plugin.getDataFolder() + "/maps/" + id + ".yml");
        }
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
        String worldName = config.getString("world");
        if (worldName == null) {
            throw new RuntimeException("World name not specified in map config: " + file.getName());
        }
        String name = config.getString("name");
        return new MapConfig(id, worldName, name);
    }

    /**
     * Saves the specified map configuration to a YAML file.
     * The configuration is stored in the "maps" directory with the filename based on the map ID.
     *
     * @param config the {@link MapConfig} object containing the map configuration to save
     * @throws IOException if an I/O error occurs while saving the configuration file
     */
    public void save(@NotNull MapConfig config) throws IOException {
        File file = new File(plugin.getDataFolder(), "maps/" + config.id() + ".yml");
        YamlConfiguration configFile = new YamlConfiguration();
        configFile.set("world", config.worldName());
        configFile.set("name", config.name());
        configFile.save(file);
    }
}
