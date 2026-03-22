// Main app: tab routing, helpers, data loading

function switchTab(name) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    document.querySelector(`.tab[data-tab="${name}"]`).classList.add('active');
    document.getElementById('panel-' + name).classList.add('active');

    // Load tab-specific data
    if (name === 'bonuses') loadBonuses();
}

function loadAllData() {
    loadGroups();
    loadPlayers();
    loadTree();
    // Bonuses loaded on tab switch to avoid unnecessary calls
}

function openModal(id) { document.getElementById(id).classList.add('open'); }
function closeModal(id) { document.getElementById(id).classList.remove('open'); }

function esc(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// Populate a <select> with groups
async function populateGroupSelect(selectId, currentValue, options) {
    const res = await api('GET', '/api/groups');
    const groups = await res.json();
    const sel = document.getElementById(selectId);
    const includeNone = options && options.includeNone;
    const excludeId = options && options.excludeId;

    sel.innerHTML = includeNone ? '<option value="">— None —</option>' : '';
    groups.filter(g => g.id !== excludeId).forEach(g => {
        const opt = document.createElement('option');
        opt.value = g.id;
        opt.textContent = g.displayName + ' (' + g.id + ')';
        if (g.id === currentValue) opt.selected = true;
        sel.appendChild(opt);
    });
}
