package net.bored.block.entity;

import net.bored.registry.ModBlockEntities;
import net.bored.system.RitualSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RuneBlockEntity extends BlockEntity {

    public float tickCounter = 0;
    public int activationTimer = 0;
    public static final int MAX_ACTIVATION_TIME = 60;

    // Ritual State
    private String activeRitualId = "";
    private boolean isRitualActive = false;
    private int ritualTickTimer = 0;

    // --- New: Ownership ---
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

        if (!world.isClient && be.isRitualActive && !be.activeRitualId.isEmpty()) {
            be.ritualTickTimer++;
            if (be.ritualTickTimer >= 20) {
                be.ritualTickTimer = 0;
                RitualSystem.tickActiveRitual((net.minecraft.server.world.ServerWorld) world, pos, be);
            }
        }
    }

    // --- Ownership Methods ---
    public void setOwner(UUID uuid) {
        this.owner = uuid;
        markDirty();
    }

    public UUID getOwner() {
        return owner;
    }

    // --- Existing Methods ---
    public void setRunningRitual(Identifier id) {
        this.activeRitualId = id.toString();
        this.isRitualActive = true;
        markDirty();
    }

    public void stopRitual() {
        this.activeRitualId = "";
        this.isRitualActive = false;
        markDirty();
    }

    public String getActiveRitualId() { return activeRitualId; }
    public boolean isRitualActive() { return isRitualActive; }

    public void activate() {
        this.activationTimer = MAX_ACTIVATION_TIME;
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
        Inventories.writeNbt(nbt, inventory);

        nbt.putString("ActiveRitualId", activeRitualId);
        nbt.putBoolean("IsRitualActive", isRitualActive);

        if (owner != null) {
            nbt.putUuid("Owner", owner);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("tickCounter")) this.tickCounter = nbt.getFloat("tickCounter");
        if (nbt.contains("activationTimer")) this.activationTimer = nbt.getInt("activationTimer");
        inventory.clear();
        Inventories.readNbt(nbt, inventory);

        if (nbt.contains("ActiveRitualId")) {
            this.activeRitualId = nbt.getString("ActiveRitualId");
            this.isRitualActive = nbt.getBoolean("IsRitualActive");
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