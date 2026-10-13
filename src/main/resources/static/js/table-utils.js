document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-table]').forEach(initTable);
});

function initTable(container) {
    const table = container.querySelector('table');
    if (!table) return;

    const tbody = table.tBodies[0];
    const headers = [...table.tHead.rows[0].cells];
    const rows = [...tbody.rows];
    const searchInput = container.querySelector('.search-box input');
    const filtersBox = container.querySelector('.table-filters');

    const state = { query: '', filters: {}, sortIndex: null, sortDir: 'asc' };

    const rawValue = (row, i) => {
        const cell = row.cells[i];
        return (cell.dataset.value ?? cell.textContent).trim();
    };
    const label = (row, i) => row.cells[i].textContent.trim().replace(/\s+/g, ' ');

    // "No results" row
    const noResults = document.createElement('tr');
    noResults.className = 'no-results-row';
    noResults.hidden = true;
    noResults.innerHTML = `<td colspan="${headers.length}" style="text-align:center;padding:24px;">No matching records found</td>`;
    tbody.appendChild(noResults);

    // ---------- Filters (dropdowns) ----------
    const filterSelects = [];
    headers.forEach((th, i) => {
        if (!th.hasAttribute('data-filter')) return;

        const options = new Map(); // value -> label
        rows.forEach(r => {
            const v = rawValue(r, i);
            if (v && !options.has(v)) options.set(v, label(r, i));
        });

        const select = document.createElement('select');
        select.className = 'filter-select';
        select.innerHTML = `<option value="">All ${th.textContent.trim()}</option>` +
            [...options].sort((a, b) => a[1].localeCompare(b[1]))
                .map(([v, l]) => `<option value="${escapeAttr(v)}">${escapeHtml(l)}</option>`).join('');

        select.addEventListener('change', () => {
            select.value ? (state.filters[i] = select.value) : delete state.filters[i];
            render();
        });

        filtersBox.appendChild(select);
        filterSelects.push(select);
    });

    if (filterSelects.length) {
        const reset = document.createElement('button');
        reset.type = 'button';
        reset.className = 'filter-reset';
        reset.textContent = 'Reset';
        reset.addEventListener('click', () => {
            state.query = ''; state.filters = {}; state.sortIndex = null;
            if (searchInput) searchInput.value = '';
            filterSelects.forEach(s => (s.value = ''));
            headers.forEach(h => h.removeAttribute('aria-sort'));
            render();
        });
        filtersBox.appendChild(reset);
    }

    // ---------- Search ----------
    if (searchInput) {
        let t;
        searchInput.addEventListener('input', () => {
            clearTimeout(t);
            t = setTimeout(() => { state.query = searchInput.value.trim().toLowerCase(); render(); }, 150);
        });
    }

    // ---------- Sorting ----------
    headers.forEach((th, i) => {
        const type = th.dataset.sort;
        if (!type) return;
        th.classList.add('sortable');
        th.addEventListener('click', () => {
            state.sortDir = state.sortIndex === i && state.sortDir === 'asc' ? 'desc' : 'asc';
            state.sortIndex = i;
            headers.forEach(h => h.removeAttribute('aria-sort'));
            th.setAttribute('aria-sort', state.sortDir === 'asc' ? 'ascending' : 'descending');
            render();
        });
    });

    const compare = (a, b, type) => {
        if (type === 'number') return (parseFloat(a.replace(/[^\d.-]/g, '')) || 0) - (parseFloat(b.replace(/[^\d.-]/g, '')) || 0);
        if (type === 'date') return (Date.parse(a) || 0) - (Date.parse(b) || 0);
        return a.localeCompare(b, undefined, { sensitivity: 'base', numeric: true });
    };

    // ---------- Render ----------
    function render() {
        let visible = rows.filter(row => {
            const matchesSearch = !state.query || row.textContent.toLowerCase().includes(state.query);
            const matchesFilters = Object.entries(state.filters).every(([i, v]) => rawValue(row, +i) === v);
            return matchesSearch && matchesFilters;
        });

        if (state.sortIndex !== null) {
            const type = headers[state.sortIndex].dataset.sort;
            const dir = state.sortDir === 'asc' ? 1 : -1;
            visible.sort((a, b) => dir * compare(rawValue(a, state.sortIndex), rawValue(b, state.sortIndex), type));
        }

        rows.forEach(r => (r.hidden = true));
        visible.forEach(r => { r.hidden = false; tbody.insertBefore(r, noResults); });
        noResults.hidden = visible.length > 0;
    }
}

function escapeHtml(s) {
    return s.replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function escapeAttr(s) { return escapeHtml(s); }