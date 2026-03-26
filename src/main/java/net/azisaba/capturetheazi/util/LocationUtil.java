package net.azisaba.capturetheazi.util;

import io.papermc.paper.math.BlockPosition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class LocationUtil {
    /**
     * Determines whether a point defined by coordinates (x, y, z) lies within the specified bounds.
     *
     * @param x    the x-coordinate of the point to check
     * @param y    the y-coordinate of the point to check
     * @param z    the z-coordinate of the point to check
     * @param minX the minimum x-coordinate of the bounding box
     * @param maxX the maximum x-coordinate of the bounding box
     * @param minY the minimum y-coordinate of the bounding box
     * @param maxY the maximum y-coordinate of the bounding box
     * @param minZ the minimum z-coordinate of the bounding box
     * @param maxZ the maximum z-coordinate of the bounding box
     * @return true if the point (x, y, z) is within the bounds, false otherwise
     */
    public static boolean inBounds(double x, double y, double z, double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /**
     * Checks if the given location is within the bounds defined by two other locations.
     *
     * @param location the location to check
     * @param pos1     the first boundary location
     * @param pos2     the second boundary location
     * @return true if the location is within the bounds, false otherwise
     */
    public static boolean inBounds(@NotNull Location location, @NotNull Location pos1, @NotNull Location pos2) {
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return inBounds(location.getX(), location.getY(), location.getZ(), minX, maxX, minY, maxY, minZ, maxZ);
    }

    /**
     * Checks if the given location is within the bounds defined by two other locations.
     * @param location the location to check
     * @param min      the minimum boundary location
     * @param max      the maximum boundary location
     * @return true if the location is within the bounds, false otherwise
     */
    public static boolean inBoundsBlock(@NotNull Location location, @NotNull Location min, @NotNull Location max) {
        int minX = Math.min(min.getBlockX(), max.getBlockX());
        int maxX = Math.max(min.getBlockX(), max.getBlockX());
        int minY = Math.min(min.getBlockY(), max.getBlockY());
        int maxY = Math.max(min.getBlockY(), max.getBlockY());
        int minZ = Math.min(min.getBlockZ(), max.getBlockZ());
        int maxZ = Math.max(min.getBlockZ(), max.getBlockZ());
        return inBounds(location.getBlockX(), location.getBlockY(), location.getBlockZ(), minX, maxX, minY, maxY, minZ, maxZ);
    }

    public static @NotNull Location toLocation(@NotNull World world, @NotNull BlockPosition blockPosition) {
        return new Location(world, blockPosition.x() + 0.5, blockPosition.y(), blockPosition.z() + 0.5);
    }

    public static @NotNull Location toBlockLocation(@NotNull World world, @NotNull BlockPosition blockPosition) {
        return new Location(world, blockPosition.blockX(), blockPosition.blockY(), blockPosition.blockZ());
    }

    public static @NotNull Iterator<Location> iterate(@NotNull Location start, @NotNull Location end) {
        World world = start.getWorld();
        if (world == null || end.getWorld() == null || world != end.getWorld()) {
            throw new IllegalArgumentException("start and end must be in the same world");
        }

        final int minX = Math.min(start.getBlockX(), end.getBlockX());
        final int maxX = Math.max(start.getBlockX(), end.getBlockX());
        final int minY = Math.min(start.getBlockY(), end.getBlockY());
        final int maxY = Math.max(start.getBlockY(), end.getBlockY());
        final int minZ = Math.min(start.getBlockZ(), end.getBlockZ());
        final int maxZ = Math.max(start.getBlockZ(), end.getBlockZ());

        return new Iterator<>() {
            private int x = minX;
            private int y = minY;
            private int z = minZ;

            @Override
            public boolean hasNext() {
                return y <= maxY;
            }

            @Override
            public @NotNull Location next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                Location location = new Location(world, x, y, z);

                x++;
                if (x > maxX) {
                    x = minX;
                    z++;
                    if (z > maxZ) {
                        z = minZ;
                        y++;
                    }
                }

                return location;
            }
        };
    }

    public static void fill(@NotNull Location start, @NotNull Location end, @NotNull Material type) {
        LocationUtil.iterate(start, end).forEachRemaining(location -> location.getBlock().setType(type));
    }
}
