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
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.jetbrains.annotations.Nullable;

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
    private final @NotNull Map<UUID, Team> playerTeams = new HashMap<>();
    private final @NotNull Map<Team, Triple<Azi, Azi, Azi>> points = new HashMap<>();
    private final @NotNull Set<Location> playerPlacedBlocks = new HashSet<>();
    private GamePhase phase = GamePhase.PRE_START;
    private int phaseTimer = 0;
    private World world = null;
    private boolean closed = false;

    public GameInstance(@NotNull CTAPlugin plugin, @NotNull MapConfig mapConfig) {
        this.plugin = plugin;
        this.mapConfig = mapConfig;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = this.scoreboard.registerNewObjective("capturetheazi", Criteria.DUMMY, Component.text("Capture The Azi"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (Team team : Team.values()) {
            var bukkitTeam = this.scoreboard.registerNewTeam(team.getScoreboardName());
            bukkitTeam.color(team.getColor());
            bukkitTeam.prefix(Component.text("", team.getColor()));
            bukkitTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.FOR_OWN_TEAM);
            bukkitTeam.setOption(org.bukkit.scoreboard.Team.Option.DEATH_MESSAGE_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.FOR_OWN_TEAM);
            bukkitTeam.setAllowFriendlyFire(false);
            bukkitTeam.setCanSeeFriendlyInvisibles(true);
            this.teamPlayers.put(team, new HashSet<>());
            this.points.put(team, Triple.of(new Azi(this, team), new Azi(this, team), new Azi(this, team)));
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
        Path worldsDir = Paths.get("worlds");
        Path worldDir = worldsDir.resolve(worldName);
        while (Files.exists(worldDir)) {
            worldName = "cta" + RandomUtil.randomDigits(4);
            worldDir = worldsDir.resolve(worldName);
        }
        Path worldZipFile = mapConfig.getWorldZipPath();
        String finalWorldName = worldName;
        Path finalWorldDir = worldDir;
        return CompletableFuture.runAsync(() -> {
            plugin.getSLF4JLogger().info("Unzipping world zip file ({}) to {}", worldZipFile, finalWorldDir);
            try {
                ZipUtil.unzip(worldZipFile, finalWorldDir);
            } catch (IOException e) {
                throw new RuntimeException("unzip failed (" + worldZipFile + " -> " + finalWorldDir + ")", e);
            }
        }, r -> Bukkit.getScheduler().runTaskAsynchronously(plugin, r)).thenRunAsync(() -> {
            world = Bukkit.createWorld(new WorldCreator(finalWorldName));
            if (world == null) {
                throw new RuntimeException("Failed to create world");
            }
            world.setGameRule(GameRules.LOCATOR_BAR, false);
            world.setGameRule(GameRules.ADVANCE_TIME, false);
            world.setGameRule(GameRules.ADVANCE_WEATHER, false);
            world.setGameRule(GameRules.SPAWN_MOBS, false);
            world.setGameRule(GameRules.SPAWN_MONSTERS, false);
            world.setGameRule(GameRules.SPAWN_PATROLS, false);
            world.setGameRule(GameRules.SPAWN_PHANTOMS, false);
            world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
            world.setGameRule(GameRules.KEEP_INVENTORY, true);
            teamPlayers.forEach((team, players) -> {
                Location spawnLocation = LocationUtil.toLocation(world, mapConfig.spawnPoints().get(team));
                for (UUID uuid : players) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) continue;
                    player.teleport(spawnLocation);
                }
            });
            setPhase(GamePhase.PREPARATION);
            this.runTaskTimer(plugin, 0, 20);
        }, r -> Bukkit.getScheduler().runTask(plugin, r));
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        setPhase(GamePhase.END);
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
        for (org.bukkit.scoreboard.Team team : scoreboard.getTeams()) {
            team.unregister();
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

    public boolean isAcceptingPlayers() {
        return phase.isAcceptingPlayers();
    }

    public boolean isClosed() {
        return closed;
    }

    public @Nullable Azi findAzi(@NotNull Item item) {
        for (Triple<Azi, Azi, Azi> triple : points.values()) {
            if (triple.getLeft().getEntity() == item) {
                return triple.getLeft();
            }
            if (triple.getMiddle().getEntity() == item) {
                return triple.getMiddle();
            }
            if (triple.getRight().getEntity() == item) {
                return triple.getRight();
            }
        }
        return null;
    }

    public @Nullable Azi getFirstAziOfStatus(@NotNull Azi.Status status, @NotNull Team team) {
        var triple = points.get(team);
        if (triple.getLeft().getStatus() == status) {
            return triple.getLeft();
        }
        if (triple.getMiddle().getStatus() == status) {
            return triple.getMiddle();
        }
        return triple.getRight().getStatus() == status ? triple.getRight() : null;
    }

    public @Nullable Azi getAzi(@NotNull Player player) {
        for (Map.Entry<Team, Triple<Azi, Azi, Azi>> entry : points.entrySet()) {
            var triple = entry.getValue();
            if (triple.getLeft().getHolder() == player) {
                return triple.getLeft();
            }
            if (triple.getMiddle().getHolder() == player) {
                return triple.getMiddle();
            }
            if (triple.getRight().getHolder() == player) {
                return triple.getRight();
            }
        }
        return null;
    }

    public @NotNull MapConfig getMapConfig() {
        return mapConfig;
    }

    public @NotNull Optional<World> getWorldOptional() {
        return Optional.ofNullable(world);
    }

    public @NotNull World getWorld() {
        return Objects.requireNonNull(world, "game is not started");
    }

    public @NotNull Scoreboard getScoreboard() {
        return scoreboard;
    }

    public @NotNull GamePhase getPhase() {
        return phase;
    }

    @Contract(pure = true)
    public @NotNull @UnmodifiableView Map<Team, Set<UUID>> getTeamPlayers() {
        return Collections.unmodifiableMap(teamPlayers);
    }

    @Contract(pure = true)
    public @NotNull @UnmodifiableView Map<UUID, Team> getPlayerTeams() {
        return Collections.unmodifiableMap(playerTeams);
    }

    public @NotNull org.bukkit.scoreboard.Team getScoreboardTeam(@NotNull Team team) {
        return Objects.requireNonNull(scoreboard.getTeam(team.getScoreboardName()), "Team " + team.name() + " is not registered in scoreboard");
    }

    public @NotNull Map<Team, Triple<Azi, Azi, Azi>> getPoints() {
        return points;
    }

    public @NotNull List<Player> getOnlinePlayers() {
        return world == null ? Collections.emptyList() : world.getPlayers();
    }

    public void broadcastMessage(@NotNull Component message) {
        for (Player player : getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    public void setPhase(@NotNull GamePhase phase) {
        Objects.requireNonNull(phase, "phase cannot be null");
        if (phase == this.phase) return;
        this.phase.onLeave(this);
        this.phase = phase;
        this.phase.onEnter(this);
    }

    public void addPlayer(@NotNull Team team, @NotNull Player player) {
        if (playerTeams.put(player.getUniqueId(), team) != null) {
            throw new IllegalStateException("Player " + player.getName() + " is already in a team");
        }
        teamPlayers.get(team).add(player.getUniqueId());
        playerTeams.put(player.getUniqueId(), team);
        player.setScoreboard(scoreboard);
        getScoreboardTeam(team).addEntry(player.getName());
    }

    public void removePlayer(@NotNull Team team, @NotNull UUID playerId, @Nullable Player player) {
        boolean removed = teamPlayers.get(team).remove(playerId);
        playerTeams.remove(playerId);
        if (!removed) return;
        Player p = player != null ? player : Bukkit.getPlayer(playerId);
        if (p != null) {
            Azi azi = getAzi(p);
            if (azi != null) azi.drop(p.getLocation());
            org.bukkit.scoreboard.Team bukkitTeam = getScoreboardTeam(team);
            bukkitTeam.removeEntry(p.getName());
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        } else {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
            org.bukkit.scoreboard.Team bukkitTeam = getScoreboardTeam(team);
            bukkitTeam.removeEntry(Objects.requireNonNull(offlinePlayer.getName()));
        }
    }

    public @Nullable Team getTeam(@NotNull UUID player) {
        return playerTeams.get(player);
    }

    public void unregisterPlayer(@NotNull UUID player) {
        for (Team team : teamPlayers.keySet()) {
            removePlayer(team, player, Bukkit.getPlayer(player));
        }
    }

    @Contract(pure = true)
    public @NotNull Team chooseTeam() {
        List<Map.Entry<Team, Integer>> teamPlayerCounts = new ArrayList<>();
        for (Map.Entry<Team, Set<UUID>> entry : teamPlayers.entrySet()) {
            teamPlayerCounts.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().size()));
        }
        teamPlayerCounts.sort(Map.Entry.comparingByValue());
        return teamPlayerCounts.getFirst().getKey();
    }

    public void addPlayerPlacedBlock(@NotNull Location location) {
        playerPlacedBlocks.add(location);
    }

    public boolean isPlayerPlacedBlock(@NotNull Location location) {
        return playerPlacedBlocks.contains(location);
    }

    @Override
    public void run() {
        if (closed) {
            cancel();
            return;
        }
        if (++phaseTimer >= phase.getDuration().toSeconds()) {
            phase = phase.next();
            phaseTimer = 0;
        }
    }
}
