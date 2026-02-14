package com.stufy.fragmc.frost.commands;

import com.stufy.fragmc.frost.Frost;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FrostCommand implements CommandExecutor, TabCompleter {
    private final Frost plugin;

    public FrostCommand(Frost plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /frost <reload>", NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("frost.admin")) {
                plugin.getConfigManager().loadConfig();
                sender.sendMessage(Component.text("Configuration reloaded.", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
            }
        } else {
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
            }
        }
        return completions;
    }
}
