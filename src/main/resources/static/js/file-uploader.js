/**
 * Reusable file uploader. Works on any <input type="file">.
 *
 * Opt in with data-uploader. Optional settings (all data attributes):
 *   data-max-size="5"        max size per file in MB (default 5)
 *   data-max-files="10"      max number of files (default: unlimited)
 *   data-landscape="true"    images only: reject non-landscape images
 *   data-cover="true"        show a "Cover" badge on the first image
 *   data-label="images"      word used in the counter (default "files")
 * Allowed types come from the input's own `accept` attribute.
 * Add `multiple` to the input to allow more than one file.
 */

// Sample File Input
// <input class="input file-input" type="file" id="imageFiles" name="imageFiles" accept="image/jpeg,image/png,image/webp" multiple data-uploader data-landscape="true" data-cover="true" data-label="images">
// <input class="input file-input" type="file" id="roomFiles" name="roomFiles" accept="image/*" multiple data-uploader data-max-files="8" data-max-size="3" data-label="photos">
// <input class="input file-input" type="file" id="brochure" name="brochure" accept=".pdf" data-uploader data-label="file">

(() => {
    const isImage = f => f.type.startsWith('image/');

    // Check a file against the input's accept="" list (image/png, image/*, .pdf)
    const matchesAccept = (file, accept) => {
        if (!accept) return true;
        const name = file.name.toLowerCase();
        return accept.split(',').map(s => s.trim().toLowerCase()).filter(Boolean).some(a =>
            a.startsWith('.') ? name.endsWith(a)
                : a.endsWith('/*') ? file.type.startsWith(a.slice(0, -1))
                    : file.type === a);
    };

    // Turn accept="image/jpeg,image/png" into "JPEG, PNG"
    const acceptLabel = accept => accept
        ? accept.split(',').map(s => s.trim().replace(/^.*\//, '').replace(/^\./, '').replace('*', 'any').toUpperCase()).join(', ')
        : 'Any file';

    const isLandscape = file => new Promise(res => {
        const img = new Image(), url = URL.createObjectURL(file);
        img.onload = () => { URL.revokeObjectURL(url); res(img.width > img.height); };
        img.onerror = () => { URL.revokeObjectURL(url); res(false); };
        img.src = url;
    });

    function initFileUploader(input) {
        if (input.dataset.uploaderReady) return;
        input.dataset.uploaderReady = '1';

        const multiple = input.multiple;
        const maxSize = (parseFloat(input.dataset.maxSize) || 5) * 1024 * 1024;
        const maxFiles = multiple ? (parseInt(input.dataset.maxFiles, 10) || Infinity) : 1;
        const landscapeOnly = input.dataset.landscape === 'true';
        const showCover = input.dataset.cover === 'true';
        const label = input.dataset.label || 'files';
        const accept = input.accept;
        let files = [];

        if (!input.id) input.id = 'uploader-' + Math.random().toString(36).slice(2, 8);

        // Build UI around the existing input
        const drop = document.createElement('label');
        drop.className = 'file-drop';
        drop.htmlFor = input.id;
        const rules = [acceptLabel(accept), landscapeOnly && 'landscape', `max ${maxSize / 1048576} MB each`]
            .filter(Boolean).join(' · ');
        drop.innerHTML = `<div><strong>Click to upload</strong> or drag &amp; drop</div>
                          <div style="font-size:13px;color:#6b7280">${rules}</div>`;
        const counter = document.createElement('div'); counter.className = 'counter';
        const msg = document.createElement('div'); msg.className = 'msg'; msg.setAttribute('role', 'alert');
        const previews = document.createElement('div'); previews.className = 'previews';

        input.after(drop);
        const hint = input.closest('.input-field')?.querySelector('small');
        if (hint) { hint.before(counter); hint.after(msg, previews); }
        else drop.after(counter, msg, previews);

        const sync = () => {
            const dt = new DataTransfer();
            files.forEach(f => dt.items.add(f));
            input.files = dt.files;
            counter.textContent = maxFiles === Infinity
                ? `${files.length} ${label} selected`
                : `${files.length} / ${maxFiles} ${label}`;
        };

        async function add(list) {
            const errors = [];
            if (!multiple) files = []; // single input: new file replaces old one
            for (const f of list) {
                if (!matchesAccept(f, accept)) { errors.push(`${f.name}: file type not allowed.`); continue; }
                if (f.size > maxSize) { errors.push(`${f.name}: larger than ${maxSize / 1048576} MB.`); continue; }
                if (files.some(x => x.name === f.name && x.size === f.size)) continue;
                if (files.length >= maxFiles) { errors.push(`Maximum ${maxFiles} ${label} allowed.`); break; }
                if (landscapeOnly && isImage(f) && !(await isLandscape(f))) { errors.push(`${f.name}: not landscape.`); continue; }
                files.push(f);
            }
            msg.innerHTML = errors.join('<br>');
            sync(); render();
        }

        function render() {
            previews.innerHTML = '';
            files.forEach((f, i) => {
                const d = document.createElement('div'); d.className = 'thumb';
                let url = null;
                if (isImage(f)) {
                    url = URL.createObjectURL(f);
                    d.innerHTML = `<img src="${url}" alt="">`;
                } else {
                    d.classList.add('thumb-file');
                    d.textContent = f.name; // textContent avoids HTML injection from file names
                }
                if (showCover && i === 0 && isImage(f)) {
                    const c = document.createElement('span'); c.className = 'cover'; c.textContent = 'Cover';
                    d.appendChild(c);
                }
                const b = document.createElement('button');
                b.type = 'button'; b.textContent = '×'; b.setAttribute('aria-label', 'Remove ' + f.name);
                b.onclick = () => { if (url) URL.revokeObjectURL(url); files.splice(i, 1); sync(); render(); };
                d.appendChild(b);
                previews.appendChild(d);
            });
        }

        input.addEventListener('change', () => add([...input.files]));
        ['dragenter', 'dragover'].forEach(e => drop.addEventListener(e, ev => { ev.preventDefault(); drop.classList.add('dragover'); }));
        ['dragleave', 'drop'].forEach(e => drop.addEventListener(e, ev => { ev.preventDefault(); drop.classList.remove('dragover'); }));
        drop.addEventListener('drop', e => add([...e.dataTransfer.files]));

        // Keep the UI in step if the form is reset
        input.form?.addEventListener('reset', () => { files = []; msg.textContent = ''; sync(); render(); });

        sync();
    }

    window.initFileUploader = initFileUploader;
    document.querySelectorAll('input[type=file][data-uploader]').forEach(initFileUploader);
})();