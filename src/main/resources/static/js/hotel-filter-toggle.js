/**
 * Hotel search page: adds a "Filters" button that opens/closes the filter
 * panel on tablet/mobile (the CSS only hides the panel once this script has
 * marked the page as collapsible, so without JS the filters stay visible).
 */
(() => {
    const container = document.querySelector('.hotels-container');
    const filters = container?.querySelector('.form-hotel-filters');
    if (!container || !filters) return;

    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'filter-toggle';
    btn.setAttribute('aria-expanded', 'false');
    btn.innerHTML =
        '<span class="filter-toggle-label"><span class="material-symbols-outlined">tune</span>Filters</span>' +
        '<span class="material-symbols-outlined chevron">expand_more</span>';

    btn.addEventListener('click', () => {
        const open = filters.classList.toggle('open');
        btn.setAttribute('aria-expanded', String(open));
    });

    filters.before(btn);
    container.classList.add('filters-collapsible');
})();

