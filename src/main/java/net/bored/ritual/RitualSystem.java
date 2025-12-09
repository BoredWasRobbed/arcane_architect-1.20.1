package net.bored.ritual;

import net.bored.aether.AetherAttachment;
import net.bored.aether.AetherChunkData;
import net.bored.block.AbstractRuneBlock;
import net.bored.block.RuneBlock;
import net.bored.block.entity.RuneBlockEntity;
import net.bored.block.enums.RuneType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

public class RitualSystem {

    private enum MatchStatus { SUCCESS, PRESSURE_FAIL, ITEM_FAIL, PATTERN_FAIL, NONE }
    private record MatchResult(RitualRecipe recipe, int rotation, MatchStatus status, Text failureReason, int score, List<BlockPos> augmentPositions) {}

    public static void tryActivate(ServerWorld world, BlockPos centerPos) {}

    public static void tryActivate(ServerWorld world, BlockPos centerPos, ServerPlayerEntity player) {
        BlockState state = world.getBlockState(centerPos);

        // Venting Logic
        if (state.getBlock() instanceof RuneBlock && state.get(RuneBlock.TYPE) == RuneType.VENT) {
            triggerVent(world, centerPos, player);
            return;
        }

        BlockEntity be = world.getBlockEntity(centerPos);
        if (!(be instanceof RuneBlockEntity rune)) return;

        // Permanent Ownership Logic
        if (rune.getOwner() == null) {
            rune.setOwner(player.getUuid());
            player.sendMessage(Text.literal("§bYou have claimed this ritual."), true);
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

    private static void triggerVent(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        Chunk chunk = world.getChunk(pos);
        if (chunk instanceof AetherAttachment attachment) {
            AetherChunkData data = attachment.getAetherData();

            world.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 0.5f);
            world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 50, 0.5, 0.5, 0.5, 0.1);
            world.spawnParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 1.0, 0.5, 1.0, 0.2);

            data.setStability(1.0f);
            float currentP = data.getPressure();
            data.setPressure(Math.max(0.0f, currentP - 0.3f));

            player.sendMessage(Text.literal("§7[Vent] §fStability Restored. §bPressure dropped."), true);
        }
    }

    public static void checkAndPulse(ServerWorld world, BlockPos centerPos) {
        BlockEntity be = world.getBlockEntity(centerPos);
        if (!(be instanceof RuneBlockEntity rune)) return;
        if (rune.isRitualActive()) return;

        MatchResult match = findBestCandidate(world, centerPos);

        if (match.status == MatchStatus.SUCCESS) {
            rune.cachedPassiveRecipeId = match.recipe.getId().toString();
            rune.cachedPassiveRotation = match.rotation;
            rune.markDirty();

            // Get facing state and pass it to pulseRunes
            BlockState state = world.getBlockState(centerPos);
            Direction facing = state.contains(AbstractRuneBlock.FACING) ? state.get(AbstractRuneBlock.FACING) : Direction.UP;

            pulseRunes(world, centerPos, match.recipe.getPattern(), match.augmentPositions, match.rotation, true, facing);
        } else {
            if (!rune.cachedPassiveRecipeId.isEmpty()) {
                rune.cachedPassiveRecipeId = "";
                rune.markDirty();
            }
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

        BlockState state = world.getBlockState(centerPos);
        Direction facing = state.contains(AbstractRuneBlock.FACING) ? state.get(AbstractRuneBlock.FACING) : Direction.UP;

        MatchResult bestMatch = new MatchResult(null, 0, MatchStatus.NONE, Text.empty(), -1, Collections.emptyList());

        for (RitualRecipe recipe : RitualManager.INSTANCE.getAll().values()) {
            for (int r = 0; r < 4; r++) {
                MatchResult result = analyzeRecipe(world, centerPos, recipe, r, currentPressure, facing);

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

    private static MatchResult analyzeRecipe(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation, float currentPressure, Direction facing) {
        Map<BlockPos, RuneType> pattern = recipe.getPattern();
        int totalRunes = pattern.size();
        int matchedRunes = 0;

        for (Map.Entry<BlockPos, RuneType> entry : pattern.entrySet()) {
            BlockPos offset = transform(entry.getKey(), facing, rotation);
            BlockPos worldPos = center.add(offset);

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
            BlockPos offset = transform(aug.getOffset(), facing, rotation);
            BlockPos worldPos = center.add(offset);
            BlockState state = world.getBlockState(worldPos);

            if (state.getBlock() instanceof RuneBlock && state.get(RuneBlock.TYPE) == aug.getRune()) {
                activeAugments.add(aug);
                // Store the raw recipe offset, not the transformed world offset.
                // pulseRunes will handle the transformation.
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
                checkItemsShapeless(world, center, effectiveRecipe, rotation, facing) :
                checkItemsStrict(world, center, effectiveRecipe, rotation, facing);

        if (!itemsValid) {
            return new MatchResult(effectiveRecipe, rotation, MatchStatus.ITEM_FAIL, Text.literal("§6Missing or Incorrect Items."), score + 50, augmentPositions);
        }

        return new MatchResult(effectiveRecipe, rotation, MatchStatus.SUCCESS, Text.empty(), score + 1000, augmentPositions);
    }

    private static void startRitual(ServerWorld world, BlockPos center, RuneBlockEntity rune, RitualRecipe recipe, int rotation, ServerPlayerEntity player) {
        int startup = recipe.getStartupTime();
        int interval = recipe.getInterval();

        rune.setRunningRitual(recipe.getId(), rotation, startup, interval);

        List<BlockPos> augOffsets = new ArrayList<>();
        for(RitualAugment aug : recipe.getAugments()) augOffsets.add(aug.getOffset());

        BlockState state = world.getBlockState(center);
        Direction facing = state.contains(AbstractRuneBlock.FACING) ? state.get(AbstractRuneBlock.FACING) : Direction.UP;

        pulseRunes(world, center, recipe.getPattern(), augOffsets, rotation, false, facing);

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

        BlockState state = world.getBlockState(center);
        Direction facing = state.contains(AbstractRuneBlock.FACING) ? state.get(AbstractRuneBlock.FACING) : Direction.UP;

        if (world.getTime() % 2 == 0) {
            for (BlockPos offset : pattern.keySet()) {
                if (offset.equals(BlockPos.ORIGIN)) continue;

                BlockPos runePos = center.add(transform(offset, facing, rot));
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

        BlockState state = world.getBlockState(center);
        Direction facing = state.contains(AbstractRuneBlock.FACING) ? state.get(AbstractRuneBlock.FACING) : Direction.UP;

        if (recipe.isContinuous()) {
            world.playSound(null, center, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        } else {
            MatchResult check = analyzeRecipe(world, center, recipe, rune.activeRotation, getPressure(world, center), facing);
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

        Chunk chunk = world.getChunk(center);
        if (!(chunk instanceof AetherAttachment)) { rune.stopRitual(); return; }
        AetherChunkData data = ((AetherAttachment) chunk).getAetherData();

        // Consumption & Stability
        float currentP = data.getPressure();
        float currentS = data.getStability();

        if (currentP < recipe.getMinPressure()) {
            world.playSound(null, center, SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT, SoundCategory.BLOCKS, 0.5f, 0.5f);
            return;
        }

        if (currentS < 0.5f) {
            world.spawnParticles(ParticleTypes.SMOKE, center.getX()+0.5, center.getY()+0.5, center.getZ()+0.5, 3, 0.2, 0.2, 0.2, 0.05);
        }
        if (currentS < 0.2f) {
            if (world.random.nextFloat() < 0.3f) {
                world.playSound(null, center, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5f, 0.5f);
                return;
            }
        }

        data.setPressure(Math.max(0, currentP - recipe.getPressureCost()));
        data.setStability(Math.max(0, currentS - recipe.getInstabilityCost()));

        int rot = rune.activeRotation;
        BlockState state = world.getBlockState(center);
        Direction facing = state.contains(AbstractRuneBlock.FACING) ? state.get(AbstractRuneBlock.FACING) : Direction.UP;

        MatchResult check = analyzeRecipe(world, center, recipe, rot, currentP, facing);

        if (check.status != MatchStatus.SUCCESS) {
            rune.stopRitual();
            world.playSound(null, center, SoundEvents.BLOCK_CONDUIT_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            return;
        }

        if (check.recipe.consumesItem()) {
            consumeItemFromPattern(world, center, check.recipe, rot, facing);
        }

        pulseRunes(world, center, check.recipe.getPattern(), check.augmentPositions, rot, false, facing);
        executeEffect(world, center, check.recipe, rot);
    }

    private static void consumeItemFromPattern(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation, Direction facing) {
        if (recipe.isShapeless()) {
            consumeShapeless(world, center, recipe, rotation, facing);
        } else {
            consumeStrict(world, center, recipe, rotation, facing);
        }
    }

    private static void consumeStrict(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation, Direction facing) {
        String globalReq = recipe.getRequiredItem();

        for (Map.Entry<BlockPos, RuneType> entry : recipe.getPattern().entrySet()) {
            if (entry.getValue() != RuneType.ITEM) continue;

            BlockPos defPos = entry.getKey();
            BlockPos worldPos = center.add(transform(defPos, facing, rotation));

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

    private static void consumeShapeless(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation, Direction facing) {
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

            BlockPos worldPos = center.add(transform(entry.getKey(), facing, rotation));
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

    private static boolean checkItemsStrict(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation, Direction facing) {
        String globalReq = recipe.getRequiredItem();
        boolean requireAll = recipe.requiresAllItems();
        boolean itemSatisfied = false;
        boolean hasItemReq = false;

        for (Map.Entry<BlockPos, RuneType> entry : recipe.getPattern().entrySet()) {
            if (entry.getValue() != RuneType.ITEM) continue;

            BlockPos defPos = entry.getKey();
            BlockPos worldPos = center.add(transform(defPos, facing, rotation));

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

    private static boolean checkItemsShapeless(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation, Direction facing) {
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
                BlockPos worldPos = center.add(transform(entry.getKey(), facing, rotation));
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

    private static BlockPos transform(BlockPos pos, Direction facing, int rotation) {
        int px = pos.getX();
        int pz = pos.getZ();
        int py = pos.getY();

        int rx = switch(rotation) {
            case 1 -> -pz;
            case 2 -> -px;
            case 3 -> pz;
            default -> px;
        };
        int rz = switch(rotation) {
            case 1 -> px;
            case 2 -> -pz;
            case 3 -> -px;
            default -> pz;
        };
        int ry = py;

        return switch (facing) {
            case DOWN -> new BlockPos(rx, -ry, -rz);
            case NORTH -> new BlockPos(-rx, -rz, -ry);
            case SOUTH -> new BlockPos(rx, -rz, ry);
            case WEST -> new BlockPos(-ry, -rz, rx);
            case EAST -> new BlockPos(ry, -rz, -rx);
            default -> new BlockPos(rx, ry, rz);
        };
    }

    private static void pulseRunes(ServerWorld world, BlockPos center, Map<BlockPos, RuneType> pattern, List<BlockPos> augmentOffsets, int rotation, boolean weak, Direction facing) {
        if (world.getBlockEntity(center) instanceof RuneBlockEntity rune) {
            if (rune.activationTimer == 0) rune.activate(weak);
        }

        for (BlockPos offset : pattern.keySet()) {
            activateAt(world, center.add(transform(offset, facing, rotation)), weak);
        }

        for (BlockPos offset : augmentOffsets) {
            activateAt(world, center.add(transform(offset, facing, rotation)), weak);
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

            // DEBUG OVERLAY
            String ownerName = (owner != null) ? owner.toString().substring(0, 5) + "..." : "Null";
            boolean affectsOwner = recipe.affectsOwner();

            // Send to all players nearby
            world.getPlayers().forEach(p -> {
                if (p.squaredDistanceTo(center.getX(), center.getY(), center.getZ()) < 100) {
                    p.sendMessage(Text.literal("Ritual: §b" + recipe.getId().getPath() + " §f| Owner: §e" + ownerName + " §f| Affects Owner: §a" + affectsOwner), true);
                }
            });
        }

        BlockState state = world.getBlockState(center);
        Direction facing = state.contains(AbstractRuneBlock.FACING) ? state.get(AbstractRuneBlock.FACING) : Direction.UP;

        if (!recipe.isContinuous()) {
            pulseRunes(world, center, recipe.getPattern(), Collections.emptyList(), rotation, false, facing);
            world.playSound(null, center, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
    }
}