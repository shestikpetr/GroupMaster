package com.shestikpetr.groupmaster.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.shestikpetr.groupmaster.model.Bonus;
import com.shestikpetr.groupmaster.model.Group;
import com.shestikpetr.groupmaster.storage.BonusRepository;
import com.shestikpetr.groupmaster.storage.GroupRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public class ConfigExporter {

    private final GroupRepository groupRepo;
    private final BonusRepository bonusRepo;
    private final Path configDir;

    public ConfigExporter(GroupRepository groupRepo, BonusRepository bonusRepo, Path configDir) {
        this.groupRepo = groupRepo;
        this.bonusRepo = bonusRepo;
        this.configDir = configDir;
    }

    public ExportResult export() throws IOException, SQLException {
        List<Group> allGroups = groupRepo.findAll();
        List<Group> sorted = sortTopologically(allGroups);
        List<Bonus> allBonuses = bonusRepo.findAll();

        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("exportedAt", Instant.now().toString());

        JsonArray groupsArr = new JsonArray();
        for (Group g : sorted) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", g.getId());
            obj.addProperty("displayName", g.getDisplayName());
            obj.addProperty("parentId", g.getParentId());
            obj.addProperty("priority", g.getPriority());
            groupsArr.add(obj);
        }
        root.add("groups", groupsArr);

        JsonArray bonusesArr = new JsonArray();
        for (Bonus b : allBonuses) {
            JsonObject obj = new JsonObject();
            obj.addProperty("groupId", b.getGroupId());
            obj.addProperty("trigger", b.getTrigger());
            obj.addProperty("tickInterval", b.getTickInterval());
            obj.addProperty("condition", b.getCondition());
            obj.addProperty("actionType", b.getActionType());
            obj.addProperty("actionValue", b.getActionValue());
            obj.addProperty("mergeKey", b.getMergeKey());
            obj.addProperty("override", b.isOverride());
            obj.addProperty("target", b.getTarget());
            obj.addProperty("maxStacks", b.getMaxStacks());
            obj.addProperty("stackMode", b.getStackMode());
            obj.addProperty("resetOn", b.getResetOn());
            bonusesArr.add(obj);
        }
        root.add("bonuses", bonusesArr);

        Files.createDirectories(configDir);
        Path file = configDir.resolve("config.json");
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(root);
        Files.writeString(file, json);

        return new ExportResult(sorted.size(), allBonuses.size(), file);
    }

    private List<Group> sortTopologically(List<Group> groups) {
        Map<String, Group> byId = new LinkedHashMap<>();
        for (Group g : groups) {
            byId.put(g.getId(), g);
        }

        List<Group> result = new ArrayList<>();
        Set<String> emitted = new HashSet<>();

        while (result.size() < groups.size()) {
            boolean progress = false;
            for (Group g : byId.values()) {
                if (emitted.contains(g.getId())) continue;
                if (g.getParentId() == null || emitted.contains(g.getParentId())) {
                    result.add(g);
                    emitted.add(g.getId());
                    progress = true;
                }
            }
            if (!progress) {
                // Remaining groups have broken parent refs — add them anyway
                for (Group g : byId.values()) {
                    if (!emitted.contains(g.getId())) {
                        result.add(g);
                        emitted.add(g.getId());
                    }
                }
                break;
            }
        }

        return result;
    }

    public record ExportResult(int groups, int bonuses, Path file) {}
}
