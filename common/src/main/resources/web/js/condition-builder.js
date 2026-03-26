// Visual Condition Builder — recursive UI for composing bonus conditions

const CONDITION_DEFS = {
    always:       { category: 'simple', label: 'Always' },
    never:        { category: 'simple', label: 'Never' },
    is_day:       { category: 'simple', label: 'Is Day' },
    is_night:     { category: 'simple', label: 'Is Night' },
    in_sunlight:  { category: 'simple', label: 'In Sunlight' },
    in_water:     { category: 'simple', label: 'In Water' },
    in_rain:      { category: 'simple', label: 'In Rain' },
    on_fire:      { category: 'simple', label: 'On Fire' },
    sneaking:     { category: 'simple', label: 'Sneaking' },
    health_below: { category: 'param', label: 'Health Below', params: [{ key: 'value', label: 'Health (0-1)', type: 'number', min: 0, max: 1, step: 0.05, def: 0.5 }] },
    health_above: { category: 'param', label: 'Health Above', params: [{ key: 'value', label: 'Health (0-1)', type: 'number', min: 0, max: 1, step: 0.05, def: 0.5 }] },
    in_dimension: { category: 'param', label: 'In Dimension', params: [{ key: 'dimension', label: 'Dimension', type: 'text', def: 'minecraft:overworld' }] },
    and:          { category: 'composite', label: 'AND (all match)', mode: 'array' },
    or:           { category: 'composite', label: 'OR (any match)', mode: 'array' },
    not:          { category: 'composite', label: 'NOT (invert)', mode: 'single' },
};

let conditionMode = 'visual';

function setConditionMode(mode) {
    conditionMode = mode;
    const builder = document.getElementById('bm-condition-builder');
    const textarea = document.getElementById('bm-condition');
    const visualBtn = document.getElementById('bm-condition-visual-btn');
    const jsonBtn = document.getElementById('bm-condition-json-btn');

    if (mode === 'visual') {
        // Sync: parse textarea JSON into visual builder
        try {
            const json = JSON.parse(textarea.value || '{}');
            renderConditionBuilder(builder, json);
        } catch (e) {
            alert('Cannot parse condition JSON: ' + e.message);
            conditionMode = 'json';
            return;
        }
        builder.style.display = '';
        textarea.style.display = 'none';
        visualBtn.className = 'btn btn-sm';
        jsonBtn.className = 'btn btn-sm btn-secondary';
    } else {
        // Sync: serialize visual builder into textarea
        try {
            const json = conditionBuilderToJson(builder);
            textarea.value = JSON.stringify(json, null, 2);
        } catch (e) {
            // Keep whatever is there
        }
        builder.style.display = 'none';
        textarea.style.display = '';
        visualBtn.className = 'btn btn-sm btn-secondary';
        jsonBtn.className = 'btn btn-sm';
    }
}

function renderConditionBuilder(container, data) {
    container.innerHTML = '';
    const node = createConditionNode(data || {}, 0, false);
    container.appendChild(node);
}

function createConditionNode(data, depth, removable) {
    const node = document.createElement('div');
    node.className = 'condition-node';
    node.style.marginLeft = (depth * 16) + 'px';

    const header = document.createElement('div');
    header.className = 'condition-node-header';

    // Type select
    const select = document.createElement('select');
    select.className = 'condition-type-select';

    const groups = { 'Simple': [], 'Parameterized': [], 'Composite': [] };
    for (const [type, def] of Object.entries(CONDITION_DEFS)) {
        const cat = def.category === 'simple' ? 'Simple' : def.category === 'param' ? 'Parameterized' : 'Composite';
        groups[cat].push({ type, label: def.label });
    }
    for (const [groupName, items] of Object.entries(groups)) {
        const optgroup = document.createElement('optgroup');
        optgroup.label = groupName;
        items.forEach(item => {
            const opt = document.createElement('option');
            opt.value = item.type;
            opt.textContent = item.label;
            optgroup.appendChild(opt);
        });
        select.appendChild(optgroup);
    }

    // Determine type from data
    let type = data.type || 'always';
    if (!CONDITION_DEFS[type]) type = 'always';
    // Handle empty object {} as "always"
    if (!data.type && Object.keys(data).length === 0) type = 'always';
    select.value = type;

    header.appendChild(select);

    // Remove button
    if (removable) {
        const removeBtn = document.createElement('button');
        removeBtn.className = 'btn btn-sm btn-danger condition-remove-btn';
        removeBtn.textContent = '×';
        removeBtn.onclick = () => node.remove();
        header.appendChild(removeBtn);
    }

    node.appendChild(header);

    // Params container
    const paramsContainer = document.createElement('div');
    paramsContainer.className = 'condition-params';
    node.appendChild(paramsContainer);

    // Children container (for composite)
    const childrenContainer = document.createElement('div');
    childrenContainer.className = 'condition-children';
    node.appendChild(childrenContainer);

    function renderForType(t, existingData) {
        paramsContainer.innerHTML = '';
        childrenContainer.innerHTML = '';
        const def = CONDITION_DEFS[t];
        if (!def) return;

        if (def.category === 'param') {
            def.params.forEach(p => {
                const row = document.createElement('div');
                row.className = 'condition-param-row';

                const label = document.createElement('label');
                label.textContent = p.label;
                label.className = 'condition-param-label';
                row.appendChild(label);

                const input = document.createElement('input');
                input.type = p.type;
                input.className = 'condition-param-input';
                input.dataset.key = p.key;
                if (p.min !== undefined) input.min = p.min;
                if (p.max !== undefined) input.max = p.max;
                if (p.step !== undefined) input.step = p.step;
                input.value = existingData && existingData[p.key] !== undefined ? existingData[p.key] : p.def;
                row.appendChild(input);

                paramsContainer.appendChild(row);
            });
        } else if (def.category === 'composite') {
            if (def.mode === 'array') {
                const items = (existingData && existingData.conditions) || [];
                items.forEach(item => {
                    childrenContainer.appendChild(createConditionNode(item, depth + 1, true));
                });

                const addBtn = document.createElement('button');
                addBtn.className = 'btn btn-sm condition-add-btn';
                addBtn.textContent = '+ Add condition';
                addBtn.style.marginLeft = ((depth + 1) * 16) + 'px';
                addBtn.onclick = () => {
                    childrenContainer.insertBefore(
                        createConditionNode({ type: 'always' }, depth + 1, true),
                        addBtn
                    );
                };
                childrenContainer.appendChild(addBtn);
            } else if (def.mode === 'single') {
                const inner = (existingData && existingData.condition) || { type: 'always' };
                childrenContainer.appendChild(createConditionNode(inner, depth + 1, false));
            }
        }
    }

    renderForType(type, data);

    select.onchange = () => {
        renderForType(select.value, {});
    };

    return node;
}

function conditionBuilderToJson(container) {
    const node = container.querySelector('.condition-node');
    if (!node) return {};
    return nodeToJson(node);
}

function nodeToJson(node) {
    const select = node.querySelector(':scope > .condition-node-header > .condition-type-select');
    const type = select.value;

    if (type === 'always') return {};

    const def = CONDITION_DEFS[type];
    if (!def) return {};

    const result = { type };

    if (def.category === 'param') {
        const inputs = node.querySelectorAll(':scope > .condition-params .condition-param-input');
        inputs.forEach(input => {
            const key = input.dataset.key;
            result[key] = input.type === 'number' ? parseFloat(input.value) : input.value;
        });
    } else if (def.category === 'composite') {
        if (def.mode === 'array') {
            const children = node.querySelectorAll(':scope > .condition-children > .condition-node');
            result.conditions = Array.from(children).map(nodeToJson);
        } else if (def.mode === 'single') {
            const child = node.querySelector(':scope > .condition-children > .condition-node');
            result.condition = child ? nodeToJson(child) : {};
        }
    }

    return result;
}

function getConditionJsonFromBuilder() {
    if (conditionMode === 'visual') {
        const container = document.getElementById('bm-condition-builder');
        return conditionBuilderToJson(container);
    } else {
        return JSON.parse(document.getElementById('bm-condition').value || '{}');
    }
}

function initConditionBuilder(conditionData) {
    const builder = document.getElementById('bm-condition-builder');
    const textarea = document.getElementById('bm-condition');

    // Default to visual mode
    conditionMode = 'visual';
    builder.style.display = '';
    textarea.style.display = 'none';

    const visualBtn = document.getElementById('bm-condition-visual-btn');
    const jsonBtn = document.getElementById('bm-condition-json-btn');
    if (visualBtn) visualBtn.className = 'btn btn-sm';
    if (jsonBtn) jsonBtn.className = 'btn btn-sm btn-secondary';

    renderConditionBuilder(builder, conditionData || {});
    textarea.value = JSON.stringify(conditionData || {}, null, 2);
}
