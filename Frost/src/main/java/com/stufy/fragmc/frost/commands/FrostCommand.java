package com.stufy.fragmc.frost.commands;

import com.stufy.fragmc.frost.Frost;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FrostCommand implements CommandExecutor, TabCompleter {
    private final Frost plugin;

    public FrostCommand(Frost plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /frost <reload|setprofile|listprofiles|givecosmetic>", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("frost.admin")) {
                    sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
                    return true;
                }
                plugin.getConfigManager().loadConfig();
                plugin.getProfileManager().loadProfiles();
                plugin.getCosmeticManager().loadCosmetics();
                sender.sendMessage(Component.text("Configuration reloaded.", NamedTextColor.GREEN));
                break;

            case "setprofile":
                if (!sender.hasPermission("frost.admin")) {
                    sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /frost setprofile <player> <profile>", NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }
                if (plugin.getProfileManager().setPlayerProfile(target, args[2])) {
                    sender.sendMessage(Component.text("Profile set to " + args[2] + " for " + target.getName(), NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Profile not found: " + args[2], NamedTextColor.RED));
                }
                break;

            case "listprofiles":
                if (!sender.hasPermission("frost.admin")) {
                    sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
                    return true;
                }
                sender.sendMessage(Component.text("Available Profiles:", NamedTextColor.AQUA));
                plugin.getProfileManager().getAllProfileIds().forEach(id -> {
                    var profile = plugin.getProfileManager().getProfile(id);
                    sender.sendMessage(Component.text("  - " + id + ": " + profile.getDisplayName(), NamedTextColor.GRAY));
                });
                break;

            case "givecosmetic":
                if (!sender.hasPermission("frost.admin")) {
                    sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /frost givecosmetic <player> <cosmetic>", NamedTextColor.RED));
                    return true;
                }
                Player cosmeticTarget = Bukkit.getPlayer(args[1]);
                if (cosmeticTarget == null) {
                    sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                    return true;
                }
                if (plugin.getAPI().giveCosmetic(cosmeticTarget, args[2])) {
                    sender.sendMessage(Component.text("Gave " + args[2] + " to " + cosmeticTarget.getName(), NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Cosmetic not found: " + args[2], NamedTextColor.RED));
                }
                break;

            default:
                sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("frost.admin")) {
                completions.add("reload");
                completions.add("setprofile");
                completions.add("listprofiles");
                completions.add("givecosmetic");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("setprofile") || args[0].equalsIgnoreCase("givecosmetic")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setprofile")) {
                return new ArrayList<>(plugin.getProfileManager().getAllProfileIds());
            } else if (args[0].equalsIgnoreCase("givecosmetic")) {
                return plugin.getCosmeticManager().getCategories().values().stream()
                        .flatMap(cat -> cat.getCosmetics().stream())
                        .map(cosmetic -> cosmetic.getId())
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }
}
