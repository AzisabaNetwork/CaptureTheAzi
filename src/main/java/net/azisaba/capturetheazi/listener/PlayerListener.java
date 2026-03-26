package net.azisaba.capturetheazi.listener;

import net.azisaba.capturetheazi.CTAPlugin;
import net.azisaba.capturetheazi.Team;
import net.azisaba.capturetheazi.game.Azi;
import net.azisaba.capturetheazi.game.GameInstance;
import net.azisaba.capturetheazi.util.LocationUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public final class PlayerListener implements Listener {
    private final @NotNull CTAPlugin plugin;

    public PlayerListener(@NotNull CTAPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        GameInstance gameInstance = plugin.findGame(e.getPlayer().getUniqueId());
        if (gameInstance == null) return;
        gameInstance.addPlayerPlacedBlock(e.getBlock().getLocation());
        Team selfTeam = gameInstance.getTeam(e.getPlayer().getUniqueId());
        double distance = gameInstance.getMapConfig().spawnPoints().get(selfTeam).toLocation(e.getBlock().getWorld()).distance(e.getBlock().getLocation());
        if (distance > 10) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(Component.text("スポーン地点から半径10ブロック以内にはブロックを設置できません", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        GameInstance gameInstance = plugin.findGame(e.getPlayer().getUniqueId());
        if (gameInstance == null) return;
        if (gameInstance.isPlayerPlacedBlock(e.getBlock().getLocation())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        GameInstance gameInstance = plugin.findGame(e.getPlayer().getUniqueId());
        if (gameInstance == null) return;
        Azi azi = gameInstance.getAzi(e.getPlayer());
        if (azi == null) return;
        azi.drop(e.getEntity().getLocation());
    }

    @EventHandler
    public void onPlayerAttemptPickupAzi(PlayerAttemptPickupItemEvent e) {
        GameInstance gameInstance = plugin.findGame(e.getPlayer().getUniqueId());
        if (gameInstance == null || !gameInstance.getPhase().canPickupAzi()) return;
        Team selfTeam = gameInstance.getTeam(e.getPlayer().getUniqueId());
        if (selfTeam == null) return;
        if (!e.getItem().getItemStack().isSimilar(Azi.AZI_ITEM)) return;
        e.setCancelled(true);
        Azi azi = gameInstance.getFirstAziOfStatus(Azi.Status.DROPPED, selfTeam);
        if (azi == null) return;
        azi.holdBy(e.getPlayer());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        GameInstance gameInstance = plugin.findGame(e.getPlayer().getUniqueId());
        if (gameInstance == null) return;
        Team selfTeam = gameInstance.getTeam(e.getPlayer().getUniqueId());
        if (selfTeam == null) return;
        Azi azi = gameInstance.getAzi(e.getPlayer());
        if (azi == null) return;
        var goals = gameInstance.getMapConfig().goals().get(selfTeam);
        boolean inBounds = LocationUtil.inBounds(e.getPlayer().getLocation(), goals.getFirst().toLocation(e.getPlayer().getWorld()), goals.getSecond().toLocation(e.getPlayer().getWorld()));
        if (!inBounds) return;
        azi.capture();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        GameInstance gameInstance = plugin.findGame(e.getPlayer().getUniqueId());
        if (gameInstance == null) return;
        Team selfTeam = gameInstance.getTeam(e.getPlayer().getUniqueId());
        if (selfTeam == null) return;
        gameInstance.removePlayer(selfTeam, e.getPlayer().getUniqueId(), e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        GameInstance gameInstance = plugin.findGame(e.getPlayer().getUniqueId());
        if (gameInstance == null) return;
        Team selfTeam = gameInstance.getTeam(e.getPlayer().getUniqueId());
        if (selfTeam == null) return;
        e.setRespawnLocation(gameInstance.getMapConfig().spawnPoints().get(selfTeam).toLocation(e.getPlayer().getWorld()));
    }
}
