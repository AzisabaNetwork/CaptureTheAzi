package net.azisaba.capturetheazi;

import com.mojang.serialization.Codec;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public enum Team {
    RED(NamedTextColor.RED),
    BLUE(NamedTextColor.BLUE),
    ;

    public static final Codec<Team> CODEC = Codec.STRING.xmap(Team::valueOf, Team::name);
    private final NamedTextColor color;

    Team(NamedTextColor color) {
        this.color = color;
    }

    public @NotNull NamedTextColor getColor() {
        return color;
    }

    public @NotNull String getScoreboardName() {
        return name().toLowerCase();
    }

    public @NotNull String getDisplayName() {
        return name().substring(0, 1).toUpperCase() + name().toLowerCase().substring(1);
    }
}
