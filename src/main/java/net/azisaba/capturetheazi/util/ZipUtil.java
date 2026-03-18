package net.azisaba.capturetheazi.util;

import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class ZipUtil {
    public static void unzip(@NotNull Path zipFile, @NotNull Path targetDir) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                Path target = targetDir.resolve(entry.getName());
                Files.createDirectories(target.getParent());
                try (var out = new FileOutputStream(target.toFile())) {
                    zip.transferTo(out);
                }
            }
        }
    }

    public static void zip(@NotNull Path zipFile, @NotNull Path sourceDir) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            try (Stream<Path> stream = Files.walk(sourceDir)) {
                stream.forEach(path -> {
                    try {
                        if (Files.isDirectory(path)) {
                            ZipEntry e = new ZipEntry(sourceDir.relativize(path).toString());
                            if (e.isDirectory()) {
                                e.setMethod(ZipEntry.STORED);
                                e.setSize(0);
                                e.setCrc(0);
                            }
                            zip.putNextEntry(e);
                        } else {
                            zip.putNextEntry(new ZipEntry(sourceDir.relativize(path).toString()));
                            Files.copy(path, zip);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }
}
