package com.shestikpetr.groupmaster.model;

import java.util.UUID;

public class PlayerGroupData {

    private final UUID playerUuid;
    private String playerName;
    private String groupId;
    private long assignedAt;
    private String assignedBy;

    public PlayerGroupData(UUID playerUuid, String playerName, String groupId, long assignedAt, String assignedBy) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.groupId = groupId;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public long getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(long assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }
}
