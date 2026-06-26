package dev.ixpu.itemorigin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ItemOriginListener implements Listener {

    private static final Set<Material> TRACKED_ITEMS = new HashSet<>();

    static {
        TRACKED_ITEMS.addAll(Set.of(
            // Iron tools
            Material.IRON_SHOVEL,
            Material.IRON_PICKAXE,
            Material.IRON_AXE,
            Material.IRON_HOE,
            Material.IRON_SWORD,
            
            // Iron armor
            Material.IRON_HELMET,
            Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS,
            Material.IRON_BOOTS,
            
            // Gold tools
            Material.GOLDEN_SHOVEL,
            Material.GOLDEN_PICKAXE,
            Material.GOLDEN_AXE,
            Material.GOLDEN_HOE,
            Material.GOLDEN_SWORD,
            
            // Gold armor
            Material.GOLDEN_HELMET,
            Material.GOLDEN_CHESTPLATE,
            Material.GOLDEN_LEGGINGS,
            Material.GOLDEN_BOOTS,
            
            // Diamond tools
            Material.DIAMOND_SHOVEL,
            Material.DIAMOND_PICKAXE,
            Material.DIAMOND_AXE,
            Material.DIAMOND_HOE,
            Material.DIAMOND_SWORD,
            
            // Diamond armor
            Material.DIAMOND_HELMET,
            Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS,
            Material.DIAMOND_BOOTS,
            
            // Netherite tools
            Material.NETHERITE_SHOVEL,
            Material.NETHERITE_PICKAXE,
            Material.NETHERITE_AXE,
            Material.NETHERITE_HOE,
            Material.NETHERITE_SWORD,
            
            // Netherite armor
            Material.NETHERITE_HELMET,
            Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS,
            Material.NETHERITE_BOOTS,

            // Other weapons
            Material.MACE,
            Material.TRIDENT,
            Material.BOW,
            Material.CROSSBOW,
            Material.SHIELD
        ));

        // Try to add spear materials if they exist (Minecraft 1.21+)
        try {
            TRACKED_ITEMS.add(Material.valueOf("IRON_SPEAR"));
            TRACKED_ITEMS.add(Material.valueOf("GOLDEN_SPEAR"));
            TRACKED_ITEMS.add(Material.valueOf("DIAMOND_SPEAR"));
            TRACKED_ITEMS.add(Material.valueOf("NETHERITE_SPEAR"));
        } catch (IllegalArgumentException e) {
            // Materials don't exist in this Paper version yet
        }
    }

    private static final String ORIGIN_PREFIX = "Origin: ";

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        if (item == null || item.getAmount() == 0) {
            return;
        }

        if (!TRACKED_ITEMS.contains(item.getType())) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        // Check if item already has origin lore
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        boolean hasOrigin = lore.stream()
            .anyMatch(line -> PlainTextComponentSerializer.plainText().serialize(line).startsWith(ORIGIN_PREFIX));

        if (!hasOrigin) {
            Component originLine = Component.text(ORIGIN_PREFIX + player.getName(), NamedTextColor.RED);
            lore.add(originLine);
            meta.lore(lore);
            item.setItemMeta(meta);
        }
    }
}