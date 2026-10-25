# StayEase --- Hotel Reservation Platform

StayEase is a hotel reservation web application built with **Spring
Boot, Thymeleaf, Spring Security, and MongoDB**. It provides separate
experiences for customers, hotel managers, and administrators.

> **Live demo:** https://stayease-b7ju.onrender.com/
> **Repository:** https://github.com/sujithg-dev/StayEase

## Features

### Customer

-   Register and sign in securely.
-   Browse and search active hotels.
-   Filter hotel results using supported search criteria such as location,
    rating, amenities, no of guests, and room price.
-   View hotel details, images, amenities, policies, and available
    rooms.
-   Create bookings with check-in/check-out dates and guest counts.
-   View bookings and cancel eligible confirmed bookings.
-   Submit reviews for eligible bookings.
-   Profile management and password-reset functionality

### Hotel manager

-   Access manager-only pages protected by role-based authorization.
-   Create and manage hotels and rooms associated with the manager.
-   Manage hotel descriptions, amenities, policies, images, and room
    details.
-   View bookings for managed hotels and cancel eligible bookings.
-   View manager dashboard statistics for the manager's own hotels and
    bookings.

### Administrator

-   Access administrator-only pages.
-   View user, manager, hotel, bookings information.
-   Manage user roles and hotel status.
-   View dashboard statistics across the application.

### Booking and availability rules

-   Booking validation checks hotel and room status, date ranges, guest
    capacity, and room availability.
-   Bookings use statuses such as `PENDING_PAYMENT`, `CONFIRMED`,
    `CANCELLED`, and `COMPLETED`.
-   Payment status is tracked separately.
-   Cancellation is restricted by booking status and check-in timing.
-   Completed bookings can be updated when the checkout date has passed.

## Technology stack

-   **Java 25**
-   **Spring Boot**
-   **Spring MVC**
-   **Spring Security**
-   **Spring Data MongoDB**
-   **MongoDB Atlas** for hosted database storage
-   **Thymeleaf** for server-rendered HTML
-   **HTML, CSS, and JavaScript** for the user interface
-   **Maven** for dependency management and builds
-   **JUnit / Spring Boot Test / Mockito** for automated tests
-   **Render** for deployment

## Application roles

  -----------------------------------------------------------------------
Role                                Purpose
  ----------------------------------- -----------------------------------
`CUSTOMER`                          Search hotels and manage personal
bookings and reviews

`MANAGER`                           Manage owned hotels, rooms, and
related bookings

`ADMIN`                             Manage users, roles, hotels, and
application-wide information
  -----------------------------------------------------------------------

Access is protected using Spring Security. Keep authorization checks in
the backend; hiding a UI link alone is not security.

## Prerequisites

Install the following before running the project locally:

-   Java 25
-   Maven (or use the Maven Wrapper if included)
-   A MongoDB database, such as a MongoDB Atlas cluster
-   Git

## Run locally

1.  Clone the repository:

    ``` bash
    git clone <your-repository-url>
    cd StayEase
    ```

2.  Configure the environment variables described below. Do not commit
    credentials or production secrets.

3.  Start the application using the Maven Wrapper, if included:

    **Windows**

    ``` powershell
    .\mvnw.cmd spring-boot:run
    ```

    **macOS / Linux**

    ``` bash
    ./mvnw spring-boot:run
    ```

    Alternatively, run:

    ``` bash
    mvn spring-boot:run
    ```

4.  Open `http://localhost:8080` unless your local server port is
    configured differently.

## Configuration

Use environment variables for environment-specific settings. A typical
production configuration is:

``` properties
server.port=${PORT:8080}
spring.data.mongodb.uri=${MONGODB_URI}
```

Configure these values in your local environment or deployment platform:

  -----------------------------------------------------------------------
Variable                            Description
  ----------------------------------- -----------------------------------
`MONGODB_URI`                       MongoDB connection URI for the
target database

`PORT`                              Port supplied by the hosting
platform; defaults to `8080`
locally

`APP_BASE_URL`                      Public application URL used to
construct links in emails, if wired
into the application

Email configuration                 Supply the variables expected by
the current `EmailService`
implementation
  -----------------------------------------------------------------------

Use the exact environment-variable names referenced by the code.
`APP_BASE_URL` is a recommended configuration name; the current email
service must read it for it to affect generated links.