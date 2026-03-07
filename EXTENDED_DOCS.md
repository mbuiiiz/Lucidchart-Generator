
# Lucidchart Generator – System & Architecture Documentation

## System Architecture: The Big Picture
This project uses a Full-Stack Web Application Architecture with two main layers:

1. Backend (Server)
• Technology: Java Spring Boot
• Location: aetherxmlbridge/
• What it does: Handles business logic, security, and API communications. It
  also servers Thymeleaf templates to the client.
2. Database
• Technology: PostgreSQL on Render
• What it does: Stores sensitive user information. Handles all CRUD operations.

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


## Useful Notes For Teammates

If certain Thymeleaf templates are not loading and you cannot locate the source of error, try:
1. Ensure that every opening tag has its corresponding closing tag
2. No failed imports in the html file
3. Your HTML files are not allowed to link to CSS files using ../static/css/styles.css, you   must use /css/styles.css as its file path. 
4. Thymeleaf parses our HTML files, which means that if there exists even one syntax error in our HTML file, we will get an error. 

For any big changes, please do NOT push your changes directly to the main branch, as one bad commit will take down our application that is hosted on Render. Please make a Pull Request, and we will all review it. 

Please Don't push your IDE Settings

---

