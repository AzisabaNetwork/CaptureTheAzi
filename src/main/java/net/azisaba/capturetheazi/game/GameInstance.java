package net.azisaba.capturetheazi.game;

import net.azisaba.capturetheazi.CTAPlugin;
import net.azisaba.capturetheazi.GamePhase;
import net.azisaba.capturetheazi.Team;
import net.azisaba.capturetheazi.config.MapConfig;
import net.azisaba.capturetheazi.util.LocationUtil;
import net.azisaba.capturetheazi.util.RandomUtil;
import net.azisaba.capturetheazi.util.ZipUtil;
import net.kyori.adventure.text.Component;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class GameInstance extends BukkitRunnable implements AutoCloseable {
    private final @NotNull CTAPlugin plugin;
    private final @NotNull MapConfig mapConfig;
    private final Scoreboard scoreboard;
    private final @NotNull Map<Team, Set<UUID>> teamPlayers = new HashMap<>();
    private GamePhase phase = GamePhase.PRE_START;
    private World world = null;
    private boolean closed = false;

    public GameInstance(@NotNull CTAPlugin plugin, @NotNull MapConfig mapConfig) {
        this.plugin = plugin;
        this.mapConfig = mapConfig;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = this.scoreboard.registerNewObjective("capturetheazi", Criteria.DUMMY, Component.text("Capture The Azi"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (Team team : Team.values()) {
            this.scoreboard.registerNewTeam(team.name().toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Starts the game. Do not call {@link CompletableFuture#join()} on the future returned from this method in the
     * main thread, as it will cause the deadlock.
     * @return future
     */
    public @NotNull CompletableFuture<Void> startAsync() {
        if (mapConfig.goals().get(Team.RED) == null || mapConfig.goals().get(Team.BLUE) == null) {
            throw new IllegalStateException("Map configuration is incomplete: missing goals for red and/or blue teams");
        }
        if (mapConfig.spawnPoints().get(Team.RED) == null || mapConfig.spawnPoints().get(Team.BLUE) == null) {
            throw new IllegalStateException("Map configuration is incomplete: missing spawn points for red and/or blue teams");
        }
        String unavailableReason = mapConfig.getMapUnavailableReason();
        if (unavailableReason != null) {
            throw new IllegalStateException(unavailableReason);
        }
        String worldName = "cta" + RandomUtil.randomDigits(4);
        return CompletableFuture.runAsync(() -> {
            Path worldsDir = Paths.get("worlds");
            Path worldDir = worldsDir.resolve(worldName);
            Path worldZipFile = mapConfig.getWorldZipPath();
            plugin.getSLF4JLogger().info("Unzipping world zip file ({}) to {}", worldZipFile, worldDir);
            try {
                ZipUtil.unzip(worldZipFile, worldDir);
            } catch (IOException e) {
                throw new RuntimeException("unzip failed (" + worldZipFile + " -> " + worldDir + ")", e);
            }
        }, r -> Bukkit.getScheduler().runTaskAsynchronously(plugin, r)).thenRunAsync(() -> {
            world = Bukkit.createWorld(new WorldCreator(worldName));
            teamPlayers.forEach((team, players) -> {
                Location spawnLocation = LocationUtil.toLocation(world, mapConfig.spawnPoints().get(team));
                for (UUID uuid : players) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) continue;
                    player.teleport(spawnLocation);
                }
            });
            phase = GamePhase.PREPARATION;
            this.runTaskTimer(plugin, 0, 20);
        }, r -> Bukkit.getScheduler().runTask(plugin, r));
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        phase = GamePhase.END;
        try {
            cancel();
        } catch (IllegalStateException ignored) {}
        if (world == null) return;
        for (Player player : world.getPlayers()) {
            player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
        }
        Bukkit.unloadWorld(world, true);
        for (Objective objective : scoreboard.getObjectives()) {
            objective.unregister();
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // zip world
            Path worldsDir = Paths.get("worlds");
            Path worldDir = worldsDir.resolve(world.getName());
            Path worldZipFile = worldsDir.resolve("closed").resolve(System.currentTimeMillis() + "-" + world.getName() + ".zip");
            try {
                Files.createDirectories(worldZipFile.getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            plugin.getSLF4JLogger().info("Zipping world ({}) to {}", worldDir, worldZipFile);
            try {
                ZipUtil.zip(worldZipFile, worldDir);
            } catch (IOException e) {
                throw new RuntimeException("zip failed (" + worldDir + " -> " + worldZipFile + ")", e);
            }
            // delete world recursively
            try {
                FileUtils.deleteDirectory(worldDir.toFile());
            } catch (IOException e) {
                throw new RuntimeException("delete failed (" + worldDir + ")", e);
            }
            plugin.getSLF4JLogger().info("World ({}) archived and deleted", worldDir);
        });
    }

    public boolean isClosed() {
        return closed;
    }

    public @NotNull Optional<World> getWorldOptional() {
        return Optional.ofNullable(world);
    }

    public @NotNull World getWorld() {
        return Objects.requireNonNull(world, "game is not started");
    }

    public @NotNull Scoreboard getScoreboard() {
        return Objects.requireNonNull(scoreboard, "game is not started");
    }

    public void addPlayer(@NotNull Team team, @NotNull Player player) {
        teamPlayers.computeIfAbsent(team, t -> new HashSet<>()).add(player.getUniqueId());
        player.setScoreboard(scoreboard);
        Objects.requireNonNull(scoreboard.getTeam(team.name().toLowerCase(Locale.ROOT)), "Team " + team.name() + " is not registered in scoreboard").addEntry(player.getName());
    }

    public void removePlayer(@NotNull Team team, @NotNull UUID player) {
        boolean removed = teamPlayers.computeIfAbsent(team, t -> new HashSet<>()).remove(player);
        if (!removed) return;
        Player p = Bukkit.getPlayer(player);
        if (p != null) {
            org.bukkit.scoreboard.Team bukkitTeam = scoreboard.getTeam(team.name());
            if (bukkitTeam != null) bukkitTeam.removeEntry(p.getName());
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player);
            org.bukkit.scoreboard.Team bukkitTeam = scoreboard.getTeam(team.name());
            if (bukkitTeam != null) bukkitTeam.removeEntry(Objects.requireNonNull(offlinePlayer.getName()));
        }
    }

    public void unregisterPlayer(@NotNull UUID player) {
        for (Team team : teamPlayers.keySet()) {
            removePlayer(team, player);
        }
    }

    @Override
    public void run() {
        if (closed) {
            cancel();
            return;
        }
    }
}
