package net.azisaba.capturetheazi.codecs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;

public class ExtraCodecs {
    public static final Codec<BlockPosition> BLOCK_POSITION =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.INT.fieldOf("x").forGetter(BlockPosition::blockX),
                            Codec.INT.fieldOf("y").forGetter(BlockPosition::blockY),
                            Codec.INT.fieldOf("z").forGetter(BlockPosition::blockZ)
                    ).apply(instance, Position::block));
}
