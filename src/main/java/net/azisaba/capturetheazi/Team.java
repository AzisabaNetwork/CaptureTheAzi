package net.azisaba.capturetheazi;

import com.mojang.serialization.Codec;

public enum Team {
    RED,
    BLUE,
    ;

    public static final Codec<Team> CODEC = Codec.STRING.xmap(Team::valueOf, Team::name);
}
