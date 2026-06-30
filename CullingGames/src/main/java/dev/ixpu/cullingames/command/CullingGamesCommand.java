package dev.ixpu.cullingames.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import dev.ixpu.cullingames.manager.EventManager;
import dev.ixpu.cullingames.util.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CullingGamesCommand implements CommandExecutor, TabCompleter {

    private final EventManager eventManager;
    private final MessageManager messageManager;

    public CullingGamesCommand(EventManager eventManager, MessageManager messageManager) {
        this.eventManager = eventManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            messageManager.sendMessage(sender, "Usage: /cgames <join|leave|start|pause|forcestop|givepoints|takepoints|stats|reload>");
            return true;
        }

        String action = args[0].toLowerCase();

        return switch (action) {
            case "join" -> cmdJoin(sender);
            case "leave" -> cmdLeave(sender);
            case "start" -> cmdStart(sender);
            case "pause" -> cmdPause(sender);
            case "forcestop" -> cmdForceStop(sender, args.length > 1 ? args[1] : null);
            case "givepoints" -> cmdGivePoints(sender, args);
            case "takepoints" -> cmdTakePoints(sender, args);
            case "stats" -> cmdStats(sender);
            case "reload" -> cmdReload(sender);
            default -> {
                messageManager.sendMessage(sender, "Unknown subcommand: " + action);
                yield false;
            }
        };
    }

    @SuppressWarnings("nullness")
    private boolean cmdJoin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "Only players can join");
            return true;
        }

        if (eventManager.isActive()) {
            player.sendMessage(Component.text("Cannot join while event is active", NamedTextColor.RED));
            return true;
        }

        if (eventManager.isParticipant(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already registered", NamedTextColor.RED));
            return true;
        }

        eventManager.addPlayer(player.getUniqueId());
        messageManager.sendMessage(player, "You have joined the CullingGames event!");
        Bukkit.broadcast(Component.text(player.getName() + " joined the event", NamedTextColor.YELLOW));
        return true;
    }

    @SuppressWarnings("nullness")
    private boolean cmdLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageManager.sendMessage(sender, "Only players can leave");
            return true;
        }

        if (!eventManager.isParticipant(player.getUniqueId())) {
            player.sendMessage(Component.text("You are not registered", NamedTextColor.RED));
            return true;
        }

        if (eventManager.isActive()) {
            player.sendMessage(Component.text("Cannot leave while event is active", NamedTextColor.RED));
            return true;
        }

        eventManager.removePlayer(player.getUniqueId());
        messageManager.sendMessage(player, "You have left the CullingGames event");
        return true;
    }

    private boolean cmdStart(CommandSender sender) {
        if (!sender.hasPermission("cg.admin")) {
            messageManager.sendMessage(sender, "No permission");
            return true;
        }

        if (eventManager.isActive()) {
            messageManager.sendMessage(sender, "Event is already active");
            return true;
        }

        Set<UUID> participants = eventManager.getPlayers();
        eventManager.startEvent(participants);

        Bukkit.broadcast(Component.text("CullingGames event started! (" + participants.size() + " players)", NamedTextColor.GOLD));
        messageManager.sendMessage(sender, "Event started with " + participants.size() + " players");
        return true;
    }

    private boolean cmdPause(CommandSender sender) {
        if (!sender.hasPermission("cg.admin")) {
            messageManager.sendMessage(sender, "No permission");
            return true;
        }

        if (!eventManager.isActive()) {
            messageManager.sendMessage(sender, "Event is not active");
            return true;
        }

        eventManager.togglePause();
        String status = eventManager.isPaused() ? "PAUSED" : "RESUMED";
        Bukkit.broadcast(Component.text("Event " + status + " by " + sender.getName(), NamedTextColor.YELLOW));
        return true;
    }

    private boolean cmdForceStop(CommandSender sender, String confirm) {
        if (!sender.hasPermission("cg.admin")) {
            messageManager.sendMessage(sender, "No permission");
            return true;
        }

        if (!eventManager.isActive()) {
            messageManager.sendMessage(sender, "Event is not active");
            return true;
        }

        if (confirm != null && confirm.equalsIgnoreCase("confirm")) {
            if (!eventManager.confirmForceStop()) {
                messageManager.sendMessage(sender, "No pending force stop confirmation");
                return true;
            }
            return true;
        }

        if (eventManager.isForceStopConfirmPending()) {
            sender.sendMessage(Component.text("Force stop already armed, run again with 'confirm' to execute", NamedTextColor.YELLOW));
            return true;
        }

        eventManager.armForceStop();
        sender.sendMessage(Component.text("Force stop armed. Run '/cgames forcestop confirm' within 10 seconds to execute", NamedTextColor.YELLOW));
        return true;
    }

    private boolean cmdGivePoints(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cg.admin")) {
            messageManager.sendMessage(sender, "No permission");
            return true;
        }

        if (args.length < 3) {
            messageManager.sendMessage(sender, "Usage: /cgames givepoints <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messageManager.sendMessage(sender, "Player not found");
            return true;
        }

        if (!eventManager.isParticipant(target.getUniqueId())) {
            messageManager.sendMessage(sender, "Player is not registered");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            messageManager.sendMessage(sender, "Invalid amount");
            return true;
        }

        eventManager.addPoints(target.getUniqueId(), amount);
        double newTotal = eventManager.getPoints(target.getUniqueId());

        messageManager.sendMessage(sender, "Gave " + amount + " points to " + target.getName() + " (total: " + newTotal + ")");
        messageManager.sendMessage(target, "Received " + amount + " points (total: " + newTotal + ")");
        return true;
    }

    private boolean cmdTakePoints(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cg.admin")) {
            messageManager.sendMessage(sender, "No permission");
            return true;
        }

        if (args.length < 3) {
            messageManager.sendMessage(sender, "Usage: /cgames takepoints <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messageManager.sendMessage(sender, "Player not found");
            return true;
        }

        if (!eventManager.isParticipant(target.getUniqueId())) {
            messageManager.sendMessage(sender, "Player is not registered");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            messageManager.sendMessage(sender, "Invalid amount");
            return true;
        }

        eventManager.subtractPoints(target.getUniqueId(), amount);
        double newTotal = eventManager.getPoints(target.getUniqueId());

        messageManager.sendMessage(sender, "Took " + amount + " points from " + target.getName() + " (total: " + newTotal + ")");
        messageManager.sendMessage(target, "Lost " + amount + " points (total: " + newTotal + ")");
        return true;
    }

    private boolean cmdStats(CommandSender sender) {
        if (!sender.hasPermission("cg.admin")) {
            messageManager.sendMessage(sender, "No permission");
            return true;
        }

        Set<UUID> players = eventManager.getPlayers();
        if (players.isEmpty()) {
            messageManager.sendMessage(sender, "No players registered");
            return true;
        }

        List<UUID> sorted = new ArrayList<>(players);
        sorted.sort((a, b) -> Double.compare(
            eventManager.getPoints(b),
            eventManager.getPoints(a)
        ));

        sender.sendMessage(Component.text("§8══════ §b§lCullingGames Stats [" + eventManager.getTimerFormatted() + "] §8══════", NamedTextColor.GOLD));
        int rank = 1;
        for (UUID uuid : sorted) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                double pts = eventManager.getPoints(uuid);
                int kills = eventManager.getPlayerKillCount(uuid);
                sender.sendMessage(Component.text(rank + ". " + p.getName() + " - " + (int) pts + "pts (" + kills + " kills)", NamedTextColor.YELLOW));
                rank++;
            }
        }

        return true;
    }

    private boolean cmdReload(CommandSender sender) {
        if (!sender.hasPermission("cg.admin")) {
            messageManager.sendMessage(sender, "No permission");
            return true;
        }

        try {
            dev.ixpu.cullingames.CullingGamesPlugin.getInstance().getConfigManager().reload();
            messageManager.reload();
            messageManager.sendMessage(sender, "Configuration reloaded successfully!");
        } catch (Exception e) {
            messageManager.sendMessage(sender, "Failed to reload configuration: " + e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("join");
            suggestions.add("leave");
            if (sender.hasPermission("cg.admin")) {
                suggestions.add("start");
                suggestions.add("pause");
                suggestions.add("forcestop");
                suggestions.add("givepoints");
                suggestions.add("takepoints");
                suggestions.add("stats");
                suggestions.add("reload");
            }
            return suggestions;
        }

        if (args.length == 2) {
            String action = args[0].toLowerCase();
            if ("givepoints".equals(action) || "takepoints".equals(action)) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
            }
        }

        return Collections.emptyList();
    }
}