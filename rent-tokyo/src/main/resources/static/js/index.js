const form = document.getElementById('searchForm');
const results = document.getElementById('results');
const summary = document.getElementById('summary');

async function runSearch() {
    const params = new URLSearchParams();
    for (const el of form.elements) {
        if (!el.name || !el.value) continue;
        params.append(el.name, el.value);
    }
    params.set('page', '0');

    summary.textContent = '検索中...';
    results.innerHTML = '';

    try {
        const res = await fetch('/api/properties/search?' + params.toString());
        if (!res.ok) throw new Error('検索に失敗しました');

        const data = await res.json();
        const items = data.content || [];
        summary.textContent = `検索結果: ${data.totalElements ?? items.length} 件`;

        if (items.length === 0) {
            results.innerHTML = '<div class="empty">該当する物件がありません。</div>';
            return;
        }

        results.innerHTML = items.map(item => `
            <article class="card">
                <h3>${item.name || '物件名未設定'}</h3>
                <div class="rent">¥${(item.rent || 0).toLocaleString()}</div>
                <div class="meta">${item.ward || '-'} / ${item.layout || '-'} / 徒歩${item.walkMinutes ?? '-'}分</div>
                <div class="meta">住所: ${item.address || '-'}</div>
                <div class="meta">最寄駅: ${item.nearestStation || '-'}</div>
                <div class="actions"><a href="/detail?id=${item.id}">詳細を見る</a></div>
            </article>
        `).join('');
    } catch (err) {
        summary.textContent = 'エラーが発生しました。';
        results.innerHTML = `<div class="empty">${err.message}</div>`;
    }
}

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    await runSearch();
});

runSearch();
