// Events tab

function logEvent(type, action, data) {
    const log = document.getElementById('events-log');
    const time = new Date().toLocaleTimeString();
    const name = data.displayName || data.playerName || data.id || '';
    log.innerHTML = `<div class="event-entry"><span class="event-time">${time}</span> <span class="event-type">[${type}:${action}]</span> ${esc(name)}</div>` + log.innerHTML;
}
