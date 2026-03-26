// Players tab

async function loadPlayers() {
    const res = await api('GET', '/api/players');
    const players = await res.json();
    const tbody = document.getElementById('players-table');
    tbody.innerHTML = players.map(p => `
        <tr>
            <td>${esc(p.playerName)}</td>
            <td style="font-size:11px;color:#888">${p.playerUuid}</td>
            <td>${esc(p.groupId)}</td>
            <td>${new Date(p.assignedAt).toLocaleString()}</td>
            <td>${esc(p.assignedBy)}</td>
            <td><button class="btn btn-sm btn-secondary" onclick="viewPlayerStacks('${p.playerUuid}', '${esc(p.playerName)}')">View</button></td>
            <td>
                <button class="btn btn-sm" onclick="openReassignModal('${p.playerUuid}', '${esc(p.playerName)}', '${esc(p.groupId)}')">Reassign</button>
                <button class="btn btn-sm btn-danger" onclick="removePlayer('${p.playerUuid}')">Remove</button>
            </td>
        </tr>
    `).join('');
}

function openReassignModal(uuid, name, currentGroup) {
    document.getElementById('pm-uuid').value = uuid;
    document.getElementById('pm-name').value = name;
    populateGroupSelect('pm-group', currentGroup);
    openModal('player-modal');
}

async function submitReassign() {
    const uuid = document.getElementById('pm-uuid').value;
    const groupId = document.getElementById('pm-group').value;
    await api('PUT', '/api/players/' + uuid, { groupId });
    closeModal('player-modal');
    loadAllData();
}

async function viewPlayerStacks(uuid, playerName) {
    document.getElementById('stack-modal-title').textContent = 'Stacks: ' + playerName;
    const res = await api('GET', '/api/stacks/player/' + uuid);
    const stacks = await res.json();
    const tbody = document.getElementById('stack-table');
    if (!stacks.length) {
        tbody.innerHTML = '<tr><td colspan="2" style="text-align:center;color:#888">No active stacks</td></tr>';
    } else {
        tbody.innerHTML = stacks.map(s => `
            <tr>
                <td>#${s.bonusId}</td>
                <td>${s.stacks}</td>
            </tr>
        `).join('');
    }
    openModal('stack-modal');
}

async function removePlayer(uuid) {
    if (!confirm('Remove player from group?')) return;
    await api('DELETE', '/api/players/' + uuid);
    loadAllData();
}
