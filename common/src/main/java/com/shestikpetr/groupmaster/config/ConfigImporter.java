package com.shestikpetr.groupmaster.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shestikpetr.groupmaster.Constants;
import com.shestikpetr.groupmaster.model.Bonus;
import com.shestikpetr.groupmaster.model.Group;
import com.shestikpetr.groupmaster.storage.BonusRepository;
import com.shestikpetr.groupmaster.storage.GroupRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class ConfigImporter {

    private final GroupRepository groupRepo;
    private final BonusRepository bonusRepo;
    private final Path configDir;

    public ConfigImporter(GroupRepository groupRepo, BonusRepository bonusRepo, Path configDir) {
        this.groupRepo = groupRepo;
        this.bonusRepo = bonusRepo;
        this.configDir = configDir;
    }

    public boolean configFileExists() {
        return Files.exists(configDir.resolve("config.json"));
    }

    public ImportResult importConfig(boolean force) throws IOException, SQLException {
        Path file = configDir.resolve("config.json");
        if (!Files.exists(file)) {
            return new ImportResult(0, 0, true, "Config file not found");
        }

        String content = Files.readString(file);
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();

        int version = root.has("version") ? root.get("version").getAsInt() : 1;
        if (version != 1) {
            return new ImportResult(0, 0, true, "Unsupported config version: " + version);
        }

        if (!force) {
            if (!groupRepo.findAll().isEmpty()) {
                return new ImportResult(0, 0, true, "Database already has groups, skipping auto-import");
            }
        }

        if (force) {
            bonusRepo.deleteAll();
            groupRepo.deleteAll();
        }

        JsonArray groups = root.has("groups") ? root.getAsJsonArray("groups") : new JsonArray();
        JsonArray bonuses = root.has("bonuses") ? root.getAsJsonArray("bonuses") : new JsonArray();

        int groupCount = 0;
        Set<String> imported = new HashSet<>();
        for (JsonElement el : groups) {
            JsonObject obj = el.getAsJsonObject();
            String id = obj.get("id").getAsString();
            String parentId = obj.has("parentId") && !obj.get("parentId").isJsonNull()
                    ? obj.get("parentId").getAsString() : null;

            if (parentId != null && !imported.contains(parentId)) {
                Constants.LOG.warn("Skipping group '{}': parent '{}' not found", id, parentId);
                continue;
            }

            Group group = new Group(
                    id,
                    obj.get("displayName").getAsString(),
                    parentId,
                    obj.has("priority") ? obj.get("priority").getAsInt() : 0
            );
            groupRepo.create(group);
            imported.add(id);
            groupCount++;
        }

        int bonusCount = 0;
        for (JsonElement el : bonuses) {
            JsonObject obj = el.getAsJsonObject();
            String groupId = obj.get("groupId").getAsString();

            if (!imported.contains(groupId)) {
                Constants.LOG.warn("Skipping bonus for group '{}': group not found", groupId);
                continue;
            }

            Bonus bonus = new Bonus(
                    groupId,
                    obj.get("trigger").getAsString(),
                    obj.has("tickInterval") ? obj.get("tickInterval").getAsInt() : 0,
                    obj.has("condition") ? obj.get("condition").getAsString() : "{}",
                    obj.get("actionType").getAsString(),
                    obj.get("actionValue").getAsString(),
                    obj.has("mergeKey") ? obj.get("mergeKey").getAsString() : "",
                    obj.has("override") && obj.get("override").getAsBoolean(),
                    obj.has("target") ? obj.get("target").getAsString() : "self",
                    obj.has("maxStacks") ? obj.get("maxStacks").getAsInt() : 0,
                    obj.has("stackMode") ? obj.get("stackMode").getAsString() : "each",
                    obj.has("resetOn") ? obj.get("resetOn").getAsString() : "never"
            );
            bonusRepo.create(bonus);
            bonusCount++;
        }

        return new ImportResult(groupCount, bonusCount, false,
                "Imported " + groupCount + " groups and " + bonusCount + " bonuses");
    }

    public record ImportResult(int groups, int bonuses, boolean skipped, String message) {}
}
