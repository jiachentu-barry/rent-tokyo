const summaryEl = document.getElementById('summary');
const resultsEl = document.getElementById('results');
const pagerEl = document.getElementById('pager');
const sortSelect = document.getElementById('sortSelect');
const compareList = document.getElementById('compareList');

const selectedForCompare = new Map();

function getSearchParams() {
    const params = new URLSearchParams(location.search);
    return {
        ward: params.get('ward') || '',
        rentMin: params.get('rentMin') || '',
        rentMax: params.get('rentMax') || '',
        layout: params.get('layout') || '',
        walkMinutesMax: params.get('walkMinutesMax') || '',
        hasPriceChanges: params.get('hasPriceChanges') || '',
        size: params.get('size') || '20',
        page: params.get('page') || '0'
    };
}

function yen(value) {
    return value != null ? `¥${Number(value).toLocaleString()}` : '-';
}

function sortItems(items) {
    const cloned = [...items];
    switch (sortSelect?.value) {
        case 'rentDesc':
            return cloned.sort((a, b) => (b.rent || 0) - (a.rent || 0));
        case 'walkAsc':
            return cloned.sort((a, b) => (a.walkMinutes ?? 999) - (b.walkMinutes ?? 999));
        case 'rentAsc':
        default:
            return cloned.sort((a, b) => (a.rent || 0) - (b.rent || 0));
    }
}

function renderCompareList() {
    const items = Array.from(selectedForCompare.values());
    if (!items.length) {
        compareList.textContent = 'まだ選択されていません。';
        return;
    }

    compareList.innerHTML = items.map(item => `
        <div class="compare-item">
            <div>
                <strong>${item.name || '物件名未設定'}</strong>
                <div class="meta">${yen(item.rent)} / ${item.layout || '-'}</div>
                <div class="meta">最寄り駅: ${item.nearestStation || '-'}</div>
                <div class="meta">徒歩時間: ${item.walkMinutes != null ? item.walkMinutes + '分' : '-'}</div>
            </div>
        </div>
    `).join('');
}

function toggleCompare(item, checked) {
    if (checked) {
        if (selectedForCompare.size >= 3 && !selectedForCompare.has(item.id)) {
            alert('比較は3件まで選択できます。');
            return false;
        }
        selectedForCompare.set(item.id, item);
    } else {
        selectedForCompare.delete(item.id);
    }
    renderCompareList();
    return true;
}

function renderPager(currentPage, totalPages) {
    if (!totalPages || totalPages <= 1) {
        pagerEl.innerHTML = '';
        return;
    }

    pagerEl.innerHTML = `
        <button type="button" ${currentPage <= 0 ? 'disabled' : ''} data-page="${currentPage - 1}">前へ</button>
        <span> ${currentPage + 1} / ${totalPages} </span>
        <button type="button" ${currentPage + 1 >= totalPages ? 'disabled' : ''} data-page="${currentPage + 1}">次へ</button>
    `;
}

async function loadResults() {
    const params = getSearchParams();
    summaryEl.textContent = '検索中...';
    resultsEl.innerHTML = '';

    try {
        const data = await window.PropertyApi.searchProperties(params);
        const items = sortItems(data.content || []);

        summaryEl.textContent = `検索結果: ${data.totalElements ?? items.length} 件`;

        if (!items.length) {
            resultsEl.innerHTML = '<div class="empty">該当する物件がありません。</div>';
            renderPager(0, 0);
            return;
        }

        resultsEl.innerHTML = items.map(item => `
            <article class="card">
                <div class="card-top">
                    <h3>${item.name || '物件名未設定'}</h3>
                    <label class="compare-check">
                        <input type="checkbox" data-id="${item.id}">
                        比較
                    </label>
                </div>
                <div class="rent">${yen(item.rent)}</div>
                <div class="meta">${item.ward || '-'} / ${item.layout || '-'} / 徒歩${item.walkMinutes ?? '-'}分</div>
                <div class="meta">住所: ${item.address || '-'}</div>
                <div class="meta">最寄駅: ${item.nearestStation || '-'}</div>
                <div class="actions"><a href="/detail?id=${item.id}">詳細を見る</a></div>
            </article>
        `).join('');

        resultsEl.querySelectorAll('input[type="checkbox"][data-id]').forEach((checkbox, index) => {
            const item = items[index];
            checkbox.checked = selectedForCompare.has(item.id);
            checkbox.addEventListener('change', event => {
                const accepted = toggleCompare(item, event.target.checked);
                if (!accepted) {
                    event.target.checked = false;
                }
            });
        });

        renderPager(Number(data.number || 0), Number(data.totalPages || 0));
    } catch (error) {
        summaryEl.textContent = 'エラーが発生しました。';
        resultsEl.innerHTML = `<div class="empty">${error.message}</div>`;
    }
}

sortSelect?.addEventListener('change', () => {
    loadResults();
});

pagerEl?.addEventListener('click', event => {
    const button = event.target.closest('button[data-page]');
    if (!button) return;

    const params = new URLSearchParams(location.search);
    params.set('page', button.dataset.page);
    location.search = params.toString();
});

renderCompareList();
loadResults();
