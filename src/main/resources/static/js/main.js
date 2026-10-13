function togglePassword(inputId, button) {
    const input = document.getElementById(inputId);

    if (input.type === "password") {
        input.type = "text";
        button.innerHTML = '<span class="material-symbols-outlined">password_2_off</span>';
    }
    else {
        input.type = "password";
        button.innerHTML = '<span class="material-symbols-outlined">password_2</span>';
    }
}

function toggleDescription(button) {
    const description = button.previousElementSibling;

    description.classList.toggle("expanded");

    if (description.classList.contains("expanded")) {
        button.textContent = "Show Less";
    }
    else {
        button.textContent = "... More";
    }
}

function toggleDropdown(id) {
    const dropdown = document.getElementById(id);

    if (dropdown.style.display === "none") {
        dropdown.style.display = "flex";
    } else {
        dropdown.style.display = "none";
    }
}


function changeSort() {
    const sortBy = document.getElementById("sortBy").value;

    const url = new URL(window.location.href);

    url.searchParams.set("sortBy", sortBy);

    // Sorting should return to page 1
    url.searchParams.set("page", "0");

    window.location.href = url.toString();
}

document.addEventListener("DOMContentLoaded", function ()
{
    const form = document.getElementById("hotelSearchForm");

    if (!form)
    {
        return;
    }

    form.querySelectorAll('input[type="checkbox"], input[type="radio"]').forEach(input =>
    {
        input.addEventListener("change", function ()
        {
            form.submit();
        });
    });

    let timeout;

    form.querySelectorAll('input[type="number"]').forEach(input =>
    {
        input.addEventListener("input", function ()
        {
            clearTimeout(timeout);

            timeout = setTimeout(function ()
            {
                form.submit();

            }, 600);
        });
    });

    const locationInput = document.getElementById("inLocation");

    if (locationInput)
    {
        locationInput.addEventListener("input", function ()
        {
            clearTimeout(timeout);

            timeout = setTimeout(function ()
            {
                form.submit();

            }, 700);
        });
    }

});




document.querySelectorAll('.hotel-nav .nav-item').forEach(tab => {

    tab.addEventListener('click', function (event) {

        event.preventDefault();

        document.querySelectorAll('.hotel-nav .nav-item').forEach(item => item.classList.remove('active'));

        this.classList.add('active');

        const target = document.querySelector(this.getAttribute('href'));

        if (target) {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    });
});


const fileInput = document.getElementById('file-hidden');
const placeholderText = document.getElementById('placeholder-text');

fileInput.addEventListener('change', (e) => {
    if (e.target.files.length > 0) {
        // Show the selected filename instead of the placeholder
        placeholderText.textContent = e.target.files[0].name;
        placeholderText.className = "file-name-active";
    } else {
        // Fallback if they clear it
        placeholderText.textContent = "Choose a file to upload...";
        placeholderText.className = "placeholder";
    }
});
