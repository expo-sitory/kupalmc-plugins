package dev.ixpu.worldevents;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class WorldEventsCommandExecutor implements CommandExecutor {

    private final WorldEvents plugin;

    public WorldEventsCommandExecutor(WorldEvents plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§c/we reload - Reload the configuration");
            sender.sendMessage("§c/we status - Show current event status");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("worldevents.reload")) {
                sender.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }

            plugin.reloadConfig();
            plugin.clearRecipeCache();
            sender.sendMessage("§aAll Configurations Reloaded Successfully");
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            if (!sender.hasPermission("worldevents.status")) {
                sender.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }

            showStatus(sender);
            return true;
        }

        sender.sendMessage("§cUnknown subcommand. Use /we reload or /we status");
        return true;
    }

    private void showStatus(CommandSender sender) {
        boolean inflationEnabled = plugin.isInflationEnabled();
        boolean heroEnabled = plugin.isHeroDiscountEnabled();
        boolean stagnationEnabled = plugin.isStagnationEnabled();

        String inflationStatus = inflationEnabled ? "§aEnabled" : "§cDisabled";
        String heroStatus = heroEnabled ? "§aEnabled" : "§cDisabled";
        String stagnationStatus = stagnationEnabled ? "§aEnabled" : "§cDisabled";

        sender.sendMessage("§8 ═══════ §bWorld Event Status §8═══════");
        sender.sendMessage("");
        sender.sendMessage("§bInflation: " + inflationStatus);
        sender.sendMessage("  * Discounts: " + heroStatus);
        sender.sendMessage("");
        sender.sendMessage("§bStagnation: " + stagnationStatus);
        sender.sendMessage("");
        sender.sendMessage("§8Plugin by Ixpu | §bhttps://github.com/expo-sitory");
        sender.sendMessage("§8════════════════════════════");
    }
}
