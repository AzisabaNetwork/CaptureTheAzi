package net.azisaba.capturetheazi;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public enum GamePhase {
    PRE_START(Duration.ZERO),
    PREPARATION(Duration.ofMinutes(5)),
    ACTION(Duration.ofMinutes(15)),
    END(Duration.ofMinutes(1)),
    ;

    private final @NotNull Duration duration;

    GamePhase(@NotNull Duration duration) {
        this.duration = duration;
    }

    public @NotNull Duration getDuration() {
        return duration;
    }
}
