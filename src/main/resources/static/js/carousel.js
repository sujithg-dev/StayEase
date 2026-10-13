document.addEventListener('DOMContentLoaded', function () {
    let active = null; // the carousel currently open

    document.querySelectorAll('.carousel-modal[data-carousel]').forEach(modal => {
        const key = modal.dataset.carousel;
        const slides = modal.querySelectorAll('.carousel-slide');
        const thumbs = modal.querySelectorAll('.carousel-thumb');
        const counter = modal.querySelector('.carousel-current');
        let current = 0;

        function show(index) {
            current = (index + slides.length) % slides.length;
            slides.forEach((s, i) => s.classList.toggle('active', i === current));
            thumbs.forEach((t, i) => t.classList.toggle('active', i === current));
            counter.textContent = current + 1;
            thumbs[current].scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
        }

        function open(index) {
            modal.classList.add('open');
            modal.setAttribute('aria-hidden', 'false');
            document.body.style.overflow = 'hidden';
            show(index);
            active = { show, close, getCurrent: () => current };
        }

        function close() {
            modal.classList.remove('open');
            modal.setAttribute('aria-hidden', 'true');
            document.body.style.overflow = '';
            active = null;
        }

        // Triggers that belong to this modal
        document.querySelectorAll('[data-carousel-target="' + key + '"]').forEach(el => {
            el.style.cursor = 'pointer';
            el.addEventListener('click', () => open(parseInt(el.dataset.index, 10) || 0));
        });

        modal.querySelector('.carousel-prev').addEventListener('click', () => show(current - 1));
        modal.querySelector('.carousel-next').addEventListener('click', () => show(current + 1));
        modal.querySelector('.carousel-close').addEventListener('click', close);
        thumbs.forEach(t => t.addEventListener('click', () => show(parseInt(t.dataset.index, 10))));

        modal.addEventListener('click', e => {
            if (e.target === modal ||
                e.target.classList.contains('carousel-stage') ||
                e.target.classList.contains('carousel-slide')) close();
        });

        // Swipe
        let startX = 0;
        modal.addEventListener('touchstart', e => { startX = e.changedTouches[0].clientX; }, { passive: true });
        modal.addEventListener('touchend', e => {
            const diff = e.changedTouches[0].clientX - startX;
            if (Math.abs(diff) > 50) show(diff > 0 ? current - 1 : current + 1);
        }, { passive: true });
    });

    // Single keyboard listener for whichever carousel is open
    document.addEventListener('keydown', e => {
        if (!active) return;
        if (e.key === 'Escape') active.close();
        else if (e.key === 'ArrowLeft') active.show(active.getCurrent() - 1);
        else if (e.key === 'ArrowRight') active.show(active.getCurrent() + 1);
    });
});