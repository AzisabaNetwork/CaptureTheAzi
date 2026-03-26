package net.azisaba.capturetheazi;

import net.azisaba.capturetheazi.game.GameInstance;
import net.azisaba.capturetheazi.util.LocationUtil;
import org.bukkit.Material;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.function.Consumer;

public enum GamePhase {
    PRE_START(true, Duration.ZERO, (gameInstance) -> {}, (gameInstance) -> {}),
    PREPARATION(true, Duration.ofMinutes(5), (gameInstance) -> {
        var wall = gameInstance.getMapConfig().wall().orElseThrow();
        LocationUtil.fill(wall.getFirst().toLocation(gameInstance.getWorld()), wall.getSecond().toLocation(gameInstance.getWorld()), Material.COAL_BLOCK);
    }, (gameInstance) -> {}),
    ACTION(false, Duration.ofMinutes(15), (gameInstance) -> {
        var wall = gameInstance.getMapConfig().wall().orElseThrow();
        LocationUtil.fill(wall.getFirst().toLocation(gameInstance.getWorld()), wall.getSecond().toLocation(gameInstance.getWorld()), Material.AIR);
    }, (gameInstance) -> {}),
    //DEATH_MATCH(false, Duration.ofMinutes(10), (gameInstance) -> {}, (gameInstance) -> {}),
    END(false, Duration.ofMinutes(1), (gameInstance) -> {}, (gameInstance) -> {}),
    ;

    private final boolean isAcceptingPlayers;
    private final @NotNull Duration duration;
    private final @NotNull Consumer<GameInstance> onEnter;
    private final @NotNull Consumer<GameInstance> onLeave;

    GamePhase(boolean isAcceptingPlayers, @NotNull Duration duration, @NotNull Consumer<GameInstance> onEnter, @NotNull Consumer<GameInstance> onLeave) {
        this.isAcceptingPlayers = isAcceptingPlayers;
        this.duration = duration;
        this.onEnter = onEnter;
        this.onLeave = onLeave;
    }

    public boolean isAcceptingPlayers() {
        return isAcceptingPlayers;
    }

    public boolean canPickupAzi() {
        return this == ACTION;
    }

    public @NotNull Duration getDuration() {
        return duration;
    }

    public void onEnter(@NotNull GameInstance game) {
        onEnter.accept(game);
    }

    public void onLeave(@NotNull GameInstance game) {
        onLeave.accept(game);
    }

    @Contract(pure = true)
    public @NotNull GamePhase next() {
        if (ordinal() + 1 >= values().length) {
            return END;
        }
        return values()[ordinal() + 1];
    }
}
