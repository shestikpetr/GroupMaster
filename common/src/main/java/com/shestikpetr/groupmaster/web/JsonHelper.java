package com.shestikpetr.groupmaster.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }
}
