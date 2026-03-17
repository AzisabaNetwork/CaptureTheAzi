package net.azisaba.capturetheazi.util;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

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
}
