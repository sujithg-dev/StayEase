(() => {
    const container = document.querySelector('.section-hotels');
    const filters = container?.querySelector('.form-hotel-filters');
    const btn = document.querySelector('.filter-toggle');

    if (!container || !filters) return;

    btn.addEventListener('click', () => {
        const open = filters.classList.toggle('open');
        btn.setAttribute('aria-expanded', String(open));
    });

    container.classList.add('filters-collapsible');
})();

