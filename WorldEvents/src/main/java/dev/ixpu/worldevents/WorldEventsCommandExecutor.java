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
            sender.sendMessage("§c/worldevents reload - Reload the configuration");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("worldevents.reload")) {
                sender.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }

            plugin.reloadConfig();
            plugin.clearRecipeCache();
            sender.sendMessage("§aWorldEvents configuration reloaded!");
            boolean inflationEnabled = plugin.isInflationEnabled();
            boolean heroEnabled = plugin.isHeroDiscountEnabled();
            boolean stagnationEnabled = plugin.isStagnationEnabled();
            sender.sendMessage("§bInflation feature: " + (inflationEnabled ? "§aENABLED" : "§cDISABLED"));
            sender.sendMessage("§bHero of the Village discount: " + (heroEnabled ? "§aENABLED" : "§cDISABLED"));
            sender.sendMessage("§bStagnation feature: " + (stagnationEnabled ? "§aENABLED" : "§cDISABLED"));
            return true;
        }

        sender.sendMessage("§cUnknown subcommand. Use /worldevents reload");
        return true;
    }
}
