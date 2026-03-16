package com.shestikpetr.groupmaster.model;

public class Bonus {

    private int id;
    private final String groupId;
    private final String trigger;       // on_join, on_leave, tick
    private final int tickInterval;     // for tick trigger, in ticks (20 = 1 sec)
    private final String condition;     // JSON condition object
    private final String actionType;    // effect, attribute, burn, command, message
    private final String actionValue;   // JSON action params
    private final String mergeKey;      // for override resolution
    private final boolean override;

    public Bonus(int id, String groupId, String trigger, int tickInterval,
                 String condition, String actionType, String actionValue,
                 String mergeKey, boolean override) {
        this.id = id;
        this.groupId = groupId;
        this.trigger = trigger;
        this.tickInterval = tickInterval;
        this.condition = condition;
        this.actionType = actionType;
        this.actionValue = actionValue;
        this.mergeKey = mergeKey;
        this.override = override;
    }

    public Bonus(String groupId, String trigger, int tickInterval,
                 String condition, String actionType, String actionValue,
                 String mergeKey, boolean override) {
        this(0, groupId, trigger, tickInterval, condition, actionType, actionValue, mergeKey, override);
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

    public boolean isTickBased() { return "tick".equals(trigger); }
    public boolean isOnJoin() { return "on_join".equals(trigger); }
    public boolean isOnLeave() { return "on_leave".equals(trigger); }

    /**
     * Composite key for merge/override resolution: "actionType:mergeKey"
     */
    public String getCompositeKey() {
        return actionType + ":" + mergeKey;
    }

    @Override
    public String toString() {
        return "Bonus{id=" + id + ", group=" + groupId + ", trigger=" + trigger +
               ", action=" + actionType + ", key=" + mergeKey + ", override=" + override + "}";
    }
}
