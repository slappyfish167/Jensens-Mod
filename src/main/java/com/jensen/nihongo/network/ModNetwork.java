package com.jensen.nihongo.network;

import com.jensen.nihongo.NihongoMod;
import com.jensen.nihongo.ServerConfig;
import com.jensen.nihongo.Spells;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Learning happens on the client (progress follows the player between worlds); the server only
 * hands out rewards and applies spell effects. Payloads are optional so the mod still works on
 * servers that don't have it - you just don't get rewards there.
 */
public final class ModNetwork {
    private ModNetwork() {}

    public record CastSpell(String spellId) implements CustomPacketPayload {
        public static final Type<CastSpell> TYPE = new Type<>(NihongoMod.id("cast_spell"));
        public static final StreamCodec<ByteBuf, CastSpell> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, CastSpell::spellId, CastSpell::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** kind 0: study session finished with {@code amount} correct answers; kind 1: quest completed. */
    public record Reward(int kind, int amount) implements CustomPacketPayload {
        public static final int STUDY = 0;
        public static final int QUEST = 1;
        public static final Type<Reward> TYPE = new Type<>(NihongoMod.id("reward"));
        public static final StreamCodec<ByteBuf, Reward> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Reward::kind, ByteBufCodecs.VAR_INT, Reward::amount, Reward::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToServer(CastSpell.TYPE, CastSpell.CODEC, ModNetwork::handleSpell);
        registrar.playToServer(Reward.TYPE, Reward.CODEC, ModNetwork::handleReward);
    }

    private static final Map<UUID, Map<String, Long>> SPELL_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> LAST_STUDY_REWARD = new HashMap<>();
    private static final Map<UUID, Long> LAST_QUEST_REWARD = new HashMap<>();

    private static void handleSpell(CastSpell payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            Spells.Spell spell = Spells.byId(payload.spellId());
            if (spell == null) return;
            if (!ServerConfig.SPELLS_ENABLED.get()) {
                player.displayClientMessage(Component.literal("Spells are disabled on this server.").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            long now = System.currentTimeMillis();
            Map<String, Long> cooldowns = SPELL_COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
            long readyAt = cooldowns.getOrDefault(spell.id(), 0L);
            if (now < readyAt) {
                long secs = (readyAt - now + 999) / 1000;
                player.displayClientMessage(Component.literal("言霊 recharging: " + secs + "s").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            cooldowns.put(spell.id(), now + ServerConfig.SPELL_COOLDOWN_SECONDS.get() * 1000L);
            player.addEffect(new MobEffectInstance(spell.effect(), spell.seconds() * 20, spell.amplifier()));
            player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
            player.displayClientMessage(Component.literal("言霊! " + spell.english()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        });
    }

    private static void handleReward(Reward payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            long now = System.currentTimeMillis();
            UUID uuid = player.getUUID();
            if (payload.kind() == Reward.STUDY) {
                // A session takes at least a little while; ignore rapid repeats.
                if (now - LAST_STUDY_REWARD.getOrDefault(uuid, 0L) < 20_000L) return;
                LAST_STUDY_REWARD.put(uuid, now);
                int xp = Math.min(ServerConfig.MAX_XP_PER_SESSION.get(), Math.max(0, payload.amount()) * ServerConfig.XP_PER_CORRECT.get());
                if (xp > 0) player.giveExperiencePoints(xp);
            } else if (payload.kind() == Reward.QUEST) {
                if (now - LAST_QUEST_REWARD.getOrDefault(uuid, 0L) < 60_000L) return;
                LAST_QUEST_REWARD.put(uuid, now);
                player.giveExperiencePoints(ServerConfig.QUEST_XP.get());
                int emeralds = ServerConfig.QUEST_EMERALDS.get();
                if (emeralds > 0) {
                    ItemStack stack = new ItemStack(Items.EMERALD, emeralds);
                    if (!player.getInventory().add(stack)) player.drop(stack, false);
                }
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.0F);
            }
        });
    }
}
