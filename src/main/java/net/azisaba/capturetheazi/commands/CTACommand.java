package net.azisaba.capturetheazi.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.datafixers.util.Pair;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import net.azisaba.capturetheazi.CTAPlugin;
import net.azisaba.capturetheazi.Team;
import net.azisaba.capturetheazi.config.MapConfig;
import net.azisaba.capturetheazi.game.GameInstance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

@SuppressWarnings("UnstableApiUsage")
public final class CTACommand implements BrigadierCommand {
    private final @NotNull CTAPlugin plugin;
    private final @NotNull Map<@NotNull UUID, @NotNull String> selectedMaps = new ConcurrentHashMap<>();

    public CTACommand(@NotNull CTAPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> create(@NotNull String name) {
        return literal(name)
                .requires(source -> source.getSender().hasPermission("capturetheazi.command") && source.getSender() instanceof Player)
                .then(literal("createmap")
                        .requires(source -> source.getSender().hasPermission("capturetheazi.createmap"))
                        .then(argument("id", StringArgumentType.string())
                                .then(argument("worldName", StringArgumentType.string())
                                        .executes(ctx -> createMap(ctx.getSource(), StringArgumentType.getString(ctx, "id"), StringArgumentType.getString(ctx, "worldName")))
                                )
                        )
                )
                .then(literal("select")
                        .requires(source -> source.getSender().hasPermission("capturetheazi.select"))
                        .then(argument("mapName", StringArgumentType.string())
                                .suggests(suggestMaps(plugin))
                                .executes(ctx -> selectMap(ctx.getSource(), StringArgumentType.getString(ctx, "mapName")))
                        )
                )
                .then(literal("listmaps")
                        .requires(source -> source.getSender().hasPermission("capturetheazi.listmaps"))
                        .executes(ctx -> listMaps(ctx.getSource()))
                )
                .then(literal("start")
                        .requires(source -> source.getSender().hasPermission("capturetheazi.start"))
                        .executes(ctx -> startGame(ctx.getSource()))
                )
                .then(literal("endall")
                        .requires(source -> source.getSender().hasPermission("capturetheazi.endall"))
                        .executes(ctx -> endAllGames(ctx.getSource()))
                )
                .then(literal("set")
                        .requires(source -> source.getSender().hasPermission("capturetheazi.set"))
                        .then(literal("name")
                                .requires(source -> source.getSender().hasPermission("capturetheazi.set.name"))
                                .then(argument("name", StringArgumentType.string())
                                        .executes(ctx -> setName(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                                )
                        )
                        .then(literal("spawn")
                                .requires(source -> source.getSender().hasPermission("capturetheazi.set.spawn"))
                                .then(literal("red")
                                        .then(argument("location", ArgumentTypes.blockPosition())
                                                .executes(ctx -> setSpawn(ctx.getSource(), Team.RED, getBlockPos(ctx, "location")))
                                        )
                                )
                                .then(literal("blue")
                                        .then(argument("location", ArgumentTypes.blockPosition())
                                                .executes(ctx -> setSpawn(ctx.getSource(), Team.BLUE, getBlockPos(ctx, "location")))
                                        )
                                )
                        )
                        .then(literal("goal")
                                .requires(source -> source.getSender().hasPermission("capturetheazi.set.goal"))
                                .then(literal("red")
                                        .then(argument("pos1", ArgumentTypes.blockPosition())
                                                .then(argument("pos2", ArgumentTypes.blockPosition())
                                                        .executes(ctx -> setGoal(ctx.getSource(), Team.RED, getBlockPos(ctx, "pos1"), getBlockPos(ctx, "pos2")))
                                                )
                                        )
                                )
                                .then(literal("blue")
                                        .then(argument("pos1", ArgumentTypes.blockPosition())
                                                .then(argument("pos2", ArgumentTypes.blockPosition())
                                                        .executes(ctx -> setGoal(ctx.getSource(), Team.BLUE, getBlockPos(ctx, "pos1"), getBlockPos(ctx, "pos2")))
                                                )
                                        )
                                )
                        )
                )
                .then(literal("game")
                        .requires(source -> source.getSender().hasPermission("capturetheazi.game"))
                        .then(literal("join")
                                .requires(source -> source.getSender().hasPermission("capturetheazi.game.join"))
                                .then(literal("blue")
                                        .executes(ctx -> joinGame(ctx.getSource(), Team.BLUE))
                                )
                                .then(literal("red")
                                        .executes(ctx -> joinGame(ctx.getSource(), Team.RED))
                                )
                        )
                )
                .then(literal("leave")
                        .requires(source -> source.getSender().hasPermission("capturetheazi.leave"))
                        .executes(ctx -> leaveGame(ctx.getSource()))
                )
                .build();
    }

    private @NotNull Player ensurePlayer(@NotNull CommandSourceStack source) {
        return (Player) source.getSender();
    }

    private @Nullable MapConfig ensureSelected(@NotNull CommandSourceStack source) {
        String mapId = selectedMaps.get(ensurePlayer(source).getUniqueId());
        try {
            MapConfig config = mapId == null ? null : plugin.getMapConfigLoader().load(mapId);
            if (config == null) {
                source.getSender().sendMessage(Component.text("No map selected", NamedTextColor.RED));
                return null;
            }
            return config;
        } catch (Exception e) {
            source.getSender().sendMessage(Component.text("No map selected", NamedTextColor.RED));
            return null;
        }
    }

    private @Nullable GameInstance ensureInGame(@NotNull CommandSourceStack source) {
        Player player = ensurePlayer(source);
        for (GameInstance gameInstance : plugin.getGameInstances()) {
            if (player.getWorld() == gameInstance.getWorldOptional().orElse(null)) {
                return gameInstance;
            }
        }
        source.getSender().sendMessage(Component.text("ゲームに参加していないため、このコマンドは使用できません", NamedTextColor.RED));
        return null;
    }

    private @NotNull BlockPosition getBlockPos(@NotNull CommandContext<CommandSourceStack> ctx, @NotNull String name) throws CommandSyntaxException {
        BlockPositionResolver resolver = ctx.getArgument(name, BlockPositionResolver.class);
        return resolver.resolve(ctx.getSource());
    }

    private @NotNull SuggestionProvider<CommandSourceStack> suggestMaps(@NotNull CTAPlugin plugin) {
        return (ctx, builder) -> {
            plugin.getMapConfigLoader().listMaps().forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private boolean saveMap(@NotNull CommandSourceStack source, @NotNull MapConfig config) {
        try {
            plugin.getMapConfigLoader().save(config);
            return true;
        } catch (IOException e) {
            source.getSender().sendMessage(Component.text("Failed to save map: " + e.getMessage(), NamedTextColor.RED));
            return false;
        }
    }

    private int createMap(@NotNull CommandSourceStack source, @NotNull String id, @NotNull String worldName) {
        try {
            plugin.getMapConfigLoader().create(id, worldName);
            source.getSender().sendMessage(Component.text("Map created: " + id + " (world: " + worldName + ")", NamedTextColor.GREEN));
            return 1;
        } catch (Exception e) {
            source.getSender().sendMessage(Component.text("Failed to create map: " + e.getMessage(), NamedTextColor.RED));
            return 0;
        }
    }

    private int selectMap(@NotNull CommandSourceStack source, @NotNull String mapName) {
        try {
            MapConfig mapConfig = plugin.getMapConfigLoader().load(mapName);
            selectedMaps.put(ensurePlayer(source).getUniqueId(), mapConfig.id());
        } catch (Exception e) {
            source.getSender().sendMessage(Component.text("Failed to load map: " + e.getMessage(), NamedTextColor.RED));
            return 0;
        }
        source.getSender().sendMessage(Component.text("Selected map: " + mapName, NamedTextColor.GREEN));
        return 1;
    }

    private int listMaps(@NotNull CommandSourceStack source) {
        source.getSender().sendMessage(Component.text("Available maps: " + plugin.getMapConfigLoader().listMaps(), NamedTextColor.GREEN));
        return 1;
    }

    private int startGame(@NotNull CommandSourceStack source) {
        MapConfig mapConfig = ensureSelected(source);
        if (mapConfig == null) return 0;
        source.getSender().sendMessage(Component.text("Starting game...", NamedTextColor.YELLOW));
        GameInstance gameInstance = new GameInstance(plugin, mapConfig);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                gameInstance.startAsync().join();
                plugin.getGameInstances().add(gameInstance);
                source.getSender().sendMessage(Component.text("Game started", NamedTextColor.GREEN));
            } catch (Exception e) {
                source.getSender().sendMessage(Component.text("Failed to start game: " + e.getMessage(), NamedTextColor.RED));
                plugin.getSLF4JLogger().error("Failed to start game", e);
                gameInstance.close();
                plugin.getGameInstances().remove(gameInstance);
            }
        });
        return 1;
    }

    private int endAllGames(@NotNull CommandSourceStack source) {
        plugin.getGameInstances().forEach(GameInstance::close);
        plugin.getGameInstances().clear();
        source.getSender().sendMessage(Component.text("All games ended", NamedTextColor.GREEN));
        return 1;
    }

    private int setName(@NotNull CommandSourceStack source, @NotNull String name) {
        MapConfig mapConfig = ensureSelected(source);
        if (mapConfig == null) return 0;
        if (!saveMap(source, mapConfig.mutableCopyWithName(name))) return 0;
        source.getSender().sendMessage(Component.text("Map name set to " + name, NamedTextColor.GREEN));
        return 1;
    }

    private int setSpawn(@NotNull CommandSourceStack source, @NotNull Team team, @NotNull BlockPosition location) {
        MapConfig mapConfig = MapConfig.mutableCopyOf(ensureSelected(source));
        if (mapConfig == null) return 0;
        mapConfig.spawnPoints().put(team, location);
        if (!saveMap(source, mapConfig)) return 0;
        source.getSender().sendMessage(Component.text("Spawn point set for " + team.name(), NamedTextColor.GREEN));
        return 1;
    }

    private int setGoal(@NotNull CommandSourceStack source, @NotNull Team team, @NotNull BlockPosition pos1, @NotNull BlockPosition pos2) {
        MapConfig mapConfig = MapConfig.mutableCopyOf(ensureSelected(source));
        if (mapConfig == null) return 0;
        mapConfig.goals().put(team, Pair.of(pos1, pos2));
        if (!saveMap(source, mapConfig)) return 0;
        source.getSender().sendMessage(Component.text("Goal set for " + team.name(), NamedTextColor.GREEN));
        return 1;
    }

    private int joinGame(@NotNull CommandSourceStack source, @NotNull Team team) {
        GameInstance gameInstance = ensureInGame(source);
        if (gameInstance == null) return 0;
        Player player = ensurePlayer(source);
        gameInstance.addPlayer(team, player);
        source.getSender().sendMessage(Component.text("Joined game as " + team.name(), NamedTextColor.GREEN));
        return 1;
    }

    private int leaveGame(@NotNull CommandSourceStack source) {
        GameInstance gameInstance = ensureInGame(source);
        if (gameInstance == null) return 0;
        Player player = ensurePlayer(source);
        gameInstance.unregisterPlayer(player.getUniqueId());
        source.getSender().sendMessage(Component.text("ゲームを抜けました。", NamedTextColor.GREEN));
        return 1;
    }
}
