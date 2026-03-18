package net.azisaba.capturetheazi;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.azisaba.capturetheazi.commands.CTACommand;
import net.azisaba.capturetheazi.config.MapConfigLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class CTAPlugin extends JavaPlugin {
    private final @NotNull MapConfigLoader mapConfigLoader = new MapConfigLoader(this);

    @Override
    public void onEnable() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(new CTACommand(this).create("cta"));
        });
    }

    public @NotNull MapConfigLoader getMapConfigLoader() {
        return mapConfigLoader;
    }
}
