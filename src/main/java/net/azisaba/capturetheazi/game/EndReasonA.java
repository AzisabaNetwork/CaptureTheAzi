package net.azisaba.capturetheazi.game;

import net.azisaba.capturetheazi.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public record EndReasonA(@NotNull Type type, @NotNull Team team, @NotNull Component message) {
    public static @NotNull EndReasonA captured(@NotNull Team team) {
        return new EndReasonA(Type.CAPTURED, team, Component.text("アジがすべてキャプチャーされました！", NamedTextColor.GREEN));
    }

    public static @NotNull EndReasonA timeout(@NotNull Team team) {
        return new EndReasonA(Type.TIMEOUT, team, Component.text("ゲームが時間切れになりました！", NamedTextColor.GREEN));
    }

    public static @NotNull EndReasonA eliminated(@NotNull Team team) {
        return new EndReasonA(Type.ELIMINATED, team, Component.text("デスマッチフェーズで生き残りが1チームだけになりました！", NamedTextColor.GREEN));
    }

    public static @NotNull Component getWinnerComponent(@NotNull Team team) {
        return Component.text("勝者となったチームは", NamedTextColor.WHITE).append(Component.text(team.getDisplayName(), team.getColor())).append(Component.text("です！", NamedTextColor.WHITE));
    }

    enum Type {
        CAPTURED,
        TIMEOUT,
        ELIMINATED,
    }
}
