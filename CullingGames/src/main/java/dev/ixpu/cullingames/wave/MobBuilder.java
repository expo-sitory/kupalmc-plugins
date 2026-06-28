package dev.ixpu.cullingames.wave;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MobBuilder {

    public static LivingEntity spawnMob(Location location, MobSpawnData mobData) {
        try {
            // Spawn the base entity
            LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(
                    location,
                    org.bukkit.entity.EntityType.valueOf(mobData.getEntityType().toUpperCase())
            );

            // Set custom name if provided
            if (mobData.getCustomName() != null && !mobData.getCustomName().isEmpty()) {
                entity.setCustomName(mobData.getCustomName());
                entity.setCustomNameVisible(true);
            }

            // Apply equipment
            applyEquipment(entity, mobData.getEquipment());

            // Apply attributes
            applyAttributes(entity, mobData.getAttributes());

            // Apply custom effects (potion effects)
            applyCustomEffects(entity, mobData.getCustomEffects());

            // Set drop chances to 0 (items don't drop)
            entity.getEquipment().setBootsDropChance(0);
            entity.getEquipment().setLeggingsDropChance(0);
            entity.getEquipment().setChestplateDropChance(0);
            entity.getEquipment().setHelmetDropChance(0);
            entity.getEquipment().setItemInMainHandDropChance(0);
            entity.getEquipment().setItemInOffHandDropChance(0);

            return entity;

        } catch (IllegalArgumentException e) {
            // Invalid entity type
            return null;
        } catch (Exception e) {
            // Unexpected error
            e.printStackTrace();
            return null;
        }
    }

    // Applies armor and weapon to the entity
    private static void applyEquipment(LivingEntity entity, Equipment equipment) {
        if (equipment == null) {
            return;
        }

        // Apply armor
        if (equipment.hasArmor()) {
            applyArmor(entity, equipment.getArmorType(), equipment.getArmorEnchants());
        }

        // Apply weapon
        if (equipment.hasWeapon()) {
            applyWeapon(entity, equipment.getWeapon(), equipment.getWeaponEnchants());
        }
    }

    // Applies full armor set with enchantments
    private static void applyArmor(LivingEntity entity, String armorType, java.util.List<Enchantment> enchants) {
        try {
            // Create full armor set with enchantments
            ItemStack helmet = new ItemStack(Material.valueOf(armorType.toUpperCase() + "_HELMET"));
            ItemStack chestplate = new ItemStack(Material.valueOf(armorType.toUpperCase() + "_CHESTPLATE"));
            ItemStack leggings = new ItemStack(Material.valueOf(armorType.toUpperCase() + "_LEGGINGS"));
            ItemStack boots = new ItemStack(Material.valueOf(armorType.toUpperCase() + "_BOOTS"));

            // Add enchantments to all armor pieces
            for (Enchantment enchant : enchants) {
                try {
                    org.bukkit.enchantments.Enchantment ench = getEnchantment(enchant.getName());
                    if (ench != null) {
                        helmet.addUnsafeEnchantment(ench, enchant.getLevel());
                        chestplate.addUnsafeEnchantment(ench, enchant.getLevel());
                        leggings.addUnsafeEnchantment(ench, enchant.getLevel());
                        boots.addUnsafeEnchantment(ench, enchant.getLevel());
                    }
                } catch (Exception ignored) {
                    // Skip invalid enchantments
                }
            }

            entity.getEquipment().setHelmet(helmet);
            entity.getEquipment().setChestplate(chestplate);
            entity.getEquipment().setLeggings(leggings);
            entity.getEquipment().setBoots(boots);

        } catch (Exception e) {
            // Invalid armor type - skip
        }
    }

    // Applies weapon with enchantments
    private static void applyWeapon(LivingEntity entity, String weaponType, java.util.List<Enchantment> enchants) {
        try {
            ItemStack weapon = new ItemStack(Material.valueOf(weaponType.toUpperCase()));

            // Add enchantments
            for (Enchantment enchant : enchants) {
                try {
                    org.bukkit.enchantments.Enchantment ench = getEnchantment(enchant.getName());
                    if (ench != null) {
                        weapon.addUnsafeEnchantment(ench, enchant.getLevel());
                    }
                } catch (Exception ignored) {
                    // Skip invalid enchantments
                }
            }

            entity.getEquipment().setItemInMainHand(weapon);

        } catch (Exception e) {
            // Invalid weapon type - skip
        }
    }

    // Applies attributes like health, speed, damage, etc.
    private static void applyAttributes(LivingEntity entity, java.util.Map<String, Double> attributes) {
        for (java.util.Map.Entry<String, Double> entry : attributes.entrySet()) {
            try {
                Attribute attribute = Attribute.valueOf("GENERIC_" + entry.getKey().toUpperCase());
                AttributeInstance instance = entity.getAttribute(attribute);
                if (instance != null) {
                    instance.setBaseValue(entry.getValue());
                }
            } catch (Exception ignored) {
                // Skip invalid attributes
            }
        }
    }

    // Applies potion effects to the entity
    private static void applyCustomEffects(LivingEntity entity, java.util.Map<String, Integer> customEffects) {
        for (java.util.Map.Entry<String, Integer> entry : customEffects.entrySet()) {
            try {
                PotionEffectType type = PotionEffectType.getByName(entry.getKey());
                if (type != null) {
                    int amplifier = entry.getValue();
                    // -1 duration = infinite (Integer.MAX_VALUE ticks)
                    int duration = amplifier == -1 ? Integer.MAX_VALUE : amplifier;
                    
                    // For custom effects, amplifier is actually the effect level
                    // Use amplifier - 1 since potion effect amplifiers are 0-indexed
                    entity.addPotionEffect(new PotionEffect(type, duration, entry.getValue() - 1, false, false));
                }
            } catch (Exception ignored) {
                // Skip invalid effects
            }
        }
    }

    // Helper method to get enchantment - supports both old and new Bukkit API
    private static org.bukkit.enchantments.Enchantment getEnchantment(String name) {
        try {
            // Try the registry method first (Paper 1.20+)
            return org.bukkit.Registry.ENCHANTMENT.get(
                new org.bukkit.NamespacedKey("minecraft", name.toLowerCase())
            );
        } catch (Exception e) {
            try {
                // Fall back to legacy method
                @SuppressWarnings("deprecation")
                org.bukkit.enchantments.Enchantment ench = org.bukkit.enchantments.Enchantment.getByName(name);
                return ench;
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}