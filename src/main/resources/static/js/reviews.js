document.addEventListener("DOMContentLoaded", function ()
{
    const carousel = document.querySelector(".reviews-carousel");

    if (!carousel)
    {
        return;
    }

    const viewport = carousel.querySelector(".reviews-viewport");
    const track = carousel.querySelector(".hotel-review");

    const cards = Array.from(track.querySelectorAll(".review-card"));
    const previousButton =carousel.querySelector(".btn-prev");
    const nextButton =carousel.querySelector(".btn-next");
    const dotsContainer =document.querySelector(".review-dots");

    if (cards.length === 0)
    {
        carousel.style.display = "none";

        if (dotsContainer)
        {
            dotsContainer.style.display = "none";
        }

        return;
    }

    const AUTO_SLIDE_TIME = 4000;

    let currentIndex = 0;
    let cardsPerView = getCardsPerView();
    let totalSlides = getTotalSlides();
    let autoSlide;

    function getCardsPerView()
    {

        if (window.innerWidth <= 600)
        {
            return 1;
        }
        else if (window.innerWidth <= 992)
        {
            return 2;
        }
        else
        {
            return 3;
        }
    }

    function getTotalSlides()
    {
        return Math.max(1,cards.length - cardsPerView + 1);
    }

    function createDots()
    {
        if (!dotsContainer)
        {
            return;
        }

        dotsContainer.innerHTML = "";

        for (let i = 0; i < totalSlides; i++)
        {
            const dot =document.createElement("button");

            dot.type = "button";
            dot.classList.add("review-dot");
            dot.setAttribute("aria-label", `Go to review slide ${i + 1}`);

            dot.addEventListener("click", function ()
            {
                goToSlide(i);
                restartAutoSlide();
            });
            
            dotsContainer.appendChild(dot);
        }
    }

    function updateDots()
    {
        if (!dotsContainer)
        {
            return;
        }

        const dots =dotsContainer.querySelectorAll(".review-dot");

        dots.forEach(function (dot, index)
        {
            dot.classList.toggle( "active",index === currentIndex);
        });
    }

    function getSlideWidth()
    {
        const card = cards[0];

        if (!card)
        {
            return 0;
        }

        const cardWidth = card.getBoundingClientRect().width;
        const trackStyle = window.getComputedStyle(track);
        const gap = parseFloat(trackStyle.gap) || 0;

        return cardWidth + gap;
    }

    function updateCarousel()
    {
        const slideWidth = getSlideWidth();
        const translateX = currentIndex * slideWidth;

        track.style.transform = `translateX(-${translateX}px)`;

        updateDots();
    }

    function goToSlide(index)
    {

        if (totalSlides <= 1)
        {
            currentIndex = 0;
        }
        else
        {
            currentIndex = index;

            if (currentIndex >= totalSlides)
            {
                currentIndex = 0;
            }
            if (currentIndex < 0)
            {
                currentIndex = - 1;
            }
        }

        updateCarousel();
    }

    function nextSlide()
    {
        goToSlide(currentIndex + 1);
        restartAutoSlide();
    }

    function previousSlide()
    {
        goToSlide(currentIndex - 1);
        restartAutoSlide();
    }

    nextButton.addEventListener("click", nextSlide);
    previousButton.addEventListener("click",  previousSlide);

    function startAutoSlide()
    {
        if (totalSlides <= 1)
        {
            return;
        }

        autoSlide = setInterval(function ()
        {
            goToSlide(currentIndex + 1);
        }, AUTO_SLIDE_TIME );
    }

    function restartAutoSlide()
    {
        clearInterval(autoSlide);
        startAutoSlide();
    }

    carousel.addEventListener("mouseenter", function () { clearInterval(autoSlide); });
    carousel.addEventListener("mouseleave", function () { startAutoSlide(); });

    let touchStartX = 0;
    let touchEndX = 0;

    viewport.addEventListener("touchstart", function (event)
    {
        touchStartX = event.touches[0].clientX;
        clearInterval(autoSlide);
    },
        {
            passive: true
        }
    );

    viewport.addEventListener("touchmove", function (event)
    {
        touchEndX = event.touches[0].clientX;
    },
        {
            passive: true
        }
    );

    viewport.addEventListener("touchend", function ()
    {
        const swipeDistance = touchStartX - touchEndX;
        
        if (swipeDistance > 50)
        {
            goToSlide(currentIndex + 1);
        }
        else if (swipeDistance < -50)
        {
            goToSlide(currentIndex - 1);
        }
        
        startAutoSlide();
    });

    let resizeTimer;

    window.addEventListener("resize", function ()
    {
        clearTimeout(resizeTimer);

        resizeTimer = setTimeout(function ()
        {
            const newCardsPerView = getCardsPerView();

            if (newCardsPerView !== cardsPerView)
            {
                cardsPerView = newCardsPerView;
                totalSlides = getTotalSlides();

                if (currentIndex >= totalSlides)
                {
                    currentIndex = totalSlides - 1;
                }
                
                createDots();
            }
            
            updateCarousel();
            restartAutoSlide();

        }, 200);
    });

    createDots();
    updateCarousel();
    startAutoSlide();

});
