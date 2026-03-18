package net.azisaba.capturetheazi;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.azisaba.capturetheazi.commands.CTACommand;
import net.azisaba.capturetheazi.config.MapConfigLoader;
import net.azisaba.capturetheazi.game.GameInstance;
import net.azisaba.capturetheazi.listener.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class CTAPlugin extends JavaPlugin {
    private final @NotNull MapConfigLoader mapConfigLoader = new MapConfigLoader(this);
    private final @NotNull List<GameInstance> gameInstances = new ArrayList<>();

    @Override
    public void onEnable() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(new CTACommand(this).create("cta"));
        });
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    public @NotNull MapConfigLoader getMapConfigLoader() {
        return mapConfigLoader;
    }

    public @NotNull List<GameInstance> getGameInstances() {
        return gameInstances;
    }
}
