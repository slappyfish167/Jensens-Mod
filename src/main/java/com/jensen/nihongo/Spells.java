package com.jensen.nihongo;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/**
 * Kotodama (言霊, "word spirit") spells: say a Japanese word you have learned out loud and it
 * takes effect. A playful reason to practise speaking outside of study sessions.
 */
public final class Spells {
    private Spells() {}

    /** @param vocabId the word that must be learned and spoken to cast it */
    public record Spell(String id, String vocabId, String english, int seconds, int amplifier) {
        public Holder<MobEffect> effect() {
            return switch (id) {
                case "light" -> MobEffects.NIGHT_VISION;
                case "water" -> MobEffects.WATER_BREATHING;
                case "fast" -> MobEffects.MOVEMENT_SPEED;
                case "strong" -> MobEffects.DAMAGE_BOOST;
                case "jump" -> MobEffects.JUMP;
                case "protect" -> MobEffects.DAMAGE_RESISTANCE;
                case "genki" -> MobEffects.REGENERATION;
                case "fire" -> MobEffects.FIRE_RESISTANCE;
                default -> MobEffects.LUCK;
            };
        }
    }

    public static final List<Spell> ALL = List.of(
            new Spell("light", "v_hikari", "Night Vision", 60, 0),
            new Spell("water", "v_mizu", "Water Breathing", 60, 0),
            new Spell("fast", "v_hayai", "Speed", 30, 0),
            new Spell("strong", "v_tsuyoi", "Strength", 20, 0),
            new Spell("jump", "v_tobimasu", "Jump Boost", 30, 1),
            new Spell("protect", "v_mamorimasu", "Resistance", 20, 0),
            new Spell("genki", "v_genki", "Regeneration", 8, 0),
            new Spell("fire", "v_hi", "Fire Resistance", 20, 0)
    );

    public static Spell byId(String id) {
        for (Spell s : ALL) if (s.id().equals(id)) return s;
        return null;
    }
}
