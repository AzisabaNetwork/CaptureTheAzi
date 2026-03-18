package net.azisaba.capturetheazi.listener;

import net.azisaba.capturetheazi.CTAPlugin;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public final class PlayerListener implements Listener {
    private final @NotNull CTAPlugin plugin;

    public PlayerListener(@NotNull CTAPlugin plugin) {
        this.plugin = plugin;
    }
}
