package dev.ixpu.cullingames.command;

import dev.ixpu.cullingames.manager.EventManager;
import dev.ixpu.cullingames.util.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ForcejoinallCommand implements CommandExecutor {

    private final EventManager eventManager;
    private final MessageManager messageManager;

    public ForcejoinallCommand(EventManager eventManager, MessageManager messageManager) {
        this.eventManager = eventManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("cg.admin")) {
            messageManager.sendMessage(sender, "No permission");
            return true;
        }

        if (eventManager.isActive()) {
            messageManager.sendMessage(sender, "Cannot force join while event is active");
            return true;
        }

        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!eventManager.isParticipant(player.getUniqueId())) {
                eventManager.addPlayer(player.getUniqueId());
                player.sendMessage(Component.text("You were force-joined to the CullingGames event", NamedTextColor.YELLOW));
                count++;
            }
        }

        messageManager.sendMessage(sender, "Force-joined " + count + " players to the event");
        return true;
    }
}
