package com.shestikpetr.groupmaster.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shestikpetr.groupmaster.bonus.condition.ConditionParser;
import com.shestikpetr.groupmaster.model.Bonus;
import com.shestikpetr.groupmaster.model.Group;
import com.shestikpetr.groupmaster.model.PlayerGroupData;

import java.util.Collection;
import java.util.List;

public class JsonHelper {

    private static final Gson GSON = new Gson();

    public static JsonObject groupToJson(Group group) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", group.getId());
        obj.addProperty("displayName", group.getDisplayName());
        obj.addProperty("parentId", group.getParentId());
        obj.addProperty("priority", group.getPriority());
        obj.addProperty("isRoot", group.isRoot());
        return obj;
    }

    public static JsonArray groupsToJson(Collection<Group> groups) {
        JsonArray arr = new JsonArray();
        for (Group group : groups) {
            arr.add(groupToJson(group));
        }
        return arr;
    }

    public static JsonObject playerToJson(PlayerGroupData player) {
        JsonObject obj = new JsonObject();
        obj.addProperty("playerUuid", player.getPlayerUuid().toString());
        obj.addProperty("playerName", player.getPlayerName());
        obj.addProperty("groupId", player.getGroupId());
        obj.addProperty("assignedAt", player.getAssignedAt());
        obj.addProperty("assignedBy", player.getAssignedBy());
        return obj;
    }

    public static JsonArray playersToJson(List<PlayerGroupData> players) {
        JsonArray arr = new JsonArray();
        for (PlayerGroupData player : players) {
            arr.add(playerToJson(player));
        }
        return arr;
    }

    public static JsonObject groupTreeToJson(Group group, List<Group> children) {
        JsonObject obj = groupToJson(group);
        JsonArray childArr = new JsonArray();
        for (Group child : children) {
            childArr.add(groupToJson(child));
        }
        obj.add("children", childArr);
        return obj;
    }

    public static JsonObject error(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("error", message);
        return obj;
    }

    public static JsonObject success(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("message", message);
        return obj;
    }

    public static JsonObject bonusToJson(Bonus bonus) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", bonus.getId());
        obj.addProperty("groupId", bonus.getGroupId());
        obj.addProperty("trigger", bonus.getTrigger());
        obj.addProperty("tickInterval", bonus.getTickInterval());
        obj.addProperty("actionType", bonus.getActionType());
        obj.addProperty("actionValue", bonus.getActionValue());
        obj.addProperty("mergeKey", bonus.getMergeKey());
        obj.addProperty("override", bonus.isOverride());
        obj.addProperty("target", bonus.getTarget());
        obj.addProperty("maxStacks", bonus.getMaxStacks());
        obj.addProperty("stackMode", bonus.getStackMode());
        obj.addProperty("resetOn", bonus.getResetOn());
        obj.addProperty("conditionDesc", ConditionParser.describe(bonus.getCondition()));
        // Parse condition as JSON object
        try {
            obj.add("condition", JsonParser.parseString(bonus.getCondition()));
        } catch (Exception e) {
            obj.addProperty("condition", bonus.getCondition());
        }
        return obj;
    }

    public static JsonArray bonusesToJson(List<Bonus> bonuses) {
        JsonArray arr = new JsonArray();
        for (Bonus bonus : bonuses) {
            arr.add(bonusToJson(bonus));
        }
        return arr;
    }

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }
}
