const id = new URLSearchParams(location.search).get('id');
const nameEl = document.getElementById('name');
const rentEl = document.getElementById('rent');
const infoEl = document.getElementById('info');
const historyBody = document.getElementById('historyBody');
const initialCostEl = document.getElementById('initialCost');
const sourceUrlEl = document.getElementById('sourceUrl');
const favBtnDetail = document.getElementById('favBtnDetail');

// ── Favorite (detail page) ───────────────────────────────────
function applyDetailFavState(isActive) {
    if (isActive) {
        favBtnDetail.classList.add('fav-btn--active');
        favBtnDetail.textContent = '♥ お気に入り済み（取消）';
    } else {
        favBtnDetail.classList.remove('fav-btn--active');
        favBtnDetail.textContent = '♡ お気に入りに追加';
    }
}

async function loadDetailFavState() {
    const userId = localStorage.getItem('rent_userId');
    if (!userId || !id || !favBtnDetail) return;
    try {
        const list = await fetch('/api/favorites?userId=' + userId).then(r => r.json());
        const already = list.some(f => String(f.propertyId) === String(id));
        applyDetailFavState(already);
    } catch { /* ignore */ }
}

async function toggleDetailFavorite() {
    const userId = localStorage.getItem('rent_userId');
    if (!userId) {
        alert('お気に入りに追加するにはログインが必要です。\nマイページからログインしてください。');
        return;
    }
    if (!id) return;
    const isActive = favBtnDetail.classList.contains('fav-btn--active');
    favBtnDetail.disabled = true;
    try {
        if (isActive) {
            const r = await fetch(`/api/favorites/${id}?userId=${userId}`, { method: 'DELETE' });
            if (!r.ok) throw new Error();
            applyDetailFavState(false);
        } else {
            const r = await fetch(`/api/favorites?userId=${userId}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ propertyId: Number(id) })
            });
            if (!r.ok) throw new Error();
            applyDetailFavState(true);
        }
    } catch {
        alert('操作に失敗しました。');
    } finally {
        favBtnDetail.disabled = false;
    }
}

if (favBtnDetail) {
    favBtnDetail.addEventListener('click', toggleDetailFavorite);
    loadDetailFavState();
}

function row(label, value) {
    return `<div class="info-item"><div class="label">${label}</div><div class="value">${value ?? '-'}</div></div>`;
}

function formatDateTime(value) {
    if (!value) return '-';

    const normalized = String(value).replace('T', ' ');
    const match = normalized.match(/^(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})/);
    if (match) {
        return `${match[1]} ${match[2]}`;
    }

    return normalized.slice(0, 16);
}

function formatYen(value) {
    return value != null ? `¥${Number(value).toLocaleString()}` : '-';
}

function estimateInitialCost(detail) {
    const total = [detail.rent, detail.managementFee, detail.deposit, detail.keyMoney]
        .reduce((sum, value) => sum + (Number(value) || 0), 0);
    return total > 0 ? `概算 ${formatYen(total)}` : 'データなし';
}

async function load() {
    if (!id) {
        nameEl.textContent = '物件IDが指定されていません';
        rentEl.textContent = '-';
        initialCostEl.textContent = 'データがありません。';
        historyBody.innerHTML = '<tr><td colspan="4">データがありません。</td></tr>';
        return;
    }

    try {
        const [detail, history] = await Promise.all([
            window.PropertyApi.fetchPropertyDetail(id),
            window.PropertyApi.fetchPriceHistory(id)
        ]);

        nameEl.textContent = detail.name || '物件詳細';
        rentEl.textContent = formatYen(detail.rent || 0);
        initialCostEl.textContent = estimateInitialCost(detail);

        if (detail.sourceUrl) {
            sourceUrlEl.href = detail.sourceUrl;
        } else {
            sourceUrlEl.removeAttribute('href');
            sourceUrlEl.textContent = 'SUUMO掲載ページなし';
        }

        infoEl.innerHTML = [
            row('住所', detail.address),
            row('エリア', detail.ward),
            row('最寄り駅', detail.nearestStation),
            row('徒歩', detail.walkMinutes != null ? `${detail.walkMinutes}分` : '-'),
            row('間取り', detail.layout),
            row('面積', detail.areaSqm != null ? `${detail.areaSqm}㎡` : '-'),
            row('築年数', detail.builtYear),
            row('管理費', formatYen(detail.managementFee)),
            row('敷金', formatYen(detail.deposit)),
            row('礼金', formatYen(detail.keyMoney))
        ].join('');

        if (!history.length) {
            historyBody.innerHTML = '<tr><td colspan="4">価格変動履歴はありません。</td></tr>';
            return;
        }

        historyBody.innerHTML = history.map(item => `
            <tr>
                <td>${formatDateTime(item.detectedAt)}</td>
                <td>${formatYen(item.oldRent)}</td>
                <td>${formatYen(item.newRent)}</td>
                <td>${formatYen(item.changeAmount)}</td>
            </tr>
        `).join('');
    } catch (err) {
        nameEl.textContent = '読み込みエラー';
        rentEl.textContent = '-';
        initialCostEl.textContent = '-';
        historyBody.innerHTML = `<tr><td colspan="4">${err.message}</td></tr>`;
    }
}

load();
