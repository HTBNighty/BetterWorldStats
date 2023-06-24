package me.xGinko.betterworldstats.commands.betterws.subcommands;

import me.xGinko.betterworldstats.BetterWorldStats;
import me.xGinko.betterworldstats.commands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ReloadSubCmd extends SubCommand {
    @Override
    public String getName() {return "reload";}
    @Override
    public String getDescription() {return "Reload the plugin configuration.";}
    @Override
    public String getSyntax() {return "/betterws reload";}
    @Override
    public void perform(CommandSender sender, String[] args) {
        if (sender.hasPermission("betterws.reload")) {
            sender.sendMessage(ChatColor.RED + "Reloading BetterWorldStats...");
            BetterWorldStats.getInstance().reloadPlugin();
            sender.sendMessage(ChatColor.GREEN + "Reload complete.");
        } else {
            sender.sendMessage(ChatColor.RED + BetterWorldStats.getLang(sender).no_permission);
        }
    }
}