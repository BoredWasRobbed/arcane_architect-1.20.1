package net.bored.effect;

import net.bored.api.RitualEffect;
import net.bored.recipe.RitualRecipe;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import java.util.UUID;

public class LevitationEffect implements RitualEffect {
    @Override
    public void run(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation, UUID owner) {
        world.getEntitiesByClass(net.minecraft.entity.LivingEntity.class,
                        new Box(center).expand(10), e -> true)
                .forEach(e -> {
                    // Check ownership rule
                    if (!recipe.affectsOwner() && owner != null && e.getUuid().equals(owner)) {
                        return; // Skip owner
                    }
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 40, 0, true, false));
                });
    }
}