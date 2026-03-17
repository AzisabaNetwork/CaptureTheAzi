package net.azisaba.capturetheazi.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public record MapConfig(
        @NotNull String id,
        @NotNull String worldName,
        @Nullable String name
) {
    /**
     * Checks if the map can be loaded.
     * This method checks if the world zip file exists and contains the necessary files.
     * @return true if the world zip file exists and contains the necessary files, false otherwise.
     */
    public boolean canLoad() {
        try {
            Path worldZipPath = Paths.get("worlds", worldName + ".zip");
            if (!Files.isRegularFile(worldZipPath)) {
                return false;
            }
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(worldZipPath))) {
                Set<String> entryNames = new HashSet<>();
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    entryNames.add(entry.getName());
                }
                // Validate that the world contains the necessary files
                return entryNames.contains("level.dat") && entryNames.contains("region");
            }
        } catch (Exception e) {
            return false;
        }
    }
}
