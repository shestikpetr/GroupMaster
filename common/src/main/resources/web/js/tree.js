// Interactive group tree + detail panel

let treeGroups = [];
let selectedGroupId = null;

async function loadTree() {
    const res = await api('GET', '/api/groups');
    treeGroups = await res.json();
    renderTree();
}

function renderTree() {
    const tree = document.getElementById('group-tree');
    const roots = treeGroups.filter(g => !g.parentId);

    function buildTree(parent) {
        const children = treeGroups.filter(g => g.parentId === parent.id);
        if (!children.length) return '';
        return '<ul>' + children.map(c =>
            '<li>' + nodeHtml(c) + buildTree(c) + '</li>'
        ).join('') + '</ul>';
    }

    function nodeHtml(g) {
        const sel = g.id === selectedGroupId ? ' tree-node-selected' : '';
        return `<div class="tree-node${sel}" onclick="selectGroup('${esc(g.id)}')" data-group-id="${esc(g.id)}">
            <span class="tree-node-name">${esc(g.displayName)}</span>
            <span class="group-id">${esc(g.id)}</span>
            <span class="group-priority">p:${g.priority}</span>
        </div>`;
    }

    tree.innerHTML = roots.length
        ? roots.map(r => '<li>' + nodeHtml(r) + buildTree(r) + '</li>').join('')
        : '<li style="color:#888">No groups yet</li>';
}

async function selectGroup(groupId) {
    selectedGroupId = groupId;
    renderTree();
    await loadGroupDetail(groupId);
}

async function loadGroupDetail(groupId) {
    const detail = document.getElementById('group-detail');
    detail.innerHTML = '<div class="detail-loading">Loading...</div>';

    try {
        const [groupRes, playersRes, bonusesRes] = await Promise.all([
            api('GET', '/api/groups/' + groupId),
            api('GET', '/api/players'),
            api('GET', '/api/bonuses/group/' + groupId)
        ]);
        const group = await groupRes.json();
        const allPlayers = await playersRes.json();
        const bonuses = await bonusesRes.json();

        const players = allPlayers.filter(p => p.groupId === groupId);

        // Find parent name
        const parent = group.parentId ? treeGroups.find(g => g.id === group.parentId) : null;
        const parentLabel = parent ? `${parent.displayName} (${parent.id})` : '—';

        // Find children
        const children = treeGroups.filter(g => g.parentId === groupId);

        detail.innerHTML = `
            <div class="detail-header">
                <div>
                    <h2>${esc(group.displayName)}</h2>
                    <span class="detail-id">${esc(group.id)}</span>
                </div>
                <div class="detail-actions">
                    <button class="btn btn-sm" onclick="openEditGroupModal('${esc(group.id)}')">Edit</button>
                    <button class="btn btn-sm btn-danger" onclick="deleteGroup('${esc(group.id)}')">Delete</button>
                </div>
            </div>

            <div class="detail-info">
                <div class="detail-info-item">
                    <span class="detail-info-label">Parent</span>
                    <span>${esc(parentLabel)}</span>
                </div>
                <div class="detail-info-item">
                    <span class="detail-info-label">Priority</span>
                    <span>${group.priority}</span>
                </div>
                <div class="detail-info-item">
                    <span class="detail-info-label">Children</span>
                    <span>${children.length ? children.map(c => esc(c.displayName)).join(', ') : '—'}</span>
                </div>
            </div>

            <div class="detail-section">
                <div class="detail-section-header">
                    <h3>Players (${players.length})</h3>
                </div>
                ${players.length ? `
                    <table class="detail-table">
                        <thead><tr><th>Name</th><th>UUID</th><th>Assigned</th><th>Actions</th></tr></thead>
                        <tbody>
                            ${players.map(p => `
                                <tr>
                                    <td>${esc(p.playerName)}</td>
                                    <td style="font-size:11px;color:#888">${p.playerUuid}</td>
                                    <td>${new Date(p.assignedAt).toLocaleString()}</td>
                                    <td>
                                        <button class="btn btn-sm btn-secondary" onclick="viewPlayerStacks('${p.playerUuid}', '${esc(p.playerName)}')">Stacks</button>
                                        <button class="btn btn-sm" onclick="openReassignModal('${p.playerUuid}', '${esc(p.playerName)}', '${esc(p.groupId)}')">Reassign</button>
                                    </td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                ` : '<p class="detail-empty-text">No players in this group</p>'}
            </div>

            <div class="detail-section">
                <div class="detail-section-header">
                    <h3>Bonuses (${bonuses.length})</h3>
                    <button class="btn btn-sm" onclick="openCreateBonusForGroup('${esc(groupId)}')">+ Add</button>
                </div>
                ${bonuses.length ? `
                    <table class="detail-table">
                        <thead><tr><th>ID</th><th>Trigger</th><th>Action</th><th>Stacking</th><th>Actions</th></tr></thead>
                        <tbody>
                            ${bonuses.map(b => {
                                const stackText = b.maxStacks > 0
                                    ? b.maxStacks + 'x ' + b.stackMode + (b.resetOn !== 'never' ? ' reset:' + b.resetOn : '')
                                    : '';
                                return `<tr>
                                    <td>${b.id}</td>
                                    <td><span class="badge badge-trigger">${esc(b.trigger)}</span></td>
                                    <td>
                                        <span class="badge badge-action">${esc(b.actionType)}</span>
                                        <span style="font-size:11px;color:#aaa;margin-left:4px">${esc(truncate(b.actionValue, 30))}</span>
                                    </td>
                                    <td>${stackText ? '<span class="badge badge-stack">' + esc(stackText) + '</span>' : '—'}</td>
                                    <td>
                                        <button class="btn btn-sm" onclick="openEditBonusModal(${b.id}, '${esc(b.groupId)}')">Edit</button>
                                        <button class="btn btn-sm btn-danger" onclick="deleteBonusFromDetail(${b.id}, '${esc(groupId)}')">Del</button>
                                    </td>
                                </tr>`;
                            }).join('')}
                        </tbody>
                    </table>
                ` : '<p class="detail-empty-text">No bonuses for this group</p>'}
            </div>
        `;
    } catch (e) {
        detail.innerHTML = '<div class="detail-empty"><p style="color:#ff6b6b">Error loading group details</p></div>';
    }
}

function openCreateBonusForGroup(groupId) {
    // Switch to bonuses tab and open create modal pre-filtered
    switchTab('bonuses');
    setTimeout(() => {
        document.getElementById('bonus-group-filter').value = groupId;
        openCreateBonusModal();
        // Pre-select the group in modal
        const sel = document.getElementById('bm-group');
        if (sel) sel.value = groupId;
    }, 200);
}

async function deleteBonusFromDetail(bonusId, groupId) {
    if (!confirm('Delete bonus #' + bonusId + '?')) return;
    await api('DELETE', '/api/bonuses/' + bonusId);
    loadGroupDetail(groupId);
}
