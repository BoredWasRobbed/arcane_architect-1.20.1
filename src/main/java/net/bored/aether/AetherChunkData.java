package net.bored.aether;

import net.minecraft.nbt.NbtCompound;

public class AetherChunkData {
    private float pressure = 0.5f; // Default balanced pressure
    private float stability = 1.0f; // 1.0 = stable, 0.0 = chaotic
    private long lastUpdateTick = 0;

    public AetherChunkData() {
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        // Clamp between 0.0 and 1.0
        this.pressure = Math.max(0.0f, Math.min(1.0f, pressure));
    }

    public float getStability() {
        return stability;
    }

    public void setStability(float stability) {
        this.stability = Math.max(0.0f, Math.min(1.0f, stability));
    }

    public long getLastUpdateTick() {
        return lastUpdateTick;
    }

    public void setLastUpdateTick(long tick) {
        this.lastUpdateTick = tick;
    }

    // NBT Serialization
    public void writeNbt(NbtCompound nbt) {
        nbt.putFloat("aa_pressure", pressure);
        nbt.putFloat("aa_stability", stability);
        nbt.putLong("aa_last_update", lastUpdateTick);
    }

    public void readNbt(NbtCompound nbt) {
        if (nbt.contains("aa_pressure")) {
            this.pressure = nbt.getFloat("aa_pressure");
        }
        if (nbt.contains("aa_stability")) {
            this.stability = nbt.getFloat("aa_stability");
        }
        if (nbt.contains("aa_last_update")) {
            this.lastUpdateTick = nbt.getLong("aa_last_update");
        }
    }
}