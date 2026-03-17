package net.azisaba.capturetheazi;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.azisaba.capturetheazi.commands.CTACommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CTAPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(new CTACommand().create("cta"));
        });
    }
}
