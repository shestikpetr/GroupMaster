// Visual Action Value Builder — replaces raw JSON input with friendly forms

const ACTION_DEFS = {
    effect: {
        label: 'Potion Effect',
        fields: [
            {
                key: 'effect', label: 'Effect', type: 'select', def: 'minecraft:speed',
                options: [
                    { group: 'Positive', items: [
                        { value: 'minecraft:speed', label: 'Speed' },
                        { value: 'minecraft:haste', label: 'Haste' },
                        { value: 'minecraft:strength', label: 'Strength' },
                        { value: 'minecraft:instant_health', label: 'Instant Health' },
                        { value: 'minecraft:jump_boost', label: 'Jump Boost' },
                        { value: 'minecraft:regeneration', label: 'Regeneration' },
                        { value: 'minecraft:resistance', label: 'Resistance' },
                        { value: 'minecraft:fire_resistance', label: 'Fire Resistance' },
                        { value: 'minecraft:water_breathing', label: 'Water Breathing' },
                        { value: 'minecraft:invisibility', label: 'Invisibility' },
                        { value: 'minecraft:night_vision', label: 'Night Vision' },
                        { value: 'minecraft:health_boost', label: 'Health Boost' },
                        { value: 'minecraft:absorption', label: 'Absorption' },
                        { value: 'minecraft:saturation', label: 'Saturation' },
                        { value: 'minecraft:luck', label: 'Luck' },
                        { value: 'minecraft:slow_falling', label: 'Slow Falling' },
                        { value: 'minecraft:conduit_power', label: 'Conduit Power' },
                        { value: 'minecraft:dolphins_grace', label: "Dolphin's Grace" },
                        { value: 'minecraft:hero_of_the_village', label: 'Hero of the Village' },
                    ]},
                    { group: 'Negative', items: [
                        { value: 'minecraft:slowness', label: 'Slowness' },
                        { value: 'minecraft:mining_fatigue', label: 'Mining Fatigue' },
                        { value: 'minecraft:instant_damage', label: 'Instant Damage' },
                        { value: 'minecraft:nausea', label: 'Nausea' },
                        { value: 'minecraft:blindness', label: 'Blindness' },
                        { value: 'minecraft:hunger', label: 'Hunger' },
                        { value: 'minecraft:weakness', label: 'Weakness' },
                        { value: 'minecraft:poison', label: 'Poison' },
                        { value: 'minecraft:wither', label: 'Wither' },
                        { value: 'minecraft:levitation', label: 'Levitation' },
                        { value: 'minecraft:unluck', label: 'Bad Luck' },
                        { value: 'minecraft:darkness', label: 'Darkness' },
                    ]},
                    { group: 'Neutral', items: [
                        { value: 'minecraft:glowing', label: 'Glowing' },
                        { value: 'minecraft:bad_omen', label: 'Bad Omen' },
                    ]}
                ],
                allowCustom: true
            },
            { key: 'amplifier', label: 'Level (0 = I, 1 = II, ...)', type: 'number', def: 0, min: 0, max: 255 },
            { key: 'duration', label: 'Duration (sec, -1 = infinite)', type: 'number', def: -1, min: -1, max: 1000000 }
        ]
    },
    attribute: {
        label: 'Attribute Modifier',
        fields: [
            {
                key: 'attribute', label: 'Attribute', type: 'select', def: 'minecraft:generic.max_health',
                options: [
                    { group: 'Generic', items: [
                        { value: 'minecraft:generic.max_health', label: 'Max Health' },
                        { value: 'minecraft:generic.armor', label: 'Armor' },
                        { value: 'minecraft:generic.armor_toughness', label: 'Armor Toughness' },
                        { value: 'minecraft:generic.attack_damage', label: 'Attack Damage' },
                        { value: 'minecraft:generic.attack_speed', label: 'Attack Speed' },
                        { value: 'minecraft:generic.movement_speed', label: 'Movement Speed' },
                        { value: 'minecraft:generic.knockback_resistance', label: 'Knockback Resistance' },
                        { value: 'minecraft:generic.luck', label: 'Luck' },
                        { value: 'minecraft:generic.max_absorption', label: 'Max Absorption' },
                    ]},
                    { group: 'Player', items: [
                        { value: 'minecraft:player.block_break_speed', label: 'Block Break Speed' },
                        { value: 'minecraft:player.block_interaction_range', label: 'Block Interaction Range' },
                        { value: 'minecraft:player.entity_interaction_range', label: 'Entity Interaction Range' },
                    ]}
                ],
                allowCustom: true
            },
            { key: 'amount', label: 'Amount', type: 'number', def: 4.0, step: 0.5 },
            {
                key: 'operation', label: 'Operation', type: 'select', def: 'add_value',
                options: [
                    { group: 'Operations', items: [
                        { value: 'add_value', label: 'Add Value (+N)' },
                        { value: 'add_multiplied_base', label: 'Add % of Base (×N base)' },
                        { value: 'add_multiplied_total', label: 'Add % of Total (×N total)' },
                    ]}
                ]
            }
        ]
    },
    burn: {
        label: 'Burn',
        fields: [
            { key: 'duration', label: 'Duration (seconds)', type: 'number', def: 3, min: 1, max: 1000 }
        ]
    },
    command: {
        label: 'Server Command',
        fields: [
            { key: 'command', label: 'Command', type: 'text', def: 'give {player} minecraft:diamond 1',
              hint: 'Use {player} for player name. No leading slash.' }
        ]
    },
    message: {
        label: 'Chat Message',
        fields: [
            { key: 'message', label: 'Message', type: 'text', def: 'Hello, {player}!',
              hint: 'Use {player} for player name.' }
        ]
    },
    set_group: {
        label: 'Set Group',
        fields: [
            { key: 'group', label: 'Target Group', type: 'group_select', def: '' }
        ]
    }
};

let actionBuilderMode = 'visual';

function initActionBuilder(actionType, actionValueJson) {
    actionBuilderMode = 'visual';
    const container = document.getElementById('bm-action-builder');
    const textarea = document.getElementById('bm-actionValue');
    const visualBtn = document.getElementById('bm-action-visual-btn');
    const jsonBtn = document.getElementById('bm-action-json-btn');

    container.style.display = '';
    textarea.style.display = 'none';
    if (visualBtn) visualBtn.className = 'btn btn-sm';
    if (jsonBtn) jsonBtn.className = 'btn btn-sm btn-secondary';

    renderActionBuilder(actionType, actionValueJson);
    textarea.value = JSON.stringify(actionValueJson || {}, null, 2);
}

function setActionMode(mode) {
    const container = document.getElementById('bm-action-builder');
    const textarea = document.getElementById('bm-actionValue');
    const visualBtn = document.getElementById('bm-action-visual-btn');
    const jsonBtn = document.getElementById('bm-action-json-btn');

    if (mode === 'visual') {
        try {
            const json = JSON.parse(textarea.value || '{}');
            const actionType = document.getElementById('bm-actionType').value;
            renderActionBuilder(actionType, json);
        } catch (e) {
            alert('Cannot parse action JSON: ' + e.message);
            return;
        }
        container.style.display = '';
        textarea.style.display = 'none';
        visualBtn.className = 'btn btn-sm';
        jsonBtn.className = 'btn btn-sm btn-secondary';
    } else {
        try {
            const json = getActionJsonFromBuilder();
            textarea.value = JSON.stringify(json, null, 2);
        } catch (e) { /* keep existing */ }
        container.style.display = 'none';
        textarea.style.display = '';
        visualBtn.className = 'btn btn-sm btn-secondary';
        jsonBtn.className = 'btn btn-sm';
    }
    actionBuilderMode = mode;
}

function renderActionBuilder(actionType, data) {
    const container = document.getElementById('bm-action-builder');
    container.innerHTML = '';
    const def = ACTION_DEFS[actionType];
    if (!def) {
        container.innerHTML = '<div style="color:#888;font-size:12px">Unknown action type — use JSON mode</div>';
        return;
    }

    data = data || {};

    def.fields.forEach(field => {
        const row = document.createElement('div');
        row.className = 'action-field-row';

        const label = document.createElement('label');
        label.className = 'action-field-label';
        label.textContent = field.label;
        row.appendChild(label);

        let input;

        if (field.type === 'select') {
            const wrapper = document.createElement('div');
            wrapper.className = 'action-select-wrapper';

            const select = document.createElement('select');
            select.className = 'action-field-input';
            select.dataset.key = field.key;

            let hasValue = false;
            if (field.options) {
                field.options.forEach(group => {
                    const optgroup = document.createElement('optgroup');
                    optgroup.label = group.group;
                    group.items.forEach(item => {
                        const opt = document.createElement('option');
                        opt.value = item.value;
                        opt.textContent = item.label;
                        optgroup.appendChild(opt);
                        if (data[field.key] === item.value) hasValue = true;
                    });
                    select.appendChild(optgroup);
                });
            }

            // If current value not in dropdown and allowCustom, add it
            if (field.allowCustom && data[field.key] && !hasValue) {
                const customGroup = document.createElement('optgroup');
                customGroup.label = 'Custom';
                const opt = document.createElement('option');
                opt.value = data[field.key];
                opt.textContent = data[field.key];
                customGroup.appendChild(opt);
                select.appendChild(customGroup);
            }

            select.value = data[field.key] !== undefined ? data[field.key] : field.def;
            wrapper.appendChild(select);

            // Custom input toggle for allowCustom
            if (field.allowCustom) {
                const customBtn = document.createElement('button');
                customBtn.className = 'btn btn-sm btn-secondary action-custom-btn';
                customBtn.textContent = 'Custom';
                customBtn.title = 'Enter custom ID';
                customBtn.onclick = () => {
                    const val = prompt('Enter custom ID (e.g. minecraft:my_effect):', select.value);
                    if (val && val.trim()) {
                        // Check if already in options
                        let found = false;
                        for (const opt of select.options) {
                            if (opt.value === val.trim()) { found = true; break; }
                        }
                        if (!found) {
                            let customGroup = select.querySelector('optgroup[label="Custom"]');
                            if (!customGroup) {
                                customGroup = document.createElement('optgroup');
                                customGroup.label = 'Custom';
                                select.appendChild(customGroup);
                            }
                            const opt = document.createElement('option');
                            opt.value = val.trim();
                            opt.textContent = val.trim();
                            customGroup.appendChild(opt);
                        }
                        select.value = val.trim();
                    }
                };
                wrapper.appendChild(customBtn);
            }

            row.appendChild(wrapper);
        } else if (field.type === 'group_select') {
            input = document.createElement('select');
            input.className = 'action-field-input';
            input.dataset.key = field.key;
            // Populate from allGroups (global from bonuses.js)
            if (typeof allGroups !== 'undefined') {
                allGroups.forEach(g => {
                    const opt = document.createElement('option');
                    opt.value = g.id;
                    opt.textContent = g.displayName + ' (' + g.id + ')';
                    input.appendChild(opt);
                });
            }
            input.value = data[field.key] || field.def || '';
            row.appendChild(input);
        } else if (field.type === 'number') {
            input = document.createElement('input');
            input.type = 'number';
            input.className = 'action-field-input';
            input.dataset.key = field.key;
            if (field.min !== undefined) input.min = field.min;
            if (field.max !== undefined) input.max = field.max;
            if (field.step !== undefined) input.step = field.step;
            input.value = data[field.key] !== undefined ? data[field.key] : field.def;
            row.appendChild(input);
        } else {
            // text
            input = document.createElement('input');
            input.type = 'text';
            input.className = 'action-field-input';
            input.dataset.key = field.key;
            input.value = data[field.key] !== undefined ? data[field.key] : field.def;
            row.appendChild(input);
        }

        if (field.hint) {
            const hint = document.createElement('div');
            hint.className = 'action-field-hint';
            hint.textContent = field.hint;
            row.appendChild(hint);
        }

        container.appendChild(row);
    });
}

function actionBuilderToJson() {
    const container = document.getElementById('bm-action-builder');
    const actionType = document.getElementById('bm-actionType').value;
    const def = ACTION_DEFS[actionType];
    if (!def) return {};

    const result = {};
    def.fields.forEach(field => {
        let el;
        if (field.type === 'select' && field.allowCustom) {
            el = container.querySelector(`.action-select-wrapper select[data-key="${field.key}"]`);
        } else {
            el = container.querySelector(`[data-key="${field.key}"]`);
        }
        if (!el) return;

        if (field.type === 'number') {
            result[field.key] = parseFloat(el.value);
        } else {
            result[field.key] = el.value;
        }
    });
    return result;
}

function getActionJsonFromBuilder() {
    if (actionBuilderMode === 'visual') {
        return actionBuilderToJson();
    } else {
        return JSON.parse(document.getElementById('bm-actionValue').value || '{}');
    }
}

// Called when action type dropdown changes
function onActionTypeChanged() {
    const actionType = document.getElementById('bm-actionType').value;
    if (actionBuilderMode === 'visual') {
        renderActionBuilder(actionType, {});
    }
    // Sync textarea hint too
    const textarea = document.getElementById('bm-actionValue');
    if (!textarea.value || textarea.value === '{}') {
        const hint = ACTION_HINTS[actionType];
        if (hint) textarea.value = hint;
    }
}
