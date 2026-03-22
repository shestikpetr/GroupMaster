// API client and SSE connection

let TOKEN = localStorage.getItem('gm_token') || '';
let BASE = window.location.origin;
let eventSource = null;

async function api(method, path, body) {
    const opts = {
        method,
        headers: { 'Authorization': 'Bearer ' + TOKEN, 'Content-Type': 'application/json' }
    };
    if (body) opts.body = JSON.stringify(body);
    return fetch(BASE + path, opts);
}

function authenticate() {
    TOKEN = document.getElementById('token-input').value.trim();
    localStorage.setItem('gm_token', TOKEN);
    tryConnect();
}

async function tryConnect() {
    try {
        const res = await api('GET', '/api/groups');
        if (res.ok) {
            document.getElementById('auth-screen').style.display = 'none';
            document.getElementById('app').style.display = 'block';
            loadAllData();
            connectSSE();
        } else {
            alert('Invalid token');
        }
    } catch (e) {
        alert('Cannot connect to server');
    }
}

function connectSSE() {
    if (eventSource) eventSource.close();
    eventSource = new EventSource(BASE + '/api/events?token=' + TOKEN);
    const status = document.getElementById('connection-status');

    eventSource.addEventListener('connected', () => {
        status.textContent = 'connected';
        status.className = 'connected';
    });

    eventSource.addEventListener('group', (e) => {
        const data = JSON.parse(e.data);
        logEvent('group', data.action, data.data);
        loadAllData();
    });

    eventSource.addEventListener('player', (e) => {
        const data = JSON.parse(e.data);
        logEvent('player', data.action, data.data);
        loadAllData();
    });

    eventSource.onerror = () => {
        status.textContent = 'disconnected';
        status.className = '';
    };
}

if (TOKEN) tryConnect();
