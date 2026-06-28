package dev.ixpu.cullingames.wave;

import java.util.List;

public class WaveDefinition {

    private final String name;
    private final int order;
    private final List<MobSpawn> mobs;

    public WaveDefinition(String name, int order, List<MobSpawn> mobs) {
        this.name = name;
        this.order = order;
        this.mobs = mobs;
    }

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public List<MobSpawn> getMobs() {
        return mobs;
    }

    public int getTotalMobCount() {
        return mobs.stream().mapToInt(MobSpawn::getAmount).sum();
    }
}
