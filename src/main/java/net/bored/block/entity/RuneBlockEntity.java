package net.bored.block.entity;

import net.bored.registry.ModBlockEntities;
import net.bored.ritual.RitualSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RuneBlockEntity extends BlockEntity {

    public float tickCounter = 0;
    public int activationTimer = 0;
    public boolean isWeakPulse = false;

    public static final int MAX_PASSIVE_TIME = 60;
    public static final int MAX_ACTIVE_TIME = 20;

    private String activeRitualId = "";
    private boolean isRitualActive = false;
    private int ritualTickTimer = 0;

    public int activeRotation = 0;
    public int startupTimer = 0;
    public int totalStartupTime = 0;
    public int interval = 20;

    public String cachedPassiveRecipeId = "";
    public int cachedPassiveRotation = 0;

    private UUID owner = null;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);

    public RuneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUNE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, RuneBlockEntity be) {
        be.tickCounter++;

        if (be.activationTimer > 0) {
            be.activationTimer--;
        }

        if (!world.isClient) {
            // 1. Handle Startup Phase
            if (be.startupTimer > 0) {
                be.startupTimer--;
                RitualSystem.tickStartup((ServerWorld) world, pos, be);

                if (be.startupTimer == 0) {
                    RitualSystem.finishStartup((ServerWorld) world, pos, be);
                }
                return;
            }

            // 2. Passive Scan (Idle)
            // Fix #2 & #3: Sync to World Time and ensure we don't pulse if already pulsing (prevents component runes from overriding center)
            if (world.getTime() % 80 == 0 && !be.isRitualActive && be.activationTimer == 0) {
                RitualSystem.checkAndPulse((ServerWorld) world, pos);
            }

            // 3. Active Ritual Logic
            if (be.isRitualActive && !be.activeRitualId.isEmpty()) {
                be.ritualTickTimer++;
                if (be.ritualTickTimer >= be.interval) {
                    be.ritualTickTimer = 0;
                    RitualSystem.tickActiveRitual((ServerWorld) world, pos, be);
                }
            }
        }
    }

    public void setOwner(UUID uuid) {
        this.owner = uuid;
        markDirty();
    }

    public UUID getOwner() {
        return owner;
    }

    public void setRunningRitual(Identifier id, int rotation, int startupTime, int interval) {
        this.activeRitualId = id.toString();
        this.activeRotation = rotation;
        this.startupTimer = startupTime;
        this.totalStartupTime = startupTime;
        this.interval = Math.max(1, interval);
        this.isRitualActive = true;
        this.ritualTickTimer = 0;
        markDirty();
    }

    public void stopRitual() {
        this.activeRitualId = "";
        this.isRitualActive = false;
        this.startupTimer = 0;
        markDirty();
    }

    public String getActiveRitualId() { return activeRitualId; }
    public boolean isRitualActive() { return isRitualActive; }

    public void activate(boolean weak) {
        this.isWeakPulse = weak;
        this.activationTimer = weak ? MAX_PASSIVE_TIME : MAX_ACTIVE_TIME;
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    public ItemStack getStack() { return inventory.get(0); }

    public void setStack(ItemStack stack) {
        inventory.set(0, stack);
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putFloat("tickCounter", tickCounter);
        nbt.putInt("activationTimer", activationTimer);
        nbt.putBoolean("isWeakPulse", isWeakPulse);
        Inventories.writeNbt(nbt, inventory);

        nbt.putString("ActiveRitualId", activeRitualId);
        nbt.putBoolean("IsRitualActive", isRitualActive);
        nbt.putInt("ActiveRotation", activeRotation);
        nbt.putInt("StartupTimer", startupTimer);
        nbt.putInt("Interval", interval);

        if (owner != null) {
            nbt.putUuid("Owner", owner);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("tickCounter")) this.tickCounter = nbt.getFloat("tickCounter");
        if (nbt.contains("activationTimer")) this.activationTimer = nbt.getInt("activationTimer");
        if (nbt.contains("isWeakPulse")) this.isWeakPulse = nbt.getBoolean("isWeakPulse");
        inventory.clear();
        Inventories.readNbt(nbt, inventory);

        if (nbt.contains("ActiveRitualId")) {
            this.activeRitualId = nbt.getString("ActiveRitualId");
            this.isRitualActive = nbt.getBoolean("IsRitualActive");
            this.activeRotation = nbt.getInt("ActiveRotation");
            this.startupTimer = nbt.getInt("StartupTimer");
            if (nbt.contains("Interval")) {
                this.interval = Math.max(1, nbt.getInt("Interval"));
            }
        }

        if (nbt.containsUuid("Owner")) {
            this.owner = nbt.getUuid("Owner");
        }
    }

    @Nullable
    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}