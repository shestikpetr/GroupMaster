package com.shestikpetr.groupmaster.model;

import java.util.Objects;

public class Group {

    private final String id;
    private String displayName;
    private String parentId;
    private int priority;

    public Group(String id, String displayName, String parentId, int priority) {
        this.id = Objects.requireNonNull(id);
        this.displayName = Objects.requireNonNull(displayName);
        this.parentId = parentId;
        this.priority = priority;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isRoot() {
        return parentId == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group group)) return false;
        return id.equals(group.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Group{id='" + id + "', displayName='" + displayName + "', parentId='" + parentId + "', priority=" + priority + "}";
    }
}
