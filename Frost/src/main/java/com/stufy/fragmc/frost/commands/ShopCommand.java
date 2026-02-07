package com.stufy.fragmc.frost.commands;

import com.stufy.fragmc.frost.Frost;
import com.stufy.fragmc.frost.gui.PowerupShopGUI;
import com.stufy.fragmc.frost.gui.SkinShopGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShopCommand implements CommandExecutor, TabCompleter {

    private final Frost plugin;

    public ShopCommand(Frost plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (!player.hasPermission("fragmc.shop")) {
            player.sendMessage(plugin.getPrefix() + plugin.getMessage("no-permission"));
            return true;
        }

        // Bedrock Support
        if (plugin.isFloodgateEnabled() && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            if (args.length == 0) {
                plugin.getBedrockMenuHandler().openMainMenu(player);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "skins":
                    plugin.getBedrockMenuHandler().openMainMenu(player); // Or specific category menu if structure
                                                                         // allows
                    return true;
                case "powerups":
                    plugin.getBedrockMenuHandler().openPowerupShop(player);
                    return true;
                default:
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /shop [skins|powerups]");
                    return true;
            }
        }

        if (args.length == 0) {
            // Open main shop menu (skins by default)
            new SkinShopGUI(plugin, player).open();
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "skins":
                new SkinShopGUI(plugin, player).open();
                return true;

            case "powerups":
                new PowerupShopGUI(plugin, player).open();
                return true;

            default:
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /shop [skins|powerups]");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("fragmc.shop")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("skins", "powerups");
        }

        return new ArrayList<>();
    }
}