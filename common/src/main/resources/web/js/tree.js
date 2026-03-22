// Hierarchy tab

async function loadTree() {
    const res = await api('GET', '/api/groups');
    const groups = await res.json();
    const tree = document.getElementById('group-tree');
    const roots = groups.filter(g => !g.parentId);

    function buildTree(parent) {
        const children = groups.filter(g => g.parentId === parent.id);
        return children.length
            ? '<ul>' + children.map(c => '<li>' + nodeHtml(c) + buildTree(c) + '</li>').join('') + '</ul>'
            : '';
    }

    function nodeHtml(g) {
        return `${esc(g.displayName)} <span class="group-id">${esc(g.id)}</span> <span class="group-priority">p:${g.priority}</span>`;
    }

    tree.innerHTML = roots.length
        ? roots.map(r => '<li>' + nodeHtml(r) + buildTree(r) + '</li>').join('')
        : '<li style="color:#888">No groups yet</li>';
}
