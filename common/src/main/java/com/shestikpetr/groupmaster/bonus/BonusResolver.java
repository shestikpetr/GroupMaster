package com.shestikpetr.groupmaster.bonus;

import com.shestikpetr.groupmaster.Constants;
import com.shestikpetr.groupmaster.model.Bonus;
import com.shestikpetr.groupmaster.model.Group;
import com.shestikpetr.groupmaster.storage.BonusRepository;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

/**
 * Resolves effective bonuses for a group by walking the hierarchy chain
 * from root to the target group, applying override logic.
 *
 * Override key: "actionType:mergeKey" — a child bonus with override=true
 * replaces the parent's bonus with the same composite key.
 */
public class BonusResolver {

    private final BonusRepository bonusRepo;
    private final Function<String, List<Group>> hierarchyChainProvider;

    public BonusResolver(BonusRepository bonusRepo, Function<String, List<Group>> hierarchyChainProvider) {
        this.bonusRepo = bonusRepo;
        this.hierarchyChainProvider = hierarchyChainProvider;
    }

    /**
     * Resolve all effective bonuses for a group (inherited + direct).
     * Walks hierarchy root→target; for each composite key, last override wins.
     */
    public List<Bonus> resolve(String groupId) {
        List<Group> chain = hierarchyChainProvider.apply(groupId);
        Map<String, Bonus> resolved = new LinkedHashMap<>();

        for (Group group : chain) {
            try {
                List<Bonus> groupBonuses = bonusRepo.findByGroup(group.getId());
                for (Bonus bonus : groupBonuses) {
                    String key = bonus.getCompositeKey();
                    if (bonus.isOverride() || !resolved.containsKey(key)) {
                        resolved.put(key, bonus);
                    }
                }
            } catch (SQLException e) {
                Constants.LOG.error("Failed to load bonuses for group {}", group.getId(), e);
            }
        }

        return new ArrayList<>(resolved.values());
    }

    /**
     * Resolve only on_join bonuses.
     */
    public List<Bonus> resolveByTrigger(String groupId, String trigger) {
        return resolve(groupId).stream()
                .filter(b -> trigger.equals(b.getTrigger()))
                .toList();
    }

    /**
     * Get bonuses directly assigned to a group (not inherited).
     */
    public List<Bonus> getDirectBonuses(String groupId) {
        try {
            return bonusRepo.findByGroup(groupId);
        } catch (SQLException e) {
            Constants.LOG.error("Failed to load bonuses for group {}", groupId, e);
            return List.of();
        }
    }
}
