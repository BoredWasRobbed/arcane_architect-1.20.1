package net.bored.block;

import net.bored.block.entity.RuneBlockEntity;
import net.bored.block.enums.RuneType;
import net.bored.registry.ModBlockEntities;
import net.bored.system.RitualSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class RuneBlock extends AbstractRuneBlock {
    public static final EnumProperty<RuneType> TYPE = EnumProperty.of("type", RuneType.class);

    public RuneBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(TYPE, RuneType.AETHER));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(TYPE);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.RUNE_BLOCK_ENTITY, RuneBlockEntity::tick);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof RuneBlockEntity rune) {
                if (!rune.getStack().isEmpty()) {
                    ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), rune.getStack());
                }
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        RuneType type = state.get(TYPE);
        BlockEntity be = world.getBlockEntity(pos);
        ItemStack handStack = player.getStackInHand(hand);

        if (type == RuneType.ITEM && be instanceof RuneBlockEntity rune) {
            ItemStack runeStack = rune.getStack();

            if (!runeStack.isEmpty()) {
                if (!world.isClient) {
                    player.getInventory().offerOrDrop(runeStack);
                    rune.setStack(ItemStack.EMPTY);
                    world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 1.0f, 1.0f);
                }
                return ActionResult.SUCCESS;
            }

            if (!handStack.isEmpty()) {
                if (!world.isClient) {
                    rune.setStack(handStack.split(1));
                    world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1.0f, 1.0f);
                }
                return ActionResult.SUCCESS;
            }
        }

        if (handStack.isEmpty()) {
            if (player.isSneaking()) {
                if (!world.isClient) {
                    // Pass player to check ownership
                    RitualSystem.tryActivate((ServerWorld) world, pos, (ServerPlayerEntity) player);
                }
                return ActionResult.SUCCESS;
            }

            if (!world.isClient) {
                BlockState newState = state.cycle(TYPE);
                world.setBlockState(pos, newState);
                RuneType newType = newState.get(TYPE);
                player.sendMessage(Text.literal("§bRune attuned to: §f" + newType.name()), true);
                world.playSound(null, pos, SoundEvents.BLOCK_COMPARATOR_CLICK, SoundCategory.BLOCKS, 1.0f, 1.5f);
            }
            return ActionResult.SUCCESS;
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }
}