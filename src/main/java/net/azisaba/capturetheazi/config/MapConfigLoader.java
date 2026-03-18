package net.azisaba.capturetheazi.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.azisaba.capturetheazi.CTAPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

public final class MapConfigLoader {
    private final Gson gson = new Gson();
    private final @NotNull CTAPlugin plugin;

    public MapConfigLoader(@NotNull CTAPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Retrieves a set of map names from the "maps" directory.
     * The method scans the directory for JSON configuration files and extracts the map names by removing the ".json" extension.
     *
     * @return a non-null set of non-null strings representing the names of the maps stored in the "maps" directory.
     */
    public @NotNull Set<@NotNull String> listMaps() {
        Set<String> maps = new HashSet<>();
        File mapsDir = new File(plugin.getDataFolder(), "maps");
        if (mapsDir.exists()) {
            for (File file : Objects.requireNonNull(mapsDir.listFiles(file -> Objects.requireNonNull(file.getName()).endsWith(".json")), "Maps directory does not exist or is error")) {
                maps.add(file.getName().substring(0, file.getName().length() - ".json".length()));
            }
        }
        return maps;
    }

    /**
     * Loads all map configurations from the "maps" directory.
     * This method scans the directory for files with the ".json" extension,
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
        for (File file : Objects.requireNonNull(mapsDir.listFiles(file -> Objects.requireNonNull(file.getName()).endsWith(".json")), "Maps directory does not exist or is error")) {
            configs.add(load(file.getName().substring(0, file.getName().length() - ".json".length())));
        }
        return configs;
    }

    /**
     * Loads a map configuration file based on the given map ID.
     * The method looks for a JSON configuration file in the "maps" directory, parses it,
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
        File file = new File(plugin.getDataFolder(), "maps/" + id + ".json");
        if (!file.exists()) {
            throw new FileNotFoundException("Map config not found: " + plugin.getDataFolder() + "/maps/" + id + ".json");
        }
        JsonElement json = gson.fromJson(new FileReader(file), JsonElement.class);
        DataResult<MapConfig> result = MapConfig.CODEC.parse(JsonOps.INSTANCE, json);
        return result.resultOrPartial(plugin.getSLF4JLogger()::error).orElseThrow();
    }

    /**
     * Saves the specified map configuration to a JSON file.
     * The configuration is stored in the "maps" directory with the filename based on the map ID.
     *
     * @param config the {@link MapConfig} object containing the map configuration to save
     * @throws IOException if an I/O error occurs while saving the configuration file
     */
    public void save(@NotNull MapConfig config) throws IOException {
        File file = new File(plugin.getDataFolder(), "maps/" + config.id() + ".json");
        DataResult<JsonElement> result = MapConfig.CODEC.encodeStart(JsonOps.INSTANCE, config);
        JsonElement json = result.resultOrPartial(plugin.getSLF4JLogger()::error).orElseThrow();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(json, writer);
        }
    }

    /**
     * Creates a new {@link MapConfig} object with the specified map ID and world name,
     * saves it to the "maps" directory as a JSON file, and returns the created configuration.
     *
     * @param id the unique identifier for the map being created; must not be null
     * @param worldName the name of the world associated with the map; must not be null
     * @return a {@link MapConfig} object representing the newly created map configuration
     * @throws IOException if an I/O error occurs while saving the configuration file
     * @throws IllegalArgumentException if a map configuration with the specified ID already exists
     */
    @Contract(value = "_, _ -> new")
    public @NotNull MapConfig create(@NotNull String id, @NotNull String worldName) throws IOException {
        File file = new File(plugin.getDataFolder(), "maps/" + id + ".json");
        if (file.exists()) {
            throw new IllegalArgumentException("Map config already exists: " + plugin.getDataFolder() + "/maps/" + id + ".json");
        }
        MapConfig config = new MapConfig(id, worldName, Optional.empty(), Map.of(), Map.of());
        save(config);
        return config;
    }
}
