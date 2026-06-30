package dev.ixpu.worldevents;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class VillagerTradeListener implements Listener {

    private final WorldEvents plugin;

    public VillagerTradeListener(WorldEvents plugin) {
        this.plugin = plugin;
    }

    private void convertPrices(Merchant merchant, boolean inflatePrice) {
        List<MerchantRecipe> newRecipes = new ArrayList<>();

        for (MerchantRecipe recipe : merchant.getRecipes()) {
            List<ItemStack> ingredients = new ArrayList<>(recipe.getIngredients());
            boolean modified = false;

            for (int i = 0; i < ingredients.size(); i++) {
                ItemStack item = ingredients.get(i);
                
                if (inflatePrice && item.getType() == Material.EMERALD) {
                    // Convert emerald → emerald block (keep same quantity)
                    ingredients.set(i, new ItemStack(Material.EMERALD_BLOCK, item.getAmount()));
                    modified = true;
                } else if (!inflatePrice && item.getType() == Material.EMERALD_BLOCK) {
                    // Convert emerald block → emerald (keep same quantity)
                    ingredients.set(i, new ItemStack(Material.EMERALD, item.getAmount()));
                    modified = true;
                }
            }

            if (modified) {
                MerchantRecipe newRecipe = new MerchantRecipe(
                        recipe.getResult(),
                        recipe.getUses(),
                        recipe.getMaxUses(),
                        recipe.hasExperienceReward(),
                        recipe.getVillagerExperience(),
                        recipe.getPriceMultiplier()
                );
                newRecipe.setIngredients(ingredients);
                newRecipes.add(newRecipe);
            } else {
                newRecipes.add(recipe);
            }
        }

        merchant.setRecipes(newRecipes);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Merchant)) {
            return;
        }

        Merchant merchant = (Merchant) event.getRightClicked();
        boolean inflationEnabled = plugin.isInflationEnabled();

        // Remove Hero of the Village effect if config says to
        if (!plugin.isHeroDiscountEnabled()) {
            event.getPlayer().getActivePotionEffects().forEach(effect -> {
                if (effect.getType() == PotionEffectType.HERO_OF_THE_VILLAGE) {
                    event.getPlayer().removePotionEffect(effect.getType());
                }
            });
        }

        // Convert prices based on current setting
        convertPrices(merchant, inflationEnabled);
    }

    @EventHandler
    public void onEntityTransform(EntityTransformEvent event) {
        // Prevent villager curing to block discount exploitation
        if (event.getTransformReason() == EntityTransformEvent.TransformReason.CURED) {
            event.setCancelled(true);
        }
    }

    public void clearCache() {
        // No cache to clear with new approach
    }
}
