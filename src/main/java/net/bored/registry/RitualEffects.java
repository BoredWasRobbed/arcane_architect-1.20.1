package net.bored.registry;

import net.bored.ArcaneArchitect;
import net.bored.api.RitualEffect;
import net.bored.effect.ExplosionEffect;
import net.bored.effect.LevitationEffect;
import net.bored.effect.LightningEffect;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class RitualEffects {
    private static final Map<Identifier, RitualEffect> EFFECTS = new HashMap<>();

    public static final Identifier LIGHTNING = new Identifier(ArcaneArchitect.MOD_ID, "lightning");
    public static final Identifier EXPLOSION = new Identifier(ArcaneArchitect.MOD_ID, "explosion");
    public static final Identifier LEVITATION = new Identifier(ArcaneArchitect.MOD_ID, "levitation");

    public static void registerEffects() {
        register(LIGHTNING, new LightningEffect());
        register(EXPLOSION, new ExplosionEffect());
        register(LEVITATION, new LevitationEffect());
    }

    public static void register(Identifier id, RitualEffect effect) {
        EFFECTS.put(id, effect);
    }

    public static RitualEffect get(Identifier id) {
        return EFFECTS.get(id);
    }

    public static RitualEffect get(String id) {
        return EFFECTS.get(new Identifier(id));
    }
}