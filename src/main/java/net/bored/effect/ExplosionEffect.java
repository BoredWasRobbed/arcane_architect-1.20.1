package net.bored.effect;

import net.bored.api.RitualEffect;
import net.bored.recipe.RitualRecipe;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;

public class ExplosionEffect implements RitualEffect {
    @Override
    public void run(ServerWorld world, BlockPos center, RitualRecipe recipe, int rotation, UUID owner) {
        float power = recipe.getEffectData().has("power") ? recipe.getEffectData().get("power").getAsFloat() : 2.0f;
        world.createExplosion(null, center.getX(), center.getY() + 1, center.getZ(), power, ServerWorld.ExplosionSourceType.NONE);
    }
}