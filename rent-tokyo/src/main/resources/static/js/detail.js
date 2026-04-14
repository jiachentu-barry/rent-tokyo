const id = new URLSearchParams(location.search).get('id');
const nameEl = document.getElementById('name');
const rentEl = document.getElementById('rent');
const infoEl = document.getElementById('info');
const historyBody = document.getElementById('historyBody');

function row(label, value) {
    return `<div class="info-item"><div class="label">${label}</div><div class="value">${value ?? '-'}</div></div>`;
}

async function load() {
    if (!id) {
        nameEl.textContent = '物件IDが指定されていません';
        rentEl.textContent = '-';
        historyBody.innerHTML = '<tr><td colspan="4">データがありません。</td></tr>';
        return;
    }

    try {
        const [detailRes, historyRes] = await Promise.all([
            fetch(`/api/properties/${id}`),
            fetch(`/api/properties/${id}/price-history`)
        ]);

        if (!detailRes.ok) throw new Error('物件詳細の取得に失敗しました');
        if (!historyRes.ok) throw new Error('価格履歴の取得に失敗しました');

        const detail = await detailRes.json();
        const history = await historyRes.json();

        nameEl.textContent = detail.name || '物件詳細';
        rentEl.textContent = `¥${(detail.rent || 0).toLocaleString()}`;
        infoEl.innerHTML = [
            row('住所', detail.address),
            row('区', detail.ward),
            row('最寄駅', detail.nearestStation),
            row('徒歩', detail.walkMinutes != null ? `${detail.walkMinutes}分` : '-'),
            row('間取り', detail.layout),
            row('面積', detail.areaSqm != null ? `${detail.areaSqm}㎡` : '-'),
            row('築年', detail.builtYear),
            row('管理費', detail.managementFee != null ? `¥${detail.managementFee.toLocaleString()}` : '-')
        ].join('');

        if (!history.length) {
            historyBody.innerHTML = '<tr><td colspan="4">価格変動履歴はありません。</td></tr>';
            return;
        }

        historyBody.innerHTML = history.map(item => `
            <tr>
                <td>${item.detectedAt ? item.detectedAt.replace('T', ' ') : '-'}</td>
                <td>${item.oldRent != null ? '¥' + item.oldRent.toLocaleString() : '-'}</td>
                <td>${item.newRent != null ? '¥' + item.newRent.toLocaleString() : '-'}</td>
                <td>${item.changeAmount != null ? '¥' + item.changeAmount.toLocaleString() : '-'}</td>
            </tr>
        `).join('');
    } catch (err) {
        nameEl.textContent = '読み込みエラー';
        rentEl.textContent = '-';
        historyBody.innerHTML = `<tr><td colspan="4">${err.message}</td></tr>`;
    }
}

load();
