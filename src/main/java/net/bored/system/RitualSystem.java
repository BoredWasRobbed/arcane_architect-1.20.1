package net.bored.system;

import net.bored.access.AetherAttachment;
import net.bored.block.RuneBlock;
import net.bored.block.entity.RuneBlockEntity;
import net.bored.block.enums.RuneType;
import net.bored.recipe.RitualManager;
import net.bored.recipe.RitualRecipe;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.Map;
import java.util.UUID;

public class RitualSystem {

    public static void tryActivate(ServerWorld world, BlockPos centerPos) {}

    public static void tryActivate(ServerWorld world, BlockPos centerPos, ServerPlayerEntity player) {
        BlockEntity be = world.getBlockEntity(centerPos);
        if (!(be instanceof RuneBlockEntity rune)) return;

        UUID owner = rune.getOwner();
        if (owner != null && !owner.equals(player.getUuid())) {
            player.sendMessage(Text.literal("§cYou are not the architect of this rune."), true);
            world.playSound(null, centerPos, SoundEvents.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 1.0f, 1.0f);
            return;
        }

        if (rune.isRitualActive()) {
            rune.stopRitual();
            world.playSound(null, centerPos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            player.sendMessage(Text.literal("§cRitual Deactivated."), true);
            return;
        }

        Chunk chunk = world.getChunk(centerPos);
        float currentPressure = 0.5f;
        if (chunk instanceof AetherAttachment attachment) {
            currentPressure = attachment.getAetherData().getPressure();
        }

        for (RitualRecipe recipe : RitualManager.INSTANCE.getAll().values()) {
            if (currentPressure < recipe.getMinPressure() || currentPressure > recipe.getMaxPressure()) {
                continue;
            }

            if (matchesPattern(world, centerPos, recipe)) {
                startRitual(world, centerPos, rune, recipe, player);
                return;
            }
        }

        world.playSound(null, centerPos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5f, 0.5f);
        player.sendMessage(Text.literal("§7No matching ritual found."), true);
    }

    private static void startRitual(ServerWorld world, BlockPos center, RuneBlockEntity rune, RitualRecipe recipe, ServerPlayerEntity player) {
        if (recipe.consumesItem() && !recipe.getRequiredItem().isEmpty()) {
            consumeItemFromPattern(world, center, recipe);
        }

        if (recipe.isContinuous()) {
            rune.setRunningRitual(recipe.getId());
            world.playSound(null, center, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            player.sendMessage(Text.literal("§aContinuous Ritual Started: " + recipe.getId()), true);
            tickActiveRitual(world, center, rune);
        } else {
            executeEffect(world, center, recipe);
        }
    }

    public static void tickActiveRitual(ServerWorld world, BlockPos center, RuneBlockEntity rune) {
        String id = rune.getActiveRitualId();
        RitualRecipe recipe = RitualManager.INSTANCE.getAll().get(new Identifier(id));

        if (recipe == null) {
            rune.stopRitual();
            return;
        }

        Chunk chunk = world.getChunk(center);
        float currentPressure = 0.5f;
        if (chunk instanceof AetherAttachment attachment) {
            currentPressure = attachment.getAetherData().getPressure();
        }

        boolean pressureValid = currentPressure >= recipe.getMinPressure() && currentPressure <= recipe.getMaxPressure();
        boolean patternValid = matchesPattern(world, center, recipe);

        if (!pressureValid || !patternValid) {
            rune.stopRitual();
            world.playSound(null, center, SoundEvents.BLOCK_CONDUIT_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            return;
        }

        if (recipe.consumesItem() && !recipe.getRequiredItem().isEmpty()) {
            consumeItemFromPattern(world, center, recipe);
        }

        activateRunes(world, center, recipe.getPattern());
        executeEffect(world, center, recipe);
    }

    private static void consumeItemFromPattern(ServerWorld world, BlockPos center, RitualRecipe recipe) {
        String requiredItem = recipe.getRequiredItem();

        for (Map.Entry<BlockPos, RuneType> entry : recipe.getPattern().entrySet()) {
            // Only care about runes defined as ITEM in the pattern
            if (entry.getValue() != RuneType.ITEM) continue;

            BlockPos checkPos = center.add(entry.getKey());
            BlockEntity be = world.getBlockEntity(checkPos);

            if (be instanceof RuneBlockEntity rune) {
                ItemStack stack = rune.getStack();
                if (!stack.isEmpty()) {
                    Identifier itemId = Registries.ITEM.getId(stack.getItem());
                    if (itemId.toString().equals(requiredItem)) {
                        stack.decrement(1);
                        rune.setStack(stack);

                        // If we don't require all items, we stop after consuming just one
                        if (!recipe.requiresAllItems()) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private static boolean matchesPattern(ServerWorld world, BlockPos center, RitualRecipe recipe) {
        Map<BlockPos, RuneType> pattern = recipe.getPattern();
        String requiredItem = recipe.getRequiredItem();
        boolean requireAll = recipe.requiresAllItems();
        boolean itemFound = requiredItem.isEmpty(); // True if no item needed

        // If requiring all, assume true initially, then fail if any missing
        if (requireAll && !requiredItem.isEmpty()) {
            itemFound = true;
        }

        for (Map.Entry<BlockPos, RuneType> entry : pattern.entrySet()) {
            BlockPos checkPos = center.add(entry.getKey());
            BlockState state = world.getBlockState(checkPos);

            if (!(state.getBlock() instanceof RuneBlock)) {
                return false;
            }

            RuneType type = state.get(RuneBlock.TYPE);
            if (type != entry.getValue()) {
                return false;
            }

            // ITEM CHECKING
            if (!requiredItem.isEmpty()) {
                // If this slot in the pattern expects an ITEM rune...
                if (entry.getValue() == RuneType.ITEM) {
                    boolean hasCorrectItem = false;
                    BlockEntity be = world.getBlockEntity(checkPos);
                    if (be instanceof RuneBlockEntity rune) {
                        ItemStack stack = rune.getStack();
                        if (!stack.isEmpty()) {
                            Identifier itemId = Registries.ITEM.getId(stack.getItem());
                            if (itemId.toString().equals(requiredItem)) {
                                hasCorrectItem = true;
                            }
                        }
                    }

                    if (requireAll) {
                        // Strict Mode: EVERY item rune must match
                        if (!hasCorrectItem) {
                            return false;
                        }
                    } else {
                        // Loose Mode: ANY item rune must match
                        if (hasCorrectItem) {
                            itemFound = true;
                        }
                    }
                }
            }
        }

        return itemFound;
    }

    private static void executeEffect(ServerWorld world, BlockPos center, RitualRecipe recipe) {
        String effect = recipe.getEffectId();

        if (effect.equals("arcanearchitect:lightning")) {
            LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
            if (lightning != null) {
                lightning.refreshPositionAfterTeleport(center.getX(), center.getY(), center.getZ());
                world.spawnEntity(lightning);
            }
        } else if (effect.equals("arcanearchitect:explosion")) {
            float power = recipe.getEffectData().has("power") ? recipe.getEffectData().get("power").getAsFloat() : 2.0f;
            world.createExplosion(null, center.getX(), center.getY() + 1, center.getZ(), power, ServerWorld.ExplosionSourceType.NONE);
        } else if (effect.equals("arcanearchitect:levitation")) {
            world.getEntitiesByClass(net.minecraft.entity.LivingEntity.class,
                            new net.minecraft.util.math.Box(center).expand(10), e -> true)
                    .forEach(e -> e.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.LEVITATION, 40, 0, true, false)));
        }

        if (!recipe.isContinuous()) {
            activateRunes(world, center, recipe.getPattern());
            world.playSound(null, center, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
    }

    private static void activateRunes(ServerWorld world, BlockPos center, Map<BlockPos, RuneType> pattern) {
        BlockEntity centerBe = world.getBlockEntity(center);
        if (centerBe instanceof RuneBlockEntity rune) {
            rune.activate();
        }
        for (BlockPos offset : pattern.keySet()) {
            BlockPos runePos = center.add(offset);
            BlockEntity be = world.getBlockEntity(runePos);
            if (be instanceof RuneBlockEntity rune) {
                rune.activate();
            }
        }
    }
}