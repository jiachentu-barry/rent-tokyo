/* ============================================================
   user.js – マイページ（ログイン/登録・収藏・履歴・比較・チャート）
   ============================================================ */

const STORAGE_KEY_TOKEN   = 'rent_token';
const STORAGE_KEY_USER_ID = 'rent_userId';
const STORAGE_KEY_NAME    = 'rent_displayName';
const STORAGE_KEY_EMAIL   = 'rent_email';

// Chart instances (kept for destroy on re-draw)
let chartWardRent  = null;
let chartRentDist  = null;
let chartLayout    = null;

// Cache
let favoritesCache = [];

window.UserPage = {

    /* ─── Init ─────────────────────────────────────────────── */
    init() {
        const userId = localStorage.getItem(STORAGE_KEY_USER_ID);
        if (userId) {
            this.showUserSection();
            this.loadFavorites();
        } else {
            document.getElementById('authSection').style.display = '';
            document.getElementById('userSection').style.display = 'none';
        }
    },

    /* ─── Auth helpers ──────────────────────────────────────── */
    switchAuth(tab) {
        const isLogin = tab === 'login';
        document.getElementById('loginForm').style.display    = isLogin ? '' : 'none';
        document.getElementById('registerForm').style.display = isLogin ? 'none' : '';
        document.getElementById('tabLogin').classList.toggle('auth-tab--active', isLogin);
        document.getElementById('tabRegister').classList.toggle('auth-tab--active', !isLogin);
    },

    async doLogin(e) {
        e.preventDefault();
        const btn = document.getElementById('loginBtn');
        const errEl = document.getElementById('loginError');
        errEl.style.display = 'none';
        btn.disabled = true;
        btn.textContent = '処理中…';
        try {
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    email:    document.getElementById('loginEmail').value.trim(),
                    password: document.getElementById('loginPassword').value
                })
            });
            if (!res.ok) {
                const msg = await res.text();
                throw new Error(msg || 'ログインに失敗しました');
            }
            const data = await res.json();
            this.saveSession(data);
            this.showUserSection();
            this.loadFavorites();
        } catch (err) {
            errEl.textContent = err.message;
            errEl.style.display = '';
        } finally {
            btn.disabled = false;
            btn.textContent = 'ログイン';
        }
    },

    async doRegister(e) {
        e.preventDefault();
        const btn = document.getElementById('regBtn');
        const errEl = document.getElementById('regError');
        errEl.style.display = 'none';
        btn.disabled = true;
        btn.textContent = '処理中…';
        try {
            const res = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    displayName: document.getElementById('regName').value.trim(),
                    email:       document.getElementById('regEmail').value.trim(),
                    password:    document.getElementById('regPassword').value
                })
            });
            if (!res.ok) {
                const msg = await res.text();
                throw new Error(msg || '登録に失敗しました');
            }
            const data = await res.json();
            this.saveSession(data);
            this.showUserSection();
            this.loadFavorites();
        } catch (err) {
            errEl.textContent = err.message;
            errEl.style.display = '';
        } finally {
            btn.disabled = false;
            btn.textContent = '登録する';
        }
    },

    saveSession(data) {
        localStorage.setItem(STORAGE_KEY_TOKEN,   data.token       || '');
        localStorage.setItem(STORAGE_KEY_USER_ID, String(data.userId));
        localStorage.setItem(STORAGE_KEY_NAME,    data.displayName || '');
        localStorage.setItem(STORAGE_KEY_EMAIL,   data.email       || '');
    },

    logout() {
        localStorage.removeItem(STORAGE_KEY_TOKEN);
        localStorage.removeItem(STORAGE_KEY_USER_ID);
        localStorage.removeItem(STORAGE_KEY_NAME);
        localStorage.removeItem(STORAGE_KEY_EMAIL);
        favoritesCache = [];
        document.getElementById('authSection').style.display = '';
        document.getElementById('userSection').style.display = 'none';
        // Clear forms
        document.getElementById('loginEmail').value = '';
        document.getElementById('loginPassword').value = '';
    },

    showUserSection() {
        document.getElementById('authSection').style.display = 'none';
        document.getElementById('userSection').style.display = '';
        document.getElementById('userGreeting').textContent =
            'こんにちは、' + (localStorage.getItem(STORAGE_KEY_NAME) || 'ユーザー') + 'さん';
        document.getElementById('userEmail').textContent =
            localStorage.getItem(STORAGE_KEY_EMAIL) || '';
    },

    /* ─── Tab navigation ────────────────────────────────────── */
    showTab(name) {
        ['favorites', 'history', 'compare', 'charts'].forEach(t => {
            document.getElementById('tab-' + t).style.display = t === name ? '' : 'none';
        });
        document.querySelectorAll('.page-tab').forEach((btn, i) => {
            const names = ['favorites', 'history', 'compare', 'charts'];
            btn.classList.toggle('page-tab--active', names[i] === name);
        });
        if (name === 'history')  this.loadHistory();
        if (name === 'charts')   this.renderCharts();
        if (name === 'compare')  this.renderCompareTable();
    },

    /* ─── Favorites ─────────────────────────────────────────── */
    async loadFavorites() {
        const userId = localStorage.getItem(STORAGE_KEY_USER_ID);
        const listEl   = document.getElementById('favList');
        const loadEl   = document.getElementById('favLoading');
        const emptyEl  = document.getElementById('favEmpty');
        listEl.innerHTML = '';
        loadEl.style.display = '';
        emptyEl.style.display = 'none';

        try {
            const data = await fetch('/api/favorites?userId=' + userId).then(r => r.json());
            favoritesCache = data;
            loadEl.style.display = 'none';

            if (!data.length) {
                emptyEl.style.display = '';
                return;
            }

            listEl.innerHTML = data.map(f => `
                <div class="card fav-card" id="fav-card-${f.favoriteId}">
                    <div class="fav-check-wrap">
                        <label class="compare-check">
                            <input type="checkbox" class="fav-compare-cb" value="${f.favoriteId}"
                                   onchange="UserPage.onCompareCheck()">
                            <span class="muted" style="font-size:12px;">比較に追加</span>
                        </label>
                    </div>
                    <div class="card-top">
                        <div>
                            <div class="card-name">
                                <a href="/detail?id=${f.propertyId}" class="property-link">${escHtml(f.propertyName || '物件名不明')}</a>
                            </div>
                            <div class="meta">${escHtml(f.ward || '-')} ／ ${escHtml(f.layout || '-')} ／ 徒歩${f.walkMinutes != null ? f.walkMinutes + '分' : '-'}</div>
                        </div>
                        <div style="text-align:right;">
                            <div class="rent">¥${Number(f.rent || 0).toLocaleString()}</div>
                            <button class="btn-sm btn-danger" style="margin-top:8px;"
                                onclick="UserPage.removeFavorite(${f.propertyId}, ${f.favoriteId})">削除</button>
                        </div>
                    </div>
                    <div class="meta" style="margin-top:6px;font-size:12px;">追加日時: ${formatDateTime(f.createdAt)}</div>
                </div>
            `).join('');
        } catch {
            loadEl.style.display = 'none';
            listEl.innerHTML = '<p class="error-msg">データの取得に失敗しました。</p>';
        }
    },

    async removeFavorite(propertyId, favoriteId) {
        const userId = localStorage.getItem(STORAGE_KEY_USER_ID);
        try {
            const r = await fetch(`/api/favorites/${propertyId}?userId=${userId}`, { method: 'DELETE' });
            if (!r.ok) throw new Error();
            favoritesCache = favoritesCache.filter(f => f.favoriteId !== favoriteId);
            const card = document.getElementById('fav-card-' + favoriteId);
            if (card) card.remove();
            if (!favoritesCache.length) document.getElementById('favEmpty').style.display = '';
        } catch {
            alert('削除に失敗しました。');
        }
    },

    onCompareCheck() {
        const checked = document.querySelectorAll('.fav-compare-cb:checked');
        if (checked.length > 3) {
            // Uncheck the last one
            checked[checked.length - 1].checked = false;
            alert('比較は最大3件までです。');
        }
    },

    goCompare() {
        const checked = [...document.querySelectorAll('.fav-compare-cb:checked')];
        if (!checked.length) {
            alert('比較する物件にチェックを入れてください。');
            return;
        }
        this.showTab('compare');
    },

    /* ─── Search History ────────────────────────────────────── */
    async loadHistory() {
        const userId  = localStorage.getItem(STORAGE_KEY_USER_ID);
        const loadEl = document.getElementById('histLoading');
        const emptyEl = document.getElementById('histEmpty');
        const tableEl = document.getElementById('histTable');
        const bodyEl  = document.getElementById('histBody');
        loadEl.style.display = '';
        emptyEl.style.display = 'none';
        tableEl.style.display = 'none';
        bodyEl.innerHTML = '';

        try {
            const data = await fetch('/api/search-histories?userId=' + userId).then(r => r.json());
            loadEl.style.display = 'none';
            if (!data.length) { emptyEl.style.display = ''; return; }

            tableEl.style.display = '';
            bodyEl.innerHTML = data.map(h => {
                const rentRange = [h.rentMin, h.rentMax]
                    .filter(v => v != null)
                    .map(v => '¥' + Number(v).toLocaleString())
                    .join(' 〜 ') || '-';
                const query = buildSearchQuery(h);
                return `<tr>
                    <td style="white-space:nowrap;">${formatDateTime(h.createdAt)}</td>
                    <td>${escHtml(h.ward || '-')}</td>
                    <td>${rentRange}</td>
                    <td>${escHtml(h.layout || '-')}</td>
                    <td>${h.walkMinutesMax != null ? h.walkMinutesMax + '分以内' : '-'}</td>
                    <td>
                        <a href="/results?${query}" class="actions-link">再検索</a>
                        <button class="btn-sm btn-danger" style="margin-left:6px;"
                            onclick="UserPage.deleteHistory(${h.id}, this)">削除</button>
                    </td>
                </tr>`;
            }).join('');
        } catch {
            loadEl.style.display = 'none';
            document.getElementById('histBody').innerHTML = '<tr><td colspan="6" class="error-msg">取得に失敗しました。</td></tr>';
            tableEl.style.display = '';
        }
    },

    async deleteHistory(historyId, btn) {
        const userId = localStorage.getItem(STORAGE_KEY_USER_ID);
        btn.disabled = true;
        try {
            const r = await fetch(`/api/search-histories/${historyId}?userId=${userId}`, { method: 'DELETE' });
            if (!r.ok) throw new Error();
            btn.closest('tr').remove();
        } catch {
            alert('削除に失敗しました。');
            btn.disabled = false;
        }
    },

    /* ─── Compare ───────────────────────────────────────────── */
    renderCompareTable() {
        const checked = [...document.querySelectorAll('.fav-compare-cb:checked')];
        const compareEmpty = document.getElementById('compareEmpty');
        const compareTable = document.getElementById('compareTable');

        if (!checked.length) {
            compareEmpty.style.display = '';
            compareTable.style.display = 'none';
            return;
        }

        const ids = checked.map(c => Number(c.value));
        const items = favoritesCache.filter(f => ids.includes(f.favoriteId));

        compareEmpty.style.display = 'none';
        compareTable.style.display = '';

        const fields = [
            { label: '物件名',   fn: f => `<a href="/detail?id=${f.propertyId}" class="property-link">${escHtml(f.propertyName || '-')}</a>` },
            { label: 'エリア（区）', fn: f => escHtml(f.ward || '-') },
            { label: '家賃',     fn: f => '¥' + Number(f.rent || 0).toLocaleString() },
            { label: '間取り',   fn: f => escHtml(f.layout || '-') },
            { label: '徒歩分',   fn: f => f.walkMinutes != null ? f.walkMinutes + '分' : '-' },
        ];

        const header = '<thead><tr><th>項目</th>' + items.map(f =>
            `<th>${escHtml(f.propertyName || '物件')}</th>`).join('') + '</tr></thead>';

        const rows = fields.map(field =>
            '<tr><td class="compare-label">' + field.label + '</td>' +
            items.map(f => '<td>' + field.fn(f) + '</td>').join('') +
            '</tr>'
        ).join('');

        document.getElementById('compareContent').innerHTML = header + '<tbody>' + rows + '</tbody>';
    },

    /* ─── Charts ────────────────────────────────────────────── */
    renderCharts() {
        if (!favoritesCache.length) {
            ['chartNoFav', 'chartNoDist', 'chartNoLayout'].forEach(id => {
                document.getElementById(id).style.display = '';
            });
            ['chartWardRent', 'chartRentDist', 'chartLayout'].forEach(id => {
                document.getElementById(id).style.display = 'none';
            });
            return;
        }

        ['chartNoFav', 'chartNoDist', 'chartNoLayout'].forEach(id => {
            document.getElementById(id).style.display = 'none';
        });
        ['chartWardRent', 'chartRentDist', 'chartLayout'].forEach(id => {
            document.getElementById(id).style.display = '';
        });

        this.drawWardRentChart();
        this.drawRentDistChart();
        this.drawLayoutChart();
    },

    drawWardRentChart() {
        const wardMap = {};
        favoritesCache.forEach(f => {
            if (!f.ward) return;
            if (!wardMap[f.ward]) wardMap[f.ward] = { sum: 0, count: 0 };
            wardMap[f.ward].sum   += f.rent || 0;
            wardMap[f.ward].count += 1;
        });
        const labels = Object.keys(wardMap).sort();
        const values = labels.map(w => Math.round(wardMap[w].sum / wardMap[w].count));

        if (chartWardRent) { chartWardRent.destroy(); chartWardRent = null; }
        const ctx = document.getElementById('chartWardRent').getContext('2d');
        chartWardRent = new Chart(ctx, {
            type: 'bar',
            data: {
                labels,
                datasets: [{
                    label: '平均家賃 (¥)',
                    data: values,
                    backgroundColor: 'rgba(37, 99, 235, 0.7)',
                    borderColor: '#1d4ed8',
                    borderWidth: 1,
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, ticks: { callback: v => '¥' + v.toLocaleString() } }
                }
            }
        });
    },

    drawRentDistChart() {
        const bins = ['~5万', '5〜8万', '8〜10万', '10〜15万', '15万〜'];
        const counts = [0, 0, 0, 0, 0];
        favoritesCache.forEach(f => {
            const r = f.rent || 0;
            if      (r < 50000)  counts[0]++;
            else if (r < 80000)  counts[1]++;
            else if (r < 100000) counts[2]++;
            else if (r < 150000) counts[3]++;
            else                 counts[4]++;
        });

        if (chartRentDist) { chartRentDist.destroy(); chartRentDist = null; }
        const ctx = document.getElementById('chartRentDist').getContext('2d');
        chartRentDist = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: bins,
                datasets: [{
                    data: counts,
                    backgroundColor: ['#bfdbfe','#60a5fa','#2563eb','#1d4ed8','#1e3a8a'],
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                plugins: { legend: { position: 'bottom' } }
            }
        });
    },

    drawLayoutChart() {
        const layoutMap = {};
        favoritesCache.forEach(f => {
            const l = f.layout || '不明';
            layoutMap[l] = (layoutMap[l] || 0) + 1;
        });
        const labels = Object.keys(layoutMap).sort();
        const values = labels.map(l => layoutMap[l]);

        if (chartLayout) { chartLayout.destroy(); chartLayout = null; }
        const ctx = document.getElementById('chartLayout').getContext('2d');
        chartLayout = new Chart(ctx, {
            type: 'bar',
            data: {
                labels,
                datasets: [{
                    label: '件数',
                    data: values,
                    backgroundColor: 'rgba(15, 118, 110, 0.7)',
                    borderColor: '#0f766e',
                    borderWidth: 1,
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                plugins: { legend: { display: false } },
                scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } }
            }
        });
    }
};

/* ─── Helpers ──────────────────────────────────────────────── */
function escHtml(str) {
    return String(str ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function formatDateTime(value) {
    if (!value) return '-';
    const s = String(value).replace('T', ' ');
    const m = s.match(/^(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})/);
    return m ? `${m[1]} ${m[2]}` : s.slice(0, 16);
}

function buildSearchQuery(h) {
    const p = new URLSearchParams();
    if (h.ward)           p.set('ward', h.ward);
    if (h.rentMin != null) p.set('rentMin', h.rentMin);
    if (h.rentMax != null) p.set('rentMax', h.rentMax);
    if (h.layout)          p.set('layout', h.layout);
    if (h.walkMinutesMax != null) p.set('walkMinutesMax', h.walkMinutesMax);
    return p.toString();
}

/* ─── Bootstrap ────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => UserPage.init());
