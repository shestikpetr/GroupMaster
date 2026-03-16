package com.shestikpetr.groupmaster.bonus;

import com.shestikpetr.groupmaster.bonus.action.*;

import java.util.*;

public class BonusRegistry {

    private final Map<String, ActionType> actions = new LinkedHashMap<>();

    public BonusRegistry() {
        registerAction(new EffectAction());
        registerAction(new AttributeAction());
        registerAction(new BurnAction());
        registerAction(new CommandAction());
        registerAction(new MessageAction());
    }

    public void registerAction(ActionType action) {
        actions.put(action.getId(), action);
    }

    public Optional<ActionType> getAction(String id) {
        return Optional.ofNullable(actions.get(id));
    }

    public Collection<ActionType> getAllActions() {
        return Collections.unmodifiableCollection(actions.values());
    }

    public Set<String> getActionIds() {
        return Collections.unmodifiableSet(actions.keySet());
    }
}
