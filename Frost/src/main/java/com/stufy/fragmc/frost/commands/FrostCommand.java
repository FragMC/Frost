package com.stufy.fragmc.frost.commands;

import com.stufy.fragmc.frost.Frost;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FrostCommand implements CommandExecutor, TabCompleter {

    private final Frost plugin;

    public FrostCommand(Frost plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("toggle")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return true;
            }

            if (!player.hasPermission("fragmc.hotbar.toggle")) {
                sender.sendMessage(plugin.getPrefix() + plugin.getMessage("no-permission"));
                return true;
            }

            boolean currentState = plugin.getHotbarManager().isToggledOff(player);
            plugin.getHotbarManager().setToggledOff(player, !currentState);

            if (!currentState) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Hotbar items disabled.");
            } else {
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Hotbar items enabled.");
            }
            return true;
        }

        if (!sender.hasPermission("frost.admin")) {
            sender.sendMessage(plugin.getPrefix() + plugin.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reload();
                sender.sendMessage(plugin.getPrefix() + plugin.getMessage("reload-success"));
                return true;

            case "give":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /frost give <player>");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Player not found!");
                    return true;
                }

                plugin.getHotbarManager().giveHotbarItems(target);
                String giveMsg = plugin.getMessage("items-given").replace("%player%", target.getName());
                sender.sendMessage(plugin.getPrefix() + giveMsg);
                return true;

            case "reset":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /frost reset <player>");
                    return true;
                }

                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Player not found!");
                    return true;
                }

                // Clear hotbar and give items
                for (int i = 0; i < 9; i++) {
                    target.getInventory().setItem(i, null);
                }
                plugin.getHotbarManager().giveHotbarItems(target);

                String resetMsg = plugin.getMessage("items-reset").replace("%player%", target.getName());
                sender.sendMessage(plugin.getPrefix() + resetMsg);
                return true;

            case "toggledaily":
                boolean currentDailyState = plugin.getDailyRewardManager().isEnabled();
                plugin.getDailyRewardManager().setEnabled(!currentDailyState);
                if (!currentDailyState) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Daily rewards enabled.");
                } else {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Daily rewards disabled.");
                }
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== FragMC Commands ===");
        if (sender.hasPermission("fragmc.hotbar.toggle")) {
            sender.sendMessage(ChatColor.YELLOW + "/frost toggle" + ChatColor.GRAY + " - Toggle hotbar items");
        }
        sender.sendMessage(ChatColor.YELLOW + "/frost reload" + ChatColor.GRAY + " - Reload the plugin configuration");
        sender.sendMessage(
                ChatColor.YELLOW + "/frost give <player>" + ChatColor.GRAY + " - Give hotbar items to a player");
        sender.sendMessage(ChatColor.YELLOW + "/frost reset <player>" + ChatColor.GRAY
                + " - Reset and give hotbar items to a player");
        sender.sendMessage(ChatColor.YELLOW + "/frost toggledaily" + ChatColor.GRAY
                + " - Toggle daily rewards system");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("fragmc.hotbar.toggle")) {
                completions.add("toggle");
            }
            if (sender.hasPermission("frost.admin")) {
                completions.addAll(Arrays.asList("reload", "give", "reset", "toggledaily"));
            }
            return completions;
        }

        if (!sender.hasPermission("frost.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("reset"))) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return players;
        }

        return new ArrayList<>();
    }
}