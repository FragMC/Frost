package com.stufy.fragmc.frost.commands;

import com.stufy.fragmc.frost.Frost;
import com.stufy.fragmc.frost.managers.PlayerDataManager.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ToggleLockCommand implements CommandExecutor {
    private final Frost plugin;

    public ToggleLockCommand(Frost plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) {
            plugin.getLogger().warning("Player data is null for " + player.getName());
            return true;
        }

        data.hotbarLocked = !data.hotbarLocked;
        plugin.getPlayerDataManager().savePlayerData(player);

        // Clear any equipped armor cosmetics and armor pieces on lock toggle
        if (plugin.getCosmeticManager() != null) {
            plugin.getCosmeticManager().clearAllArmorSlots(player);
        }
        if (data.equippedCosmetics != null) {
            data.equippedCosmetics.keySet().removeIf(k -> k.startsWith("armor-cosmetics:"));
            plugin.getPlayerDataManager().savePlayerData(player);
        }

        if (data.hotbarLocked) {
            player.sendMessage(Component.text("Hotbar Lock ENABLED", NamedTextColor.GREEN));
            plugin.getHotbarLockListener().giveHotbarItems(player);
        } else {
            player.sendMessage(Component.text("Hotbar Lock DISABLED", NamedTextColor.RED));
        }
        return true;
    }
}
