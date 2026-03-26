package com.shestikpetr.groupmaster.bonus.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses JSON condition definitions into BonusCondition objects.
 *
 * Supported conditions:
 * - {"type":"always"}                                    — always true
 * - {"type":"never"}                                     — always false
 * - {"type":"is_day"}                                    — daytime (0-12000 ticks)
 * - {"type":"is_night"}                                  — nighttime
 * - {"type":"in_sunlight"}                               — daytime + sky visible + no block above
 * - {"type":"in_water"}                                  — player in water
 * - {"type":"in_rain"}                                   — raining at player position
 * - {"type":"on_fire"}                                   — player is on fire
 * - {"type":"sneaking"}                                  — player sneaking
 * - {"type":"health_below","value":0.5}                  — health below percentage
 * - {"type":"health_above","value":0.5}                  — health above percentage
 * - {"type":"in_dimension","dimension":"minecraft:the_nether"}
 * - {"type":"and","conditions":[...]}                    — all must match
 * - {"type":"or","conditions":[...]}                     — any must match
 * - {"type":"not","condition":{...}}                     — invert
 */
public class ConditionParser {

    private static final BonusCondition ALWAYS = player -> true;

    /**
     * Parse a JSON string into a BonusCondition.
     * Empty or "{}" returns ALWAYS.
     */
    public static BonusCondition parse(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) {
            return ALWAYS;
        }
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return parseObject(obj);
        } catch (Exception e) {
            return ALWAYS;
        }
    }

    /**
     * Validate a condition JSON string.
     * Returns null if valid, error message if not.
     */
    private static final int MAX_DEPTH = 10;

    public static String validate(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) {
            return null;
        }
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return validateObject(obj, 0);
        } catch (Exception e) {
            return "Invalid JSON: " + e.getMessage();
        }
    }

    public static String describe(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) {
            return "always";
        }
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return describeObject(obj);
        } catch (Exception e) {
            return "always";
        }
    }

    private static BonusCondition parseObject(JsonObject obj) {
        String type = obj.get("type").getAsString();
        return switch (type) {
            case "always" -> ALWAYS;
            case "never" -> player -> false;
            case "is_day" -> player -> {
                ServerLevel level = player.serverLevel();
                long time = level.getDayTime() % 24000;
                return time >= 0 && time < 12000;
            };
            case "is_night" -> player -> {
                ServerLevel level = player.serverLevel();
                long time = level.getDayTime() % 24000;
                return time >= 12000;
            };
            case "in_sunlight" -> player -> {
                ServerLevel level = player.serverLevel();
                long time = level.getDayTime() % 24000;
                if (time >= 12000) return false;
                BlockPos pos = player.blockPosition();
                return level.canSeeSky(pos);
            };
            case "in_water" -> ServerPlayer::isInWater;
            case "in_rain" -> player -> {
                ServerLevel level = player.serverLevel();
                BlockPos pos = player.blockPosition();
                return level.isRainingAt(pos);
            };
            case "on_fire" -> ServerPlayer::isOnFire;
            case "sneaking" -> ServerPlayer::isShiftKeyDown;
            case "health_below" -> {
                double threshold = obj.get("value").getAsDouble();
                yield player -> (player.getHealth() / player.getMaxHealth()) < threshold;
            }
            case "health_above" -> {
                double threshold = obj.get("value").getAsDouble();
                yield player -> (player.getHealth() / player.getMaxHealth()) > threshold;
            }
            case "in_dimension" -> {
                String dimension = obj.get("dimension").getAsString();
                ResourceLocation dimLoc = ResourceLocation.parse(dimension);
                yield player -> player.serverLevel().dimension().location().equals(dimLoc);
            }
            case "and" -> {
                List<BonusCondition> conditions = parseArray(obj.get("conditions").getAsJsonArray());
                yield player -> conditions.stream().allMatch(c -> c.test(player));
            }
            case "or" -> {
                List<BonusCondition> conditions = parseArray(obj.get("conditions").getAsJsonArray());
                yield player -> conditions.stream().anyMatch(c -> c.test(player));
            }
            case "not" -> {
                BonusCondition inner = parseObject(obj.get("condition").getAsJsonObject());
                yield player -> !inner.test(player);
            }
            default -> ALWAYS;
        };
    }

    private static List<BonusCondition> parseArray(JsonArray arr) {
        List<BonusCondition> list = new ArrayList<>();
        arr.forEach(el -> list.add(parseObject(el.getAsJsonObject())));
        return list;
    }

    private static final java.util.Set<String> KNOWN_TYPES = java.util.Set.of(
            "always", "never", "is_day", "is_night", "in_sunlight", "in_water",
            "in_rain", "on_fire", "sneaking", "health_below", "health_above",
            "in_dimension", "and", "or", "not"
    );

    private static String validateObject(JsonObject obj, int depth) {
        if (depth > MAX_DEPTH) return "Condition nesting too deep (max " + MAX_DEPTH + ")";
        if (!obj.has("type")) return "Missing 'type' field";
        String type = obj.get("type").getAsString();
        if (!KNOWN_TYPES.contains(type)) return "Unknown condition type: " + type;

        return switch (type) {
            case "health_below", "health_above" -> {
                if (!obj.has("value")) yield "Missing 'value' for " + type;
                double value = obj.get("value").getAsDouble();
                if (value < 0.0 || value > 1.0) yield "Value for " + type + " must be between 0.0 and 1.0";
                yield null;
            }
            case "in_dimension" -> {
                if (!obj.has("dimension")) yield "Missing 'dimension' field";
                String dim = obj.get("dimension").getAsString();
                if (!dim.matches("^[a-z0-9_.\\-]+:[a-z0-9_.\\-/]+$"))
                    yield "Invalid dimension format: " + dim + " (expected e.g. minecraft:overworld)";
                yield null;
            }
            case "and", "or" -> {
                if (!obj.has("conditions") || !obj.get("conditions").isJsonArray())
                    yield "Missing 'conditions' array for " + type;
                for (var el : obj.get("conditions").getAsJsonArray()) {
                    String err = validateObject(el.getAsJsonObject(), depth + 1);
                    if (err != null) yield err;
                }
                yield null;
            }
            case "not" -> {
                if (!obj.has("condition") || !obj.get("condition").isJsonObject())
                    yield "Missing 'condition' object for 'not'";
                yield validateObject(obj.get("condition").getAsJsonObject(), depth + 1);
            }
            default -> null;
        };
    }

    private static String describeObject(JsonObject obj) {
        String type = obj.get("type").getAsString();
        return switch (type) {
            case "always" -> "always";
            case "never" -> "never";
            case "is_day" -> "is_day";
            case "is_night" -> "is_night";
            case "in_sunlight" -> "in_sunlight";
            case "in_water" -> "in_water";
            case "in_rain" -> "in_rain";
            case "on_fire" -> "on_fire";
            case "sneaking" -> "sneaking";
            case "health_below" -> "health<" + obj.get("value").getAsDouble() * 100 + "%";
            case "health_above" -> "health>" + obj.get("value").getAsDouble() * 100 + "%";
            case "in_dimension" -> "dim:" + obj.get("dimension").getAsString();
            case "and" -> {
                List<String> parts = new ArrayList<>();
                obj.get("conditions").getAsJsonArray().forEach(el -> parts.add(describeObject(el.getAsJsonObject())));
                yield "(" + String.join(" AND ", parts) + ")";
            }
            case "or" -> {
                List<String> parts = new ArrayList<>();
                obj.get("conditions").getAsJsonArray().forEach(el -> parts.add(describeObject(el.getAsJsonObject())));
                yield "(" + String.join(" OR ", parts) + ")";
            }
            case "not" -> "NOT " + describeObject(obj.get("condition").getAsJsonObject());
            default -> type;
        };
    }
}
