window.PropertyApi = {
    async fetchJson(url) {
        const response = await fetch(url, {
            headers: {
                Accept: 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('データの取得に失敗しました');
        }

        return response.json();
    },

    async searchProperties(params = {}) {
        const query = new URLSearchParams();
        Object.entries(params).forEach(([key, value]) => {
            if (value === undefined || value === null || value === '') {
                return;
            }
            query.set(key, value);
        });

        return this.fetchJson('/api/properties/search?' + query.toString());
    },

    async fetchPropertyDetail(id) {
        return this.fetchJson(`/api/properties/${id}`);
    },

    async fetchPriceHistory(id) {
        return this.fetchJson(`/api/properties/${id}/price-history`);
    }
};
