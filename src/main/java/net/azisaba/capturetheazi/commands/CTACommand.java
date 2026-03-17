package net.azisaba.capturetheazi.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

public final class CTACommand implements BrigadierCommand {
    @Override
    public LiteralCommandNode<CommandSourceStack> create(@NotNull String name) {
        return literal(name)
                .build();
    }
}
