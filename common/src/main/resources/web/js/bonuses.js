// Bonuses tab — full CRUD with stacking, conditions, targets

let bonusActionTypes = [];
let bonusConditionTypes = [];
let allGroups = [];

const ACTION_HINTS = {
    effect: '{"effect":"minecraft:speed","amplifier":1,"duration":10}',
    attribute: '{"attribute":"minecraft:generic.max_health","amount":4.0,"operation":"add_value"}',
    burn: '{"duration":3}',
    command: '{"command":"give {player} minecraft:diamond 1"}',
    message: '{"message":"You feel stronger!"}',
    set_group: '{"group":"paladin_lv2"}'
};

const TRIGGER_DESCRIPTIONS = {
    on_join: 'When player joins group / logs in',
    on_leave: 'When player leaves group',
    tick: 'Every N ticks (configurable interval)',
    on_attack: 'When player attacks an entity',
    on_damaged: 'When player takes damage',
    on_kill: 'When player kills an entity',
    on_death: 'When player dies',
    on_eat: 'When player eats food'
};

async function loadBonuses() {
    // Load supporting data
    const [groupsRes, typesRes, condRes] = await Promise.all([
        api('GET', '/api/groups'),
        api('GET', '/api/bonuses/types'),
        api('GET', '/api/bonuses/conditions')
    ]);
    allGroups = await groupsRes.json();
    bonusActionTypes = await typesRes.json();
    bonusConditionTypes = await condRes.json();

    // Populate group filter
    const filter = document.getElementById('bonus-group-filter');
    const currentVal = filter.value;
    filter.innerHTML = '<option value="">All groups</option>';
    allGroups.forEach(g => {
        const opt = document.createElement('option');
        opt.value = g.id;
        opt.textContent = g.displayName + ' (' + g.id + ')';
        if (g.id === currentVal) opt.selected = true;
        filter.appendChild(opt);
    });

    await renderBonusTable(currentVal);
}

async function renderBonusTable(groupFilter) {
    let bonuses;
    if (groupFilter) {
        const res = await api('GET', '/api/bonuses/group/' + groupFilter);
        bonuses = await res.json();
    } else {
        const res = await api('GET', '/api/bonuses');
        bonuses = await res.json();
    }

    const tbody = document.getElementById('bonuses-table');
    if (!bonuses.length) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:#888">No bonuses configured</td></tr>';
        return;
    }

    tbody.innerHTML = bonuses.map(b => {
        const condText = b.conditionDesc && b.conditionDesc !== 'always' ? b.conditionDesc : '';
        const stackText = b.maxStacks > 0
            ? `${b.maxStacks}x ${b.stackMode}` + (b.resetOn !== 'never' ? ` reset:${b.resetOn}` : '')
            : '';

        return `<tr>
            <td>${b.id}</td>
            <td><span class="badge badge-trigger">${esc(b.trigger)}</span></td>
            <td>
                <span class="badge badge-action">${esc(b.actionType)}</span>
                <span style="font-size:11px;color:#aaa;margin-left:4px">${esc(truncate(b.actionValue, 40))}</span>
            </td>
            <td>${b.target !== 'self' ? '<span class="badge badge-target">' + esc(b.target) + '</span>' : '<span style="color:#666">self</span>'}</td>
            <td>${condText ? '<span class="badge badge-condition">' + esc(condText) + '</span>' : '<span style="color:#666">always</span>'}</td>
            <td>${stackText ? '<span class="badge badge-stack">' + esc(stackText) + '</span>' : '—'}
                ${b.override ? ' <span class="badge badge-override">OVR</span>' : ''}</td>
            <td style="white-space:nowrap">
                <button class="btn btn-sm" onclick="openEditBonusModal(${b.id}, '${esc(b.groupId)}')">Edit</button>
                <button class="btn btn-sm btn-danger" onclick="deleteBonus(${b.id})">Delete</button>
            </td>
        </tr>`;
    }).join('');
}

function onBonusFilterChange() {
    const val = document.getElementById('bonus-group-filter').value;
    renderBonusTable(val);
}

// --- Create / Edit Bonus Modal ---

let editingBonusId = null;

function openCreateBonusModal() {
    editingBonusId = null;
    document.getElementById('bonus-modal-title').textContent = 'New Bonus';
    document.getElementById('bm-submit').textContent = 'Create';

    // Populate group select
    const sel = document.getElementById('bm-group');
    sel.innerHTML = '';
    sel.disabled = false;
    allGroups.forEach(g => {
        const opt = document.createElement('option');
        opt.value = g.id;
        opt.textContent = g.displayName + ' (' + g.id + ')';
        sel.appendChild(opt);
    });
    // Pre-select from filter
    const filterVal = document.getElementById('bonus-group-filter').value;
    if (filterVal) sel.value = filterVal;

    // Populate action types
    const actionSel = document.getElementById('bm-actionType');
    actionSel.innerHTML = '';
    bonusActionTypes.forEach(t => {
        const opt = document.createElement('option');
        opt.value = t.id;
        opt.textContent = t.displayName + ' (' + t.id + ')';
        actionSel.appendChild(opt);
    });

    // Reset fields
    document.getElementById('bm-trigger').value = 'on_join';
    document.getElementById('bm-target').value = 'self';
    initActionBuilder(actionSel.value, {});
    initConditionBuilder({});
    document.getElementById('bm-tickInterval').value = '20';
    document.getElementById('bm-override').checked = false;
    document.getElementById('bm-maxStacks').value = '0';
    document.getElementById('bm-stackMode').value = 'each';
    document.getElementById('bm-resetOn').value = 'never';

    updateActionHint();
    updateTriggerFields();
    openModal('bonus-modal');
}

async function openEditBonusModal(bonusId, groupId) {
    editingBonusId = bonusId;

    // Load bonus data from the group's bonuses
    let bonus;
    const res = await api('GET', '/api/bonuses/group/' + groupId);
    const bonuses = await res.json();
    bonus = bonuses.find(b => b.id === bonusId);
    if (!bonus) {
        // Try all bonuses
        const allRes = await api('GET', '/api/bonuses');
        const all = await allRes.json();
        bonus = all.find(b => b.id === bonusId);
    }
    if (!bonus) { alert('Bonus not found'); return; }

    document.getElementById('bonus-modal-title').textContent = 'Edit Bonus #' + bonusId;
    document.getElementById('bm-submit').textContent = 'Save';

    // Group (disabled for edit)
    const sel = document.getElementById('bm-group');
    sel.innerHTML = '';
    sel.disabled = true;
    const opt = document.createElement('option');
    opt.value = bonus.groupId;
    opt.textContent = bonus.groupId;
    sel.appendChild(opt);

    // Action types
    const actionSel = document.getElementById('bm-actionType');
    actionSel.innerHTML = '';
    bonusActionTypes.forEach(t => {
        const o = document.createElement('option');
        o.value = t.id;
        o.textContent = t.displayName + ' (' + t.id + ')';
        if (t.id === bonus.actionType) o.selected = true;
        actionSel.appendChild(o);
    });

    document.getElementById('bm-trigger').value = bonus.trigger;
    document.getElementById('bm-target').value = bonus.target;
    const actionData = typeof bonus.actionValue === 'object' ? bonus.actionValue :
        (() => { try { return JSON.parse(bonus.actionValue); } catch(e) { return {}; } })();
    initActionBuilder(bonus.actionType, actionData);
    const condData = typeof bonus.condition === 'object' ? bonus.condition :
        (() => { try { return JSON.parse(bonus.condition); } catch(e) { return {}; } })();
    initConditionBuilder(condData);
    document.getElementById('bm-tickInterval').value = bonus.tickInterval || 20;
    document.getElementById('bm-override').checked = bonus.override;
    document.getElementById('bm-maxStacks').value = bonus.maxStacks || 0;
    document.getElementById('bm-stackMode').value = bonus.stackMode || 'each';
    document.getElementById('bm-resetOn').value = bonus.resetOn || 'never';

    updateActionHint();
    updateTriggerFields();
    openModal('bonus-modal');
}

function updateActionHint() {
    // Legacy — now handled by onActionTypeChanged in action-builder.js
    onActionTypeChanged();
}

function updateTriggerFields() {
    const trigger = document.getElementById('bm-trigger').value;
    document.getElementById('bm-tickInterval-group').style.display = trigger === 'tick' ? 'block' : 'none';

    const desc = document.getElementById('bm-triggerDesc');
    desc.textContent = TRIGGER_DESCRIPTIONS[trigger] || '';
}

async function submitBonus() {
    const groupId = document.getElementById('bm-group').value;
    const trigger = document.getElementById('bm-trigger').value;
    const target = document.getElementById('bm-target').value;
    const actionType = document.getElementById('bm-actionType').value;
    const tickInterval = parseInt(document.getElementById('bm-tickInterval').value) || 20;
    const override = document.getElementById('bm-override').checked;
    const maxStacks = parseInt(document.getElementById('bm-maxStacks').value) || 0;
    const stackMode = document.getElementById('bm-stackMode').value;
    const resetOn = document.getElementById('bm-resetOn').value;

    // Validate
    let actionValue, condition;
    try {
        actionValue = getActionJsonFromBuilder();
    } catch (e) {
        alert('Invalid action value: ' + e.message);
        return;
    }
    try {
        condition = getConditionJsonFromBuilder();
    } catch (e) {
        alert('Invalid condition: ' + e.message);
        return;
    }

    if (tickInterval < 1 || tickInterval > 72000) {
        alert('Tick interval must be between 1 and 72000');
        return;
    }
    if (maxStacks < 0 || maxStacks > 1000) {
        alert('Max stacks must be between 0 and 1000');
        return;
    }

    if (editingBonusId) {
        // Edit = delete + recreate (simple approach since we don't have a PATCH endpoint)
        await api('DELETE', '/api/bonuses/' + editingBonusId);
    }

    const body = {
        trigger, target, actionType,
        actionValue: JSON.stringify(actionValue),
        condition,
        tickInterval, override,
        maxStacks, stackMode, resetOn
    };

    const res = await api('POST', '/api/bonuses/group/' + groupId, body);
    const result = await res.json();

    if (res.ok) {
        closeModal('bonus-modal');
        loadBonuses();
    } else {
        alert('Error: ' + (result.error || 'Unknown error'));
    }
}

async function deleteBonus(id) {
    if (!confirm('Delete bonus #' + id + '?')) return;
    await api('DELETE', '/api/bonuses/' + id);
    loadBonuses();
}

