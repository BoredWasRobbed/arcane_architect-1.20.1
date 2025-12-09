package net.bored.ritual.effect;

import net.bored.ritual.RitualEffect;
import net.bored.ritual.RitualRecipe;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;

public class LightningEffect implements RitualEffect {
    @Override
    public void run(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation, UUID owner) {
        LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
        if (lightning != null) {
            lightning.refreshPositionAfterTeleport(center.getX(), center.getY(), center.getZ());
            world.spawnEntity(lightning);
        }
    }
}