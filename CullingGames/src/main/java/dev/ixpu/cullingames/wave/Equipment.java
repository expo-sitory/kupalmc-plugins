package dev.ixpu.cullingames.wave;

import java.util.*;

public class Equipment {
    private final String armorType; // diamond, netherite, iron, etc.
    private final List<Enchantment> armorEnchants;
    private final String weapon; // netherite_axe, bow, etc.
    private final List<Enchantment> weaponEnchants;

    public Equipment(String armorType, List<Enchantment> armorEnchants, String weapon, List<Enchantment> weaponEnchants) {
        this.armorType = armorType;
        this.armorEnchants = armorEnchants != null ? armorEnchants : new ArrayList<>();
        this.weapon = weapon;
        this.weaponEnchants = weaponEnchants != null ? weaponEnchants : new ArrayList<>();
    }

    public String getArmorType() {
        return armorType;
    }

    public List<Enchantment> getArmorEnchants() {
        return new ArrayList<>(armorEnchants);
    }

    public String getWeapon() {
        return weapon;
    }

    public List<Enchantment> getWeaponEnchants() {
        return new ArrayList<>(weaponEnchants);
    }

    public boolean hasArmor() {
        return armorType != null && !armorType.isEmpty();
    }

    public boolean hasWeapon() {
        return weapon != null && !weapon.isEmpty();
    }

    public static class Builder {
        private String armorType;
        private List<Enchantment> armorEnchants = new ArrayList<>();
        private String weapon;
        private List<Enchantment> weaponEnchants = new ArrayList<>();

        public Builder armorType(String type) {
            this.armorType = type;
            return this;
        }

        public Builder addArmorEnchant(Enchantment enchant) {
            this.armorEnchants.add(enchant);
            return this;
        }

        public Builder weapon(String weapon) {
            this.weapon = weapon;
            return this;
        }

        public Builder addWeaponEnchant(Enchantment enchant) {
            this.weaponEnchants.add(enchant);
            return this;
        }

        public Equipment build() {
            return new Equipment(armorType, armorEnchants, weapon, weaponEnchants);
        }
    }
}
