package net.azisaba.capturetheazi.game;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.azisaba.capturetheazi.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class Azi {
    public static final ItemStack AZI_ITEM = getItemStack(1);
    private final @NotNull GameInstance gameInstance;
    private final @NotNull Team team;
    private @Nullable Item entity = null;
    private @NotNull Status status = Status.DROPPED;
    private @Nullable Player holder = null;

    public Azi(@NotNull GameInstance gameInstance, @NotNull Team team) {
        this.gameInstance = gameInstance;
        this.team = team;
    }

    public @Nullable Item getEntity() {
        return entity;
    }

    public @NotNull Status getStatus() {
        return status;
    }

    public @Nullable Player getHolder() {
        return holder;
    }

    public void drop(@NotNull Location location) {
        if (location.getY() <= 1) {
            location = gameInstance.getMapConfig().spawnPoints().get(team).toLocation(location.getWorld());
        }
        if (entity != null) {
            entity.remove();
            entity = null;
        }
        if (holder != null) {
            holder.getInventory().removeItemAnySlot(getItemStack(10000));
            holder.setGlowing(false);
            holder = null;
        }
        status = Status.DROPPED;
        entity = location.getWorld().dropItem(location.add(0.0, 2.0, 0.0), AZI_ITEM);
        entity.setGlowing(true);
        entity.setGravity(false);
    }

    public void holdBy(@NotNull Player player) {
        if (entity != null) {
            entity.remove();
            entity = null;
        }
        if (holder != null) {
            holder.getInventory().removeItemAnySlot(getItemStack(10000));
            holder.setGlowing(false);
            holder = null;
        }
        status = Status.HELD_BY_PLAYER;
        holder = player;
        holder.getInventory().addItem(AZI_ITEM);
        holder.setGlowing(true);
    }

    public void capture() {
        if (entity != null) {
            entity.remove();
            entity = null;
        }
        if (holder != null) {
            holder.getInventory().removeItemAnySlot(getItemStack(10000));
            holder.setGlowing(false);
            holder = null;
        }
        status = Status.CAPTURED;
        var azi = gameInstance.getPoints().get(team);
        if (azi.getLeft().status == Status.CAPTURED && azi.getMiddle().status == Status.CAPTURED && azi.getRight().status == Status.CAPTURED) {
        }
    }

    public static @NotNull ItemStack getItemStack(int amount) {
        ItemStack stack = new ItemStack(Material.DIAMOND_SWORD, amount);
        stack.setData(DataComponentTypes.ITEM_NAME, Component.text("【AZI SAVIOR】", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        stack.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(Component.text("自分の陣地のゴールまで持っていこう！", NamedTextColor.WHITE))));
        stack.setData(DataComponentTypes.ITEM_MODEL, new NamespacedKey("azisaba", "azi"));
        stack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(207).build());
        stack.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        stack.setData(DataComponentTypes.RARITY, ItemRarity.EPIC);
        stack.setData(DataComponentTypes.UNBREAKABLE);
        // Nerf diamond sword
        /*
        stack.setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.itemAttributes()
                        .addModifier(
                                Attribute.ATTACK_DAMAGE,
                                new AttributeModifier(
                                        new NamespacedKey("cta", "damage"),
                                        1,
                                        AttributeModifier.Operation.ADD_NUMBER,
                                        EquipmentSlotGroup.MAINHAND
                                )
                        )
                        .build());
        */
        return stack;
    }

    public enum Status {
        DROPPED,
        HELD_BY_PLAYER,
        CAPTURED,
    }
}
