package com.shestikpetr.groupmaster.model;

public class Bonus {

    private int id;
    private final String groupId;
    private final String trigger;       // on_join, on_leave, tick, on_attack, on_damaged, on_kill, on_death, on_eat
    private final int tickInterval;     // for tick trigger, in ticks (20 = 1 sec)
    private final String condition;     // JSON condition object
    private final String actionType;    // effect, attribute, burn, command, message, set_group
    private final String actionValue;   // JSON action params
    private final String mergeKey;      // for override resolution
    private final boolean override;
    private final String target;        // "self" or "victim" — who receives the action
    private final int maxStacks;        // 0 = no limit, >0 = max stacks
    private final String stackMode;     // "each" = apply per stack, "threshold" = apply at max
    private final String resetOn;       // "never", "on_death", "on_leave"

    public Bonus(int id, String groupId, String trigger, int tickInterval,
                 String condition, String actionType, String actionValue,
                 String mergeKey, boolean override, String target,
                 int maxStacks, String stackMode, String resetOn) {
        this.id = id;
        this.groupId = groupId;
        this.trigger = trigger;
        this.tickInterval = tickInterval;
        this.condition = condition;
        this.actionType = actionType;
        this.actionValue = actionValue;
        this.mergeKey = mergeKey;
        this.override = override;
        this.target = target != null ? target : "self";
        this.maxStacks = maxStacks;
        this.stackMode = stackMode != null ? stackMode : "each";
        this.resetOn = resetOn != null ? resetOn : "never";
    }

    public Bonus(String groupId, String trigger, int tickInterval,
                 String condition, String actionType, String actionValue,
                 String mergeKey, boolean override, String target,
                 int maxStacks, String stackMode, String resetOn) {
        this(0, groupId, trigger, tickInterval, condition, actionType, actionValue,
                mergeKey, override, target, maxStacks, stackMode, resetOn);
    }

    public Bonus(String groupId, String trigger, int tickInterval,
                 String condition, String actionType, String actionValue,
                 String mergeKey, boolean override, String target) {
        this(0, groupId, trigger, tickInterval, condition, actionType, actionValue,
                mergeKey, override, target, 0, "each", "never");
    }

    public Bonus(String groupId, String trigger, int tickInterval,
                 String condition, String actionType, String actionValue,
                 String mergeKey, boolean override) {
        this(0, groupId, trigger, tickInterval, condition, actionType, actionValue,
                mergeKey, override, "self", 0, "each", "never");
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public String getTrigger() { return trigger; }
    public int getTickInterval() { return tickInterval; }
    public String getCondition() { return condition; }
    public String getActionType() { return actionType; }
    public String getActionValue() { return actionValue; }
    public String getMergeKey() { return mergeKey; }
    public boolean isOverride() { return override; }
    public String getTarget() { return target; }
    public boolean isTargetSelf() { return "self".equals(target); }
    public boolean isTargetVictim() { return "victim".equals(target); }
    public int getMaxStacks() { return maxStacks; }
    public String getStackMode() { return stackMode; }
    public String getResetOn() { return resetOn; }
    public boolean hasStacking() { return maxStacks > 0; }

    public boolean isTickBased() { return "tick".equals(trigger); }
    public boolean isOnJoin() { return "on_join".equals(trigger); }
    public boolean isOnLeave() { return "on_leave".equals(trigger); }
    public boolean isEventTrigger() {
        return trigger.equals("on_attack") || trigger.equals("on_damaged") ||
               trigger.equals("on_kill") || trigger.equals("on_death") || trigger.equals("on_eat");
    }

    /**
     * Composite key for merge/override resolution: "actionType:mergeKey"
     */
    public String getCompositeKey() {
        return actionType + ":" + mergeKey;
    }

    @Override
    public String toString() {
        return "Bonus{id=" + id + ", group=" + groupId + ", trigger=" + trigger +
               ", action=" + actionType + ", key=" + mergeKey + ", override=" + override +
               ", target=" + target + ", stacks=" + maxStacks + "/" + stackMode + "}";
    }
}
