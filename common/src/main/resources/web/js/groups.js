// Groups — CRUD operations (modal-driven)

let editingGroupId = null;

async function loadGroups() {
    // Groups tab now uses the tree — just reload tree
    await loadTree();
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
    await loadTree();
    if (selectedGroupId) loadGroupDetail(selectedGroupId);
}

async function deleteGroup(id) {
    if (!confirm('Delete group "' + id + '"? Children will be re-parented.')) return;
    await api('DELETE', '/api/groups/' + id);
    if (selectedGroupId === id) {
        selectedGroupId = null;
        document.getElementById('group-detail').innerHTML =
            '<div class="detail-empty"><p style="color:#666">Group deleted. Select another group.</p></div>';
    }
    await loadTree();
}
