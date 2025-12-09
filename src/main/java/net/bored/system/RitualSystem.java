package net.bored.system;

import net.bored.access.AetherAttachment;
import net.bored.api.RitualEffect;
import net.bored.block.RuneBlock;
import net.bored.block.entity.RuneBlockEntity;
import net.bored.block.enums.RuneType;
import net.bored.recipe.RitualAugment;
import net.bored.recipe.RitualManager;
import net.bored.recipe.RitualRecipe;
import net.bored.registry.RitualEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

public class RitualSystem {

    private enum MatchStatus { SUCCESS, PRESSURE_FAIL, ITEM_FAIL, PATTERN_FAIL, NONE }
    private record MatchResult(RitualRecipe recipe, int rotation, MatchStatus status, Text failureReason, int score, List<BlockPos> augmentPositions) {}

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

        MatchResult bestResult = findBestCandidate(world, centerPos);

        if (bestResult.status == MatchStatus.SUCCESS) {
            startRitual(world, centerPos, rune, bestResult.recipe, bestResult.rotation, player);
        } else if (bestResult.status != MatchStatus.NONE) {
            world.playSound(null, centerPos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5f, 0.5f);
            player.sendMessage(Text.literal("§cRitual Failed: §7" + bestResult.recipe.getId().getPath()), true);
            player.sendMessage(bestResult.failureReason, true);
        } else {
            world.playSound(null, centerPos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5f, 0.5f);
            player.sendMessage(Text.literal("§7No recognizable ritual structure found."), true);
        }
    }

    public static void checkAndPulse(ServerWorld world, BlockPos centerPos) {
        BlockEntity be = world.getBlockEntity(centerPos);
        if (!(be instanceof RuneBlockEntity rune)) return;
        if (rune.isRitualActive()) return;

        if (!rune.cachedPassiveRecipeId.isEmpty()) {
            RitualRecipe cachedRecipe = RitualManager.INSTANCE.getAll().get(new Identifier(rune.cachedPassiveRecipeId));
            if (cachedRecipe != null) {
                float pressure = getPressure(world, centerPos);
                MatchResult quickCheck = analyzeRecipe(world, centerPos, cachedRecipe, rune.cachedPassiveRotation, pressure);

                if (quickCheck.status == MatchStatus.SUCCESS) {
                    pulseRunes(world, centerPos, quickCheck.recipe.getPattern(), quickCheck.augmentPositions, rune.cachedPassiveRotation, true);
                    return;
                }
            }
            rune.cachedPassiveRecipeId = "";
        }

        MatchResult match = findBestCandidate(world, centerPos);

        if (match.status == MatchStatus.SUCCESS) {
            rune.cachedPassiveRecipeId = match.recipe.getId().toString();
            rune.cachedPassiveRotation = match.rotation;
            rune.markDirty();

            pulseRunes(world, centerPos, match.recipe.getPattern(), match.augmentPositions, match.rotation, true);
        }
    }

    private static float getPressure(ServerWorld world, BlockPos pos) {
        Chunk chunk = world.getChunk(pos);
        if (chunk instanceof AetherAttachment attachment) {
            return attachment.getAetherData().getPressure();
        }
        return 0.5f;
    }

    private static MatchResult findBestCandidate(ServerWorld world, BlockPos centerPos) {
        float currentPressure = getPressure(world, centerPos);
        MatchResult bestMatch = new MatchResult(null, 0, MatchStatus.NONE, Text.empty(), -1, Collections.emptyList());

        for (RitualRecipe recipe : RitualManager.INSTANCE.getAll().values()) {
            for (int r = 0; r < 4; r++) {
                MatchResult result = analyzeRecipe(world, centerPos, recipe, r, currentPressure);

                if (result.status == MatchStatus.SUCCESS) {
                    if (bestMatch.status != MatchStatus.SUCCESS || result.score > bestMatch.score) {
                        bestMatch = result;
                    }
                }
                else if (bestMatch.status != MatchStatus.SUCCESS) {
                    if (result.score > bestMatch.score) {
                        bestMatch = result;
                    }
                }
            }
        }
        return bestMatch;
    }

    private static MatchResult analyzeRecipe(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation, float currentPressure) {
        Map<BlockPos, RuneType> pattern = recipe.getPattern();
        int totalRunes = pattern.size();
        int matchedRunes = 0;

        for (Map.Entry<BlockPos, RuneType> entry : pattern.entrySet()) {
            BlockPos worldPos = center.add(rotate(entry.getKey(), rotation));
            BlockState state = world.getBlockState(worldPos);

            if (state.getBlock() instanceof RuneBlock && state.get(RuneBlock.TYPE) == entry.getValue()) {
                matchedRunes++;
            }
        }

        int score = matchedRunes;
        boolean isStructureValid = matchedRunes == totalRunes;
        boolean isPartialMatch = (float) matchedRunes / totalRunes > 0.75f;

        if (!isStructureValid) {
            if (isPartialMatch) {
                return new MatchResult(recipe, rotation, MatchStatus.PATTERN_FAIL, Text.literal("§eIncomplete Rune Pattern."), score, Collections.emptyList());
            } else {
                return new MatchResult(recipe, rotation, MatchStatus.NONE, Text.empty(), 0, Collections.emptyList());
            }
        }

        List<RitualAugment> activeAugments = new ArrayList<>();
        List<BlockPos> augmentPositions = new ArrayList<>();

        for (RitualAugment aug : recipe.getAugments()) {
            BlockPos worldPos = center.add(rotate(aug.getOffset(), rotation));
            BlockState state = world.getBlockState(worldPos);

            if (state.getBlock() instanceof RuneBlock && state.get(RuneBlock.TYPE) == aug.getRune()) {
                activeAugments.add(aug);
                augmentPositions.add(aug.getOffset());
                score += 10;
            }
        }

        RitualRecipe effectiveRecipe = recipe.applyAugments(activeAugments);

        if (currentPressure < effectiveRecipe.getMinPressure() || currentPressure > effectiveRecipe.getMaxPressure()) {
            return new MatchResult(effectiveRecipe, rotation, MatchStatus.PRESSURE_FAIL,
                    Text.literal("§bUnstable Aether. §7Req: " + String.format("%.2f", effectiveRecipe.getMinPressure()) + " - " + String.format("%.2f", effectiveRecipe.getMaxPressure())),
                    score + 100, augmentPositions);
        }

        boolean itemsValid = effectiveRecipe.isShapeless() ?
                checkItemsShapeless(world, center, effectiveRecipe, rotation) :
                checkItemsStrict(world, center, effectiveRecipe, rotation);

        if (!itemsValid) {
            return new MatchResult(effectiveRecipe, rotation, MatchStatus.ITEM_FAIL, Text.literal("§6Missing or Incorrect Items."), score + 50, augmentPositions);
        }

        return new MatchResult(effectiveRecipe, rotation, MatchStatus.SUCCESS, Text.empty(), score + 1000, augmentPositions);
    }

    private static void startRitual(ServerWorld world, BlockPos center, RuneBlockEntity rune, RitualRecipe recipe, int rotation, ServerPlayerEntity player) {
        int startup = recipe.getStartupTime();
        int interval = recipe.getInterval();

        rune.setRunningRitual(recipe.getId(), rotation, startup, interval);

        // Fix #4: Trigger visual pulse immediately upon activation
        List<BlockPos> augOffsets = new ArrayList<>();
        for(RitualAugment aug : recipe.getAugments()) augOffsets.add(aug.getOffset());
        pulseRunes(world, center, recipe.getPattern(), augOffsets, rotation, false);

        if (startup > 0) {
            world.playSound(null, center, SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, 1.0f, 0.5f);
            player.sendMessage(Text.literal("§bRitual Charging..."), true);
        } else {
            executeEffect(world, center, recipe, rotation);
        }
    }

    public static void tickStartup(ServerWorld world, BlockPos center, RuneBlockEntity rune) {
        String id = rune.getActiveRitualId();
        RitualRecipe recipe = RitualManager.INSTANCE.getAll().get(new Identifier(id));
        if (recipe == null) return;

        Map<BlockPos, RuneType> pattern = recipe.getPattern();
        int rot = rune.activeRotation;

        if (world.getTime() % 2 == 0) {
            for (BlockPos offset : pattern.keySet()) {
                if (offset.equals(BlockPos.ORIGIN)) continue;

                BlockPos runePos = center.add(rotate(offset, rot));
                Vec3d start = new Vec3d(runePos.getX() + 0.5, runePos.getY() + 0.5, runePos.getZ() + 0.5);
                Vec3d target = new Vec3d(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
                Vec3d dir = target.subtract(start).normalize().multiply(0.2);

                world.spawnParticles(ParticleTypes.ENCHANT, start.x, start.y, start.z, 0, dir.x, dir.y, dir.z, 1.0);
            }
        }
    }

    public static void finishStartup(ServerWorld world, BlockPos center, RuneBlockEntity rune) {
        String id = rune.getActiveRitualId();
        RitualRecipe recipe = RitualManager.INSTANCE.getAll().get(new Identifier(id));
        if (recipe == null) return;

        if (recipe.isContinuous()) {
            world.playSound(null, center, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        } else {
            MatchResult check = analyzeRecipe(world, center, recipe, rune.activeRotation, getPressure(world, center));
            if (check.status == MatchStatus.SUCCESS) {
                executeEffect(world, center, check.recipe, rune.activeRotation);
            }
            rune.stopRitual();
        }
    }

    public static void tickActiveRitual(ServerWorld world, BlockPos center, RuneBlockEntity rune) {
        String id = rune.getActiveRitualId();
        RitualRecipe recipe = RitualManager.INSTANCE.getAll().get(new Identifier(id));
        if (recipe == null) { rune.stopRitual(); return; }

        int rot = rune.activeRotation;

        MatchResult check = analyzeRecipe(world, center, recipe, rot, getPressure(world, center));

        if (check.status != MatchStatus.SUCCESS) {
            rune.stopRitual();
            world.playSound(null, center, SoundEvents.BLOCK_CONDUIT_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            return;
        }

        if (check.recipe.consumesItem()) {
            consumeItemFromPattern(world, center, check.recipe, rot);
        }

        pulseRunes(world, center, check.recipe.getPattern(), check.augmentPositions, rot, false);
        executeEffect(world, center, check.recipe, rot);
    }

    private static void consumeItemFromPattern(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation) {
        if (recipe.isShapeless()) {
            consumeShapeless(world, center, recipe, rotation);
        } else {
            consumeStrict(world, center, recipe, rotation);
        }
    }

    private static void consumeStrict(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation) {
        String globalReq = recipe.getRequiredItem();

        for (Map.Entry<BlockPos, RuneType> entry : recipe.getPattern().entrySet()) {
            if (entry.getValue() != RuneType.ITEM) continue;

            BlockPos defPos = entry.getKey();
            BlockPos worldPos = center.add(rotate(defPos, rotation));

            String specificItem = recipe.getItemRequirements().get(defPos);
            String targetItem = (specificItem != null && !specificItem.isEmpty()) ? specificItem : globalReq;

            if (targetItem == null || targetItem.isEmpty()) continue;

            BlockEntity be = world.getBlockEntity(worldPos);
            if (be instanceof RuneBlockEntity rune) {
                ItemStack stack = rune.getStack();
                if (!stack.isEmpty()) {
                    Identifier itemId = Registries.ITEM.getId(stack.getItem());
                    if (itemId.toString().equals(targetItem)) {
                        processConsumption(recipe, rune, stack);
                    }
                }
            }
        }
    }

    private static void consumeShapeless(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation) {
        List<String> toConsume = new ArrayList<>();
        String globalReq = recipe.getRequiredItem();
        for (Map.Entry<BlockPos, RuneType> entry : recipe.getPattern().entrySet()) {
            if (entry.getValue() == RuneType.ITEM) {
                String specific = recipe.getItemRequirements().get(entry.getKey());
                String target = (specific != null && !specific.isEmpty()) ? specific : globalReq;
                if (target != null && !target.isEmpty()) {
                    toConsume.add(target);
                }
            }
        }

        for (Map.Entry<BlockPos, RuneType> entry : recipe.getPattern().entrySet()) {
            if (entry.getValue() != RuneType.ITEM) continue;

            BlockPos worldPos = center.add(rotate(entry.getKey(), rotation));
            BlockEntity be = world.getBlockEntity(worldPos);

            if (be instanceof RuneBlockEntity rune) {
                ItemStack stack = rune.getStack();
                if (!stack.isEmpty()) {
                    String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                    if (toConsume.contains(itemId)) {
                        toConsume.remove(itemId);
                        processConsumption(recipe, rune, stack);
                    }
                }
            }
        }
    }

    private static void processConsumption(RitualRecipe recipe, RuneBlockEntity rune, ItemStack stack) {
        if (recipe.clearsItems()) {
            rune.setStack(ItemStack.EMPTY);
        } else {
            stack.decrement(1);
            rune.setStack(stack);
        }
    }

    private static boolean checkItemsStrict(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation) {
        String globalReq = recipe.getRequiredItem();
        boolean requireAll = recipe.requiresAllItems();
        boolean itemSatisfied = false;
        boolean hasItemReq = false;

        for (Map.Entry<BlockPos, RuneType> entry : recipe.getPattern().entrySet()) {
            if (entry.getValue() != RuneType.ITEM) continue;

            BlockPos defPos = entry.getKey();
            BlockPos worldPos = center.add(rotate(defPos, rotation));

            String specific = recipe.getItemRequirements().get(defPos);
            String target = (specific != null && !specific.isEmpty()) ? specific : globalReq;

            if (target != null && !target.isEmpty()) {
                hasItemReq = true;
                boolean slotValid = false;
                BlockEntity be = world.getBlockEntity(worldPos);
                if (be instanceof RuneBlockEntity rune) {
                    ItemStack stack = rune.getStack();
                    if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(target)) {
                        slotValid = true;
                    }
                }

                if (requireAll && !slotValid) return false;
                if (slotValid) itemSatisfied = true;
            }
        }

        if (hasItemReq && !requireAll) return itemSatisfied;
        return true;
    }

    private static boolean checkItemsShapeless(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation) {
        List<String> required = new ArrayList<>();
        String globalReq = recipe.getRequiredItem();

        for (Map.Entry<BlockPos, RuneType> entry : recipe.getPattern().entrySet()) {
            if (entry.getValue() == RuneType.ITEM) {
                String specific = recipe.getItemRequirements().get(entry.getKey());
                String target = (specific != null && !specific.isEmpty()) ? specific : globalReq;
                if (target != null && !target.isEmpty()) {
                    required.add(target);
                }
            }
        }

        if (required.isEmpty()) return true;

        List<String> available = new ArrayList<>();
        for (Map.Entry<BlockPos, RuneType> entry : recipe.getPattern().entrySet()) {
            if (entry.getValue() == RuneType.ITEM) {
                BlockPos worldPos = center.add(rotate(entry.getKey(), rotation));
                BlockEntity be = world.getBlockEntity(worldPos);
                if (be instanceof RuneBlockEntity rune) {
                    ItemStack stack = rune.getStack();
                    if (!stack.isEmpty()) {
                        available.add(Registries.ITEM.getId(stack.getItem()).toString());
                    }
                }
            }
        }

        boolean requireAll = recipe.requiresAllItems();

        if (requireAll) {
            List<String> availableCopy = new ArrayList<>(available);
            for (String req : required) {
                if (!availableCopy.remove(req)) {
                    return false;
                }
            }
            return true;
        } else {
            for (String req : required) {
                if (available.contains(req)) return true;
            }
            return false;
        }
    }

    private static BlockPos rotate(BlockPos pos, int rotation) {
        return switch (rotation) {
            case 1 -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case 2 -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case 3 -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            default -> pos;
        };
    }

    private static void pulseRunes(ServerWorld world, BlockPos center, Map<BlockPos, RuneType> pattern, List<BlockPos> augmentOffsets, int rotation, boolean weak) {
        if (world.getBlockEntity(center) instanceof RuneBlockEntity rune) {
            if (rune.activationTimer == 0) rune.activate(weak);
        }

        for (BlockPos offset : pattern.keySet()) {
            activateAt(world, center.add(rotate(offset, rotation)), weak);
        }

        for (BlockPos offset : augmentOffsets) {
            activateAt(world, center.add(rotate(offset, rotation)), weak);
        }
    }

    private static void activateAt(ServerWorld world, BlockPos pos, boolean weak) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof RuneBlockEntity rune) {
            if (rune.activationTimer == 0) rune.activate(weak);
        }
    }

    private static void executeEffect(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation) {
        RitualEffect effect = RitualEffects.get(recipe.getEffectId());

        if (effect != null) {
            UUID owner = null;
            if (world.getBlockEntity(center) instanceof RuneBlockEntity rune) {
                owner = rune.getOwner();
            }
            effect.run(world, center, recipe, rotation, owner);
        } else {
            System.out.println("Unknown ritual effect: " + recipe.getEffectId());
        }

        if (!recipe.isContinuous()) {
            pulseRunes(world, center, recipe.getPattern(), Collections.emptyList(), rotation, false);
            world.playSound(null, center, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
    }
}