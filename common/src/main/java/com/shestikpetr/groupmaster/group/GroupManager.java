package com.shestikpetr.groupmaster.group;

import com.shestikpetr.groupmaster.Constants;
import com.shestikpetr.groupmaster.model.Group;
import com.shestikpetr.groupmaster.model.PlayerGroupData;
import com.shestikpetr.groupmaster.storage.GroupRepository;
import com.shestikpetr.groupmaster.storage.PlayerRepository;

import java.sql.SQLException;
import java.util.*;

public class GroupManager {

    private final GroupRepository groupRepo;
    private final PlayerRepository playerRepo;

    // In-memory cache for fast lookups
    private final Map<String, Group> groupCache = new LinkedHashMap<>();

    public GroupManager(GroupRepository groupRepo, PlayerRepository playerRepo) {
        this.groupRepo = groupRepo;
        this.playerRepo = playerRepo;
    }

    public void loadAll() {
        try {
            groupCache.clear();
            for (Group group : groupRepo.findAll()) {
                groupCache.put(group.getId(), group);
            }
            Constants.LOG.info("Loaded {} groups", groupCache.size());
        } catch (SQLException e) {
            Constants.LOG.error("Failed to load groups", e);
        }
    }

    // --- Group CRUD ---

    public boolean createGroup(String id, String displayName, String parentId, int priority) {
        if (groupCache.containsKey(id)) {
            return false;
        }
        if (parentId != null && !groupCache.containsKey(parentId)) {
            return false;
        }

        Group group = new Group(id, displayName, parentId, priority);
        try {
            groupRepo.create(group);
            groupCache.put(id, group);
            return true;
        } catch (SQLException e) {
            Constants.LOG.error("Failed to create group {}", id, e);
            return false;
        }
    }

    public boolean deleteGroup(String id) {
        if (!groupCache.containsKey(id)) {
            return false;
        }

        try {
            // Re-parent children to deleted group's parent
            Group group = groupCache.get(id);
            List<Group> children = getChildren(id);
            for (Group child : children) {
                child.setParentId(group.getParentId());
                groupRepo.update(child);
            }

            groupRepo.delete(id);
            groupCache.remove(id);
            return true;
        } catch (SQLException e) {
            Constants.LOG.error("Failed to delete group {}", id, e);
            return false;
        }
    }

    public boolean updateGroup(String id, String displayName, String parentId, int priority) {
        Group group = groupCache.get(id);
        if (group == null) {
            return false;
        }
        if (parentId != null && !groupCache.containsKey(parentId)) {
            return false;
        }
        if (parentId != null && wouldCreateCycle(id, parentId)) {
            return false;
        }

        group.setDisplayName(displayName);
        group.setParentId(parentId);
        group.setPriority(priority);

        try {
            groupRepo.update(group);
            return true;
        } catch (SQLException e) {
            Constants.LOG.error("Failed to update group {}", id, e);
            return false;
        }
    }

    public Optional<Group> getGroup(String id) {
        return Optional.ofNullable(groupCache.get(id));
    }

    public Collection<Group> getAllGroups() {
        return Collections.unmodifiableCollection(groupCache.values());
    }

    public List<Group> getRootGroups() {
        return groupCache.values().stream()
                .filter(Group::isRoot)
                .toList();
    }

    public List<Group> getChildren(String parentId) {
        return groupCache.values().stream()
                .filter(g -> parentId.equals(g.getParentId()))
                .toList();
    }

    /**
     * Returns the hierarchy chain from root to the given group.
     * Example: for "king" with parent "human" -> returns [human, king]
     */
    public List<Group> getHierarchyChain(String groupId) {
        List<Group> chain = new ArrayList<>();
        String current = groupId;
        Set<String> visited = new HashSet<>();

        while (current != null && !visited.contains(current)) {
            visited.add(current);
            Group group = groupCache.get(current);
            if (group == null) break;
            chain.add(group);
            current = group.getParentId();
        }

        Collections.reverse(chain);
        return chain;
    }

    private boolean wouldCreateCycle(String groupId, String newParentId) {
        String current = newParentId;
        Set<String> visited = new HashSet<>();
        while (current != null && !visited.contains(current)) {
            if (current.equals(groupId)) {
                return true;
            }
            visited.add(current);
            Group g = groupCache.get(current);
            current = g != null ? g.getParentId() : null;
        }
        return false;
    }

    // --- Player assignment ---

    public boolean assignPlayer(UUID playerUuid, String playerName, String groupId, String assignedBy) {
        if (!groupCache.containsKey(groupId)) {
            return false;
        }

        PlayerGroupData data = new PlayerGroupData(
                playerUuid, playerName, groupId,
                System.currentTimeMillis(), assignedBy
        );

        try {
            playerRepo.assign(data);
            return true;
        } catch (SQLException e) {
            Constants.LOG.error("Failed to assign player {} to group {}", playerName, groupId, e);
            return false;
        }
    }

    public boolean removePlayer(UUID playerUuid) {
        try {
            return playerRepo.remove(playerUuid);
        } catch (SQLException e) {
            Constants.LOG.error("Failed to remove player {}", playerUuid, e);
            return false;
        }
    }

    public Optional<PlayerGroupData> getPlayerGroup(UUID playerUuid) {
        try {
            return playerRepo.findByUuid(playerUuid);
        } catch (SQLException e) {
            Constants.LOG.error("Failed to get player group for {}", playerUuid, e);
            return Optional.empty();
        }
    }

    public List<PlayerGroupData> getPlayersInGroup(String groupId) {
        try {
            return playerRepo.findByGroup(groupId);
        } catch (SQLException e) {
            Constants.LOG.error("Failed to get players in group {}", groupId, e);
            return List.of();
        }
    }

    public List<PlayerGroupData> getAllPlayers() {
        try {
            return playerRepo.findAll();
        } catch (SQLException e) {
            Constants.LOG.error("Failed to get all players", e);
            return List.of();
        }
    }
}
