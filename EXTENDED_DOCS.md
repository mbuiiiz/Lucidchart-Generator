
# Lucidchart Generator – System & Architecture Documentation

## System Architecture: The Big Picture
This project uses a Full-Stack Web Application Architecture with three main layers:
1. Frontend (User Interface)
• Technology: React + Vite
• Location: frontend/ (Do not change the folder name)
• What it does: This is what users see and interact with in their web browser.
2. Backend (Server)
• Technology: Java Spring Boot
• Location: aetherxmlbridge/ (Do not change the folder name)
• What it does: Handles business logic, security, and API communications.
3. Database
• Technology: PostgreSQL
• What it does: Stores sensitive user information.

## Frontend Server (React + Vite)
What is React?
React is a JavaScript library for building user interfaces. It allows developers to create reusable UI components (buttons, forms, lists, etc.) that update efficiently when data changes.
What is Vite?
Vite is a modern build tool and development server. It:
• Runs a local development server.
• Provides Hot Module Replacement (HMR) for fast feedback while developing.
• Bundles and optimizes code for production builds.

## How Frontend and Backend Communicate
Development Environment:

Frontend runs on: http://localhost:5173 (Vite default)
Backend runs on: http://localhost:8080 (Spring Boot default)
Communication: Frontend makes HTTP requests to `/api/*` which are proxied to the backend during development.

Production Environment:

In production (Docker), both frontend and backend are served from the same server on port 8080.
Static frontend files (HTML, CSS, JS) are served by Spring Boot.
API requests go to `/api/*` endpoints.
Everything runs as a single application (one container, one port).

## Docker: Containerization & Deployment
To ensure that there are no dependency issues and differences during development, a Docker container is setup, so that there does not exist cases where the application works on Device A, but not Device B despite the same codebase. To set it up, you will need Docker or Docker Desktop downloaded on your local machine. 

How to use:
```bash
docker build -t aetherxmlbridge .
docker run -p 8080:8080 aetherxmlbridge
```

## Dockerfile Contents
There is a lot going on inside the Dockerfile, the only thing you need to understand is this line: 

  COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static

This embeds the built React frontend files inside our backend application. We do this because it allows us to manage only one server instead of two, which also implies that we only need to host on Render. Springboot serves both the static files (HTML/CSS/JS) and the API endpoints when data is needed. This allows us to use only one container, one port, and one deployment.


## GitHub Actions CI/CD – Automated Testing & Build
On each commit (and pull request), GitHub Actions runs automated tests and builds the application to ensure nothing is broken and that production builds succeed. This helps prevent broken code from reaching the `main` branch.

---

