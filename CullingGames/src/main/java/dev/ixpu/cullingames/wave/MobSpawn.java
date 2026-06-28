package dev.ixpu.cullingames.wave;

public class MobSpawn {

    private final MobSpawnData mobData;

    public MobSpawn(MobSpawnData mobData) {
        this.mobData = mobData;
    }

    public String getMobTypeId() {
        return mobData.getMobTypeId();
    }

    public int getAmount() {
        return mobData.getAmount();
    }

    public int getPointValue() {
        return mobData.getPointValue();
    }

    public MobSpawnData getMobData() {
        return mobData;
    }
}
