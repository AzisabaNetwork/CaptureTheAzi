package net.azisaba.capturetheazi.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import net.azisaba.capturetheazi.CTAPlugin;
import net.azisaba.capturetheazi.Team;
import net.azisaba.capturetheazi.config.MapConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

@SuppressWarnings("UnstableApiUsage")
public final class CTACommand implements BrigadierCommand {
    private final @NotNull CTAPlugin plugin;
    private final @NotNull Map<@NotNull UUID, @NotNull MapConfig> selectedMaps = new ConcurrentHashMap<>();

    public CTACommand(@NotNull CTAPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> create(@NotNull String name) {
        return literal(name)
                .requires(source -> source.getSender().hasPermission("capturetheazi.command") && source.getSender() instanceof Player)
                .then(literal("select")
                        .requires(source -> source.getSender().hasPermission("capturetheazi.select"))
                        .then(argument("mapName", StringArgumentType.string())
                                .suggests(suggestMaps(plugin))
                                .executes(ctx -> selectMap(ctx.getSource(), StringArgumentType.getString(ctx, "mapName")))
                        )
                )
                .then(literal("set")
                        .requires(source -> source.getSender().hasPermission("capturetheazi.set"))
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
                .build();
    }

    private @NotNull Player ensurePlayer(@NotNull CommandSourceStack source) {
        return (Player) source.getSender();
    }

    private @Nullable MapConfig ensureSelected(@NotNull CommandSourceStack source) {
        MapConfig config = selectedMaps.get(ensurePlayer(source).getUniqueId());
        if (config == null) {
            source.getSender().sendMessage(Component.text("No map selected", NamedTextColor.RED));
            return null;
        }
        return config;
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

    private int selectMap(@NotNull CommandSourceStack source, @NotNull String mapName) {
        try {
            MapConfig mapConfig = plugin.getMapConfigLoader().load(mapName);
            selectedMaps.put(ensurePlayer(source).getUniqueId(), mapConfig);
        } catch (Exception e) {
            source.getSender().sendMessage(Component.text("Failed to load map: " + e.getMessage(), NamedTextColor.RED));
            return 0;
        }
        source.getSender().sendMessage(Component.text("Selected map: " + mapName, NamedTextColor.GREEN));
        return 1;
    }

    private int setSpawn(@NotNull CommandSourceStack source, @NotNull Team team, @NotNull BlockPosition location) {
        MapConfig mapConfig = ensureSelected(source);
        if (mapConfig == null) return 0;
        return 1;
    }

    private int setGoal(@NotNull CommandSourceStack source, @NotNull Team team, @NotNull BlockPosition pos1, @NotNull BlockPosition pos2) {
        MapConfig mapConfig = ensureSelected(source);
        if (mapConfig == null) return 0;
        return 1;
    }
}
