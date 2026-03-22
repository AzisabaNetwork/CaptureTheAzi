package net.azisaba.capturetheazi;

import net.azisaba.capturetheazi.game.GameInstance;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.function.Consumer;

public enum GamePhase {
    PRE_START(Duration.ZERO, (gameInstance) -> {}, (gameInstance) -> {}),
    PREPARATION(Duration.ofMinutes(5), (gameInstance) -> {}, (gameInstance) -> {}),
    ACTION(Duration.ofMinutes(15), (gameInstance) -> {}, (gameInstance) -> {}),
    END(Duration.ofMinutes(1), (gameInstance) -> {}, (gameInstance) -> {}),
    ;

    private final @NotNull Duration duration;
    private final @NotNull Consumer<GameInstance> onEnter;
    private final @NotNull Consumer<GameInstance> onLeave;

    GamePhase(@NotNull Duration duration, @NotNull Consumer<GameInstance> onEnter, @NotNull Consumer<GameInstance> onLeave) {
        this.duration = duration;
        this.onEnter = onEnter;
        this.onLeave = onLeave;
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
}
