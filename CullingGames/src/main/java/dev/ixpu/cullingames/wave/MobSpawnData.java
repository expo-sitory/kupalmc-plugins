package dev.ixpu.cullingames.wave;

import java.util.*;

public class MobSpawnData {
    private final String mobTypeId;
    private final int amount;
    private final int pointValue;
    private final String entityType;
    private final String customName;
    private final Equipment equipment;
    private final Map<String, Double> attributes;
    private final Map<String, Integer> customEffects;

    public MobSpawnData(
            String mobTypeId,
            int amount,
            int pointValue,
            String entityType,
            String customName,
            Equipment equipment,
            Map<String, Double> attributes,
            Map<String, Integer> customEffects
    ) {
        this.mobTypeId = mobTypeId;
        this.amount = amount;
        this.pointValue = pointValue;
        this.entityType = entityType;
        this.customName = customName;
        this.equipment = equipment;
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
        this.customEffects = customEffects != null ? new HashMap<>(customEffects) : new HashMap<>();
    }

    public String getMobTypeId() {
        return mobTypeId;
    }

    public int getAmount() {
        return amount;
    }

    public int getPointValue() {
        return pointValue;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getCustomName() {
        return customName;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public Map<String, Double> getAttributes() {
        return new HashMap<>(attributes);
    }

    public Map<String, Integer> getCustomEffects() {
        return new HashMap<>(customEffects);
    }

    public static class Builder {
        private String mobTypeId;
        private int amount = 1;
        private int pointValue = 0;
        private String entityType;
        private String customName;
        private Equipment equipment = new Equipment("", new ArrayList<>(), "", new ArrayList<>());
        private Map<String, Double> attributes = new HashMap<>();
        private Map<String, Integer> customEffects = new HashMap<>();

        public Builder mobTypeId(String id) {
            this.mobTypeId = id;
            return this;
        }

        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        public Builder points(int points) {
            this.pointValue = points;
            return this;
        }

        public Builder entityType(String type) {
            this.entityType = type;
            return this;
        }

        public Builder customName(String name) {
            this.customName = name;
            return this;
        }

        public Builder equipment(Equipment equipment) {
            this.equipment = equipment;
            return this;
        }

        public Builder addAttribute(String attribute, double value) {
            this.attributes.put(attribute, value);
            return this;
        }

        public Builder addCustomEffect(String effect, int amplifier) {
            this.customEffects.put(effect, amplifier);
            return this;
        }

        public MobSpawnData build() {
            return new MobSpawnData(mobTypeId, amount, pointValue, entityType, customName, equipment, attributes, customEffects);
        }
    }
}
