// Groups tab

let editingGroupId = null;

async function loadGroups() {
    const res = await api('GET', '/api/groups');
    const groups = await res.json();
    const tbody = document.getElementById('groups-table');
    tbody.innerHTML = groups.map(g => `
        <tr>
            <td>${esc(g.id)}</td>
            <td>${esc(g.displayName)}</td>
            <td>${g.parentId ? esc(g.parentId) : '—'}</td>
            <td>${g.priority}</td>
            <td>
                <button class="btn btn-sm" onclick="openEditGroupModal('${esc(g.id)}')">Edit</button>
                <button class="btn btn-sm btn-danger" onclick="deleteGroup('${esc(g.id)}')">Delete</button>
            </td>
        </tr>
    `).join('');
}

function openCreateGroupModal() {
    editingGroupId = null;
    document.getElementById('group-modal-title').textContent = 'New group';
    document.getElementById('gm-id').value = '';
    document.getElementById('gm-id').disabled = false;
    document.getElementById('gm-name').value = '';
    document.getElementById('gm-priority').value = '0';
    document.getElementById('gm-submit').textContent = 'Create';
    populateGroupSelect('gm-parent', null, { includeNone: true });
    openModal('group-modal');
}

async function openEditGroupModal(id) {
    const res = await api('GET', '/api/groups/' + id);
    const g = await res.json();
    editingGroupId = id;
    document.getElementById('group-modal-title').textContent = 'Edit group';
    document.getElementById('gm-id').value = g.id;
    document.getElementById('gm-id').disabled = true;
    document.getElementById('gm-name').value = g.displayName;
    document.getElementById('gm-priority').value = g.priority;
    document.getElementById('gm-submit').textContent = 'Save';
    populateGroupSelect('gm-parent', g.parentId, { includeNone: true, excludeId: g.id });
    openModal('group-modal');
}

async function submitGroup() {
    const id = document.getElementById('gm-id').value.trim();
    const displayName = document.getElementById('gm-name').value.trim();
    const parentId = document.getElementById('gm-parent').value || null;
    const priority = parseInt(document.getElementById('gm-priority').value) || 0;

    if (!id || !displayName) { alert('ID and Name are required'); return; }

    if (editingGroupId) {
        await api('PUT', '/api/groups/' + editingGroupId, { displayName, parentId, priority });
    } else {
        await api('POST', '/api/groups', { id, displayName, parentId, priority });
    }
    closeModal('group-modal');
    loadAllData();
}

async function deleteGroup(id) {
    if (!confirm('Delete group "' + id + '"? Children will be re-parented.')) return;
    await api('DELETE', '/api/groups/' + id);
    loadAllData();
}
