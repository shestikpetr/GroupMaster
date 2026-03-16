package com.shestikpetr.groupmaster.bonus.action;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Optional;

/**
 * Modifies a player attribute.
 * Value: {"attribute":"minecraft:generic.max_health","amount":4.0,"operation":"add_value"}
 * Operations: add_value, add_multiplied_base, add_multiplied_total
 */
public class AttributeAction implements ActionType {

    private static final String MODIFIER_PREFIX = "groupmaster:";

    @Override public String getId() { return "attribute"; }
    @Override public String getDisplayName() { return "Attribute Modifier"; }

    @Override
    public void apply(ServerPlayer player, String value) {
        JsonObject json = JsonParser.parseString(value).getAsJsonObject();
        String attrId = json.get("attribute").getAsString();
        double amount = json.get("amount").getAsDouble();
        String opStr = json.has("operation") ? json.get("operation").getAsString() : "add_value";

        getAttribute(attrId).ifPresent(attr -> {
            AttributeInstance instance = player.getAttribute(attr);
            if (instance != null) {
                ResourceLocation modId = ResourceLocation.parse(MODIFIER_PREFIX + attrId.replace(":", "_"));
                instance.removeModifier(modId);
                instance.addPermanentModifier(new AttributeModifier(modId, amount, parseOp(opStr)));
            }
        });
    }

    @Override
    public void remove(ServerPlayer player, String value) {
        JsonObject json = JsonParser.parseString(value).getAsJsonObject();
        String attrId = json.get("attribute").getAsString();

        getAttribute(attrId).ifPresent(attr -> {
            AttributeInstance instance = player.getAttribute(attr);
            if (instance != null) {
                ResourceLocation modId = ResourceLocation.parse(MODIFIER_PREFIX + attrId.replace(":", "_"));
                instance.removeModifier(modId);
            }
        });
    }

    @Override
    public String validate(String value) {
        try {
            JsonObject json = JsonParser.parseString(value).getAsJsonObject();
            if (!json.has("attribute")) return "Missing 'attribute' field";
            if (!json.has("amount")) return "Missing 'amount' field";
            if (getAttribute(json.get("attribute").getAsString()).isEmpty())
                return "Unknown attribute: " + json.get("attribute").getAsString();
            return null;
        } catch (Exception e) {
            return "Invalid JSON: " + e.getMessage();
        }
    }

    @Override
    public String extractMergeKey(String value) {
        return JsonParser.parseString(value).getAsJsonObject().get("attribute").getAsString();
    }

    @Override
    public String describe(String value) {
        JsonObject json = JsonParser.parseString(value).getAsJsonObject();
        String attr = json.get("attribute").getAsString();
        double amount = json.get("amount").getAsDouble();
        String op = json.has("operation") ? json.get("operation").getAsString() : "add_value";
        return attr + " " + (amount >= 0 ? "+" : "") + amount + " (" + op + ")";
    }

    private static Optional<Holder.Reference<Attribute>> getAttribute(String id) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(id));
    }

    private static AttributeModifier.Operation parseOp(String op) {
        return switch (op.toLowerCase()) {
            case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }
}
