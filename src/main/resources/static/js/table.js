document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-table]').forEach(initServerTable);
});

function initServerTable(container) {
    const params = new URLSearchParams(window.location.search);
    const form = container.querySelector('form[data-table-form]');

    const go = (changes = {}) => {
        if (form) {
            [...form.elements].forEach(el => {
                if (!el.name) return;
                const v = el.value.trim();
                v ? params.set(el.name, v) : params.delete(el.name);
            });
        }
        Object.entries(changes).forEach(([k, v]) => (v == null ? params.delete(k) : params.set(k, v)));
        window.location.search = params.toString();
    };

    if (form) {
        // Search runs on Enter; dropdowns and dates apply instantly. Any filter change returns to page 1.
        form.addEventListener('submit', e => { e.preventDefault(); go({ page: 0 }); });
        form.querySelectorAll('select, input[type="date"]').forEach(el =>
            el.addEventListener('change', () => go({ page: 0 })));
        form.querySelector('.search-box .icon')?.addEventListener('click', () => go({ page: 0 }));
        form.querySelector('[data-reset]')?.addEventListener('click', () => {
            window.location.search = '';
        });
    }

    // Sorting: click header, toggle asc/desc
    container.querySelectorAll('th[data-sort]').forEach(th => {
        th.addEventListener('click', () => {
            const field = th.dataset.sort;
            const nextDir = params.get('sort') === field && params.get('dir') === 'asc' ? 'desc' : 'asc';
            go({ sort: field, dir: nextDir, page: 0 });
        });
    });

    // Pagination
    container.querySelectorAll('[data-page]').forEach(btn => {
        btn.addEventListener('click', () => { if (!btn.disabled) go({ page: btn.dataset.page }); });
    });

    // Page size
    container.querySelector('[data-page-size]')?.addEventListener('change', e =>
        go({ size: e.target.value, page: 0 }));
}