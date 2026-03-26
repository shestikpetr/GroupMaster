package com.shestikpetr.groupmaster.storage;

import com.shestikpetr.groupmaster.model.Group;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GroupRepository {

    private final DatabaseManager db;

    public GroupRepository(DatabaseManager db) {
        this.db = db;
    }

    public void create(Group group) throws SQLException {
        String sql = "INSERT INTO groups (id, display_name, parent_id, priority) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, group.getId());
            stmt.setString(2, group.getDisplayName());
            stmt.setString(3, group.getParentId());
            stmt.setInt(4, group.getPriority());
            stmt.executeUpdate();
        }
    }

    public Optional<Group> findById(String id) throws SQLException {
        String sql = "SELECT id, display_name, parent_id, priority FROM groups WHERE id = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Group> findAll() throws SQLException {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT id, display_name, parent_id, priority FROM groups ORDER BY priority DESC";
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                groups.add(mapRow(rs));
            }
        }
        return groups;
    }

    public List<Group> findChildren(String parentId) throws SQLException {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT id, display_name, parent_id, priority FROM groups WHERE parent_id = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, parentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                groups.add(mapRow(rs));
            }
        }
        return groups;
    }

    public List<Group> findRoots() throws SQLException {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT id, display_name, parent_id, priority FROM groups WHERE parent_id IS NULL ORDER BY priority DESC";
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                groups.add(mapRow(rs));
            }
        }
        return groups;
    }

    public void update(Group group) throws SQLException {
        String sql = "UPDATE groups SET display_name = ?, parent_id = ?, priority = ? WHERE id = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, group.getDisplayName());
            stmt.setString(2, group.getParentId());
            stmt.setInt(3, group.getPriority());
            stmt.setString(4, group.getId());
            stmt.executeUpdate();
        }
    }

    public boolean delete(String id) throws SQLException {
        String sql = "DELETE FROM groups WHERE id = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public void deleteAll() throws SQLException {
        try (Statement stmt = db.getConnection().createStatement()) {
            stmt.executeUpdate("DELETE FROM groups");
        }
    }

    public boolean exists(String id) throws SQLException {
        String sql = "SELECT 1 FROM groups WHERE id = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeQuery().next();
        }
    }

    private Group mapRow(ResultSet rs) throws SQLException {
        return new Group(
            rs.getString("id"),
            rs.getString("display_name"),
            rs.getString("parent_id"),
            rs.getInt("priority")
        );
    }
}
