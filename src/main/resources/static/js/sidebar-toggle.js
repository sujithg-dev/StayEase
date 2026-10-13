/**
 * Adds a hamburger button + overlay so the fixed sidebar works as a drawer
 * on tablet/mobile. No changes to the topbar/sidebar fragments needed.
 * Requires manager-responsive.css (the drawer styles live there).
 */
(() => {
    const topbar = document.querySelector('.topbar');
    const sidebar = document.querySelector('.sidebar');
    if (!topbar || !sidebar) return;

    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'sidebar-toggle';
    btn.setAttribute('aria-label', 'Open menu');
    btn.setAttribute('aria-expanded', 'false');
    btn.innerHTML = '<span class="material-symbols-outlined">menu</span>';
    topbar.prepend(btn);

    const overlay = document.createElement('div');
    overlay.className = 'sidebar-overlay';
    document.body.appendChild(overlay);

    const icon = btn.querySelector('span');

    function setOpen(open) {
        document.body.classList.toggle('sidebar-open', open);
        btn.setAttribute('aria-expanded', String(open));
        btn.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');
        icon.textContent = open ? 'close' : 'menu';
    }

    btn.addEventListener('click', () => setOpen(!document.body.classList.contains('sidebar-open')));
    overlay.addEventListener('click', () => setOpen(false));
    document.addEventListener('keydown', e => { if (e.key === 'Escape') setOpen(false); });
    sidebar.addEventListener('click', e => { if (e.target.closest('a')) setOpen(false); });

    // Reset the drawer state when resizing back to desktop
    window.matchMedia('(min-width: 993px)').addEventListener('change', e => { if (e.matches) setOpen(false); });
})();