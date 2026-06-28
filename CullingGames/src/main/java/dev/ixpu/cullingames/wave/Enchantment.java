package dev.ixpu.cullingames.wave;

public class Enchantment {
    private final String name;
    private final int level;

    public Enchantment(String name, int level) {
        this.name = name;
        this.level = level;
    }

    public static Enchantment parse(String enchantString) {
        String[] parts = enchantString.split(":");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new Enchantment(parts[0].trim(), Integer.parseInt(parts[1].trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return name + ":" + level;
    }
}
