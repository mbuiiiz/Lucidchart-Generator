
# Lucidchart Generator – System & Architecture Documentation

## System Architecture: The Big Picture
This project uses a Full-Stack Web Application Architecture with two main layers:

1. Backend (Server)
• Technology: Java Spring Boot
• Location: aetherxmlbridge/
• What it does: Handles business logic, security, and API communications. It
  also servers Thymeleaf templates to the client.
2. Database
• Technology: PostgreSQL
• What it does: Stores sensitive user information.

## Docker: Containerization & Deployment
Only use this when you are facing compatibility issues, such as: Application
runs on Teammate 1's Machine, but doesn't on Teammate 2's Machine. 

Explanation: To ensure that there are no dependency issues and differences during development, a Docker container is setup as backup. To set it up, you will need Docker or Docker Desktop downloaded on your local machine. This Docker image may take some time to build, and for each new feature, you will need to update your image to get the changes as well. 

How to use:
```bash
docker build -t aetherxmlbridge .
docker run -p 8080:8080 aetherxmlbridge
```

## GitHub Actions CI/CD – Automated Testing & Build
On each commit (and pull request), GitHub Actions runs automated tests and builds the application to ensure nothing is broken and that production builds succeed. This helps prevent broken code from reaching the `main` branch.

---

