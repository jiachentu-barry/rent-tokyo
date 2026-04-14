const form = document.getElementById('searchForm');
const wardInput = document.getElementById('ward');
const wardPanel = document.getElementById('wardPanel');

const wards = [
    '千代田区', '中央区', '港区', '新宿区', '文京区', '台東区',
    '墨田区', '江東区', '品川区', '目黒区', '大田区', '世田谷区',
    '渋谷区', '中野区', '杉並区', '豊島区', '北区', '荒川区',
    '板橋区', '練馬区', '足立区', '葛飾区', '江戸川区'
];

function getSelectedWards() {
    return new Set(
        (wardInput?.value || '')
            .split(',')
            .map(item => item.trim())
            .filter(Boolean)
    );
}

function syncSelectedWards(selectedWards) {
    wardInput.value = Array.from(selectedWards).join(',');
}

function renderWardPanel() {
    if (!wardPanel) return;

    const selectedWards = getSelectedWards();
    wardPanel.innerHTML = [''].concat(wards).map(ward => {
        const label = ward || '全部';
        const active = ward ? selectedWards.has(ward) : selectedWards.size === 0;
        return `<button type="button" class="ward-chip${active ? ' active' : ''}" data-ward="${ward}">${label}</button>`;
    }).join('');
}

function applyUrlState() {
    const params = new URLSearchParams(location.search);
    for (const el of form.elements) {
        if (!el.name) continue;
        if (el.type === 'checkbox') {
            el.checked = params.get(el.name) === el.value;
            continue;
        }
        const value = params.get(el.name);
        if (value != null) {
            el.value = value;
        }
    }
    renderWardPanel();
}

if (wardPanel) {
    wardPanel.addEventListener('click', event => {
        const button = event.target.closest('.ward-chip');
        if (!button) return;

        const ward = button.dataset.ward || '';
        const selectedWards = getSelectedWards();

        if (!ward) {
            selectedWards.clear();
        } else if (selectedWards.has(ward)) {
            selectedWards.delete(ward);
        } else {
            selectedWards.add(ward);
        }

        syncSelectedWards(selectedWards);
        renderWardPanel();
    });
}

form?.addEventListener('submit', event => {
    event.preventDefault();

    const params = new URLSearchParams();
    for (const el of form.elements) {
        if (!el.name) continue;
        if ((el.type === 'checkbox' || el.type === 'radio') && !el.checked) continue;
        if (!el.value) continue;
        params.set(el.name, el.value);
    }
    params.set('page', '0');

    location.href = '/results?' + params.toString();
});

renderWardPanel();
applyUrlState();
