package com.shestikpetr.groupmaster.bonus.action;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Optional;

/**
 * Applies a potion effect.
 * Value: {"effect":"minecraft:speed","amplifier":1,"duration":10}
 * duration in seconds, -1 = infinite
 */
public class EffectAction implements ActionType {

    @Override public String getId() { return "effect"; }
    @Override public String getDisplayName() { return "Potion Effect"; }

    @Override
    public void apply(ServerPlayer player, String value) {
        JsonObject json = JsonParser.parseString(value).getAsJsonObject();
        String effectId = json.get("effect").getAsString();
        int amplifier = json.has("amplifier") ? json.get("amplifier").getAsInt() : 0;
        int duration = json.has("duration") ? json.get("duration").getAsInt() : -1;

        getEffect(effectId).ifPresent(effect -> {
            int ticks = duration == -1 ? Integer.MAX_VALUE : duration * 20;
            player.addEffect(new MobEffectInstance(effect, ticks, amplifier, false, false, true));
        });
    }

    @Override
    public void remove(ServerPlayer player, String value) {
        JsonObject json = JsonParser.parseString(value).getAsJsonObject();
        String effectId = json.get("effect").getAsString();
        getEffect(effectId).ifPresent(player::removeEffect);
    }

    @Override
    public String validate(String value) {
        try {
            JsonObject json = JsonParser.parseString(value).getAsJsonObject();
            if (!json.has("effect")) return "Missing 'effect' field";
            if (getEffect(json.get("effect").getAsString()).isEmpty())
                return "Unknown effect: " + json.get("effect").getAsString();
            return null;
        } catch (Exception e) {
            return "Invalid JSON: " + e.getMessage();
        }
    }

    @Override
    public String extractMergeKey(String value) {
        return JsonParser.parseString(value).getAsJsonObject().get("effect").getAsString();
    }

    @Override
    public String describe(String value) {
        JsonObject json = JsonParser.parseString(value).getAsJsonObject();
        String effect = json.get("effect").getAsString();
        int amp = json.has("amplifier") ? json.get("amplifier").getAsInt() : 0;
        int dur = json.has("duration") ? json.get("duration").getAsInt() : -1;
        return effect + " lvl " + (amp + 1) + (dur == -1 ? " (infinite)" : " (" + dur + "s)");
    }

    private static Optional<Holder.Reference<MobEffect>> getEffect(String id) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id));
    }
}
