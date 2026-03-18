package net.azisaba.capturetheazi.config;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.papermc.paper.math.BlockPosition;
import net.azisaba.capturetheazi.Team;
import net.azisaba.capturetheazi.codecs.ExtraCodecs;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public record MapConfig(
        @NotNull String id,
        @NotNull String worldName,
        @NotNull Optional<String> name,
        @NotNull Map<Team, BlockPosition> spawnPoints,
        @NotNull Map<Team, Pair<BlockPosition, BlockPosition>> goals
) {
    public static final Codec<MapConfig> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(
                    Codec.STRING.fieldOf("id").forGetter(MapConfig::id),
                    Codec.STRING.fieldOf("worldName").forGetter(MapConfig::worldName),
                    Codec.STRING.optionalFieldOf("name").forGetter(MapConfig::name),
                    Codec.unboundedMap(Team.CODEC, ExtraCodecs.BLOCK_POSITION).fieldOf("spawnPoints").forGetter(MapConfig::spawnPoints),
                    Codec.unboundedMap(Team.CODEC, Codec.pair(ExtraCodecs.BLOCK_POSITION, ExtraCodecs.BLOCK_POSITION)).fieldOf("goals").forGetter(MapConfig::goals)
            ).apply(builder, MapConfig::new)
    );

    @Contract(value = "null -> null; !null -> new", pure = true)
    public static MapConfig mutableCopyOf(@Nullable MapConfig config) {
        if (config == null) return null;
        return config.mutableCopy();
    }

    /**
     * Checks if the map can be loaded.
     * This method checks if the world zip file exists and contains the necessary files.
     * @return true if the world zip file exists and contains the necessary files, false otherwise.
     */
    public boolean canLoad() {
        return getMapUnavailableReason() == null;
    }

    /**
     * Determines the reason why a map is unavailable.
     * This method validates the existence and integrity of the associated world zip file.
     * The method checks if the world zip file exists, is not corrupted, and contains the required files,
     * such as `level.dat` and the `region` folder.
     *
     * @return A string describing the reason the map is unavailable, or null if the map is valid and available.
     */
    public @Nullable String getMapUnavailableReason() {
        Path worldZipPath = getWorldZipPath();
        try {
            if (!Files.isRegularFile(worldZipPath)) {
                return "World zip file (" + worldZipPath + ") does not exist or is corrupted";
            }
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(worldZipPath))) {
                Set<String> entryNames = new HashSet<>();
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    entryNames.add(entry.getName());
                }
                // Validate that the world contains the necessary files
                if (!entryNames.contains("level.dat")) {
                    return "World zip file (" + worldZipPath + ") does not contain level.dat";
                }
                boolean foundRegion = false;
                for (String entryName : entryNames) {
                    if (entryName.startsWith("region/")) {
                        foundRegion = true;
                        break;
                    }
                }
                if (!foundRegion) {
                    return "World zip file (" + worldZipPath + ") does not contain region folder";
                }
            }
        } catch (Exception e) {
            return "Failed to validate world zip file (" + worldZipPath + "): " + e.getMessage();
        }
        return null;
    }

    @Contract(pure = true)
    public @NotNull Path getWorldZipPath() {
        return Paths.get("worlds", worldName + ".zip");
    }

    @Contract(value = "-> new", pure = true)
    public @NotNull MapConfig mutableCopy() {
        return new MapConfig(id, worldName, name, new HashMap<>(spawnPoints), new HashMap<>(goals));
    }

    @Contract(value = "_ -> new", pure = true)
    public @NotNull MapConfig mutableCopyWithName(@NotNull String name) {
        return new MapConfig(id, worldName, Optional.of(name), new HashMap<>(spawnPoints), new HashMap<>(goals));
    }
}
