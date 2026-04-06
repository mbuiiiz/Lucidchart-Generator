# Web Application Name
Lucidchart Generator 

## Abstract
Lucidchart Generator is a browser-based web application that automatically converts structured project data from Zoho Creator into Lucidchart diagrams. The system retrieves organized project information from the user and sends the data to an AI service through an AI language model API. The AI then generates and returns a Lucidchart-compatible XML file. This reduces manual work and improves documentation consistency.

## What is Zoho / Zoho Creator, and how is it used?
Zoho Creator is a low-code application platform that allows businesses to build custom web apps and store structured business data. This structured business data often contains information about client projects, such as project name and description, scope of work, client information, workflows, and notes from calls or meetings. This implies that instead of storing such information in spreadsheets or a Word doc, there is a structured database in Zoho Creator, ensuring consistency and organization. Additionally, Zoho Creator provides a REST API, meaning our web application can send HTTP requests and get project data back.

## Understanding the Problem 
At Aether Automation, project information is stored in Zoho Creator in a structured format. However, this data is not directly usable for visualization purposes. Employees must manually analyze the project details and recreate them as diagrams in Lucidchart. As projects grow in complexity, this manual process becomes inefficient and increases the risk of inconsistencies between documentation and actual project structure. There is no automated integration between the two systems, and no AI-based solution exists that directly converts structured project data into diagrams.

## Solving the Problem
We are solving this problem by building a web application that uses AI to turn project data into Lucidchart diagrams. First, our system will extract and filter the project data from the Zoho API in an organized way. This formatted data will then be sent to an AI model, which will analyze the information clearly and return an XML file. This XML file can then be downloaded or uploaded into Lucidchart, where it automatically generates the corresponding system architecture diagram.

## Making Life Better
This project will reduce the time required to create Lucidchart diagrams. It also improves consistency because these diagrams are created from structured data using a standardized process. It makes day-to-day tasks a lot more efficient and allows tasks to be completed more quickly. 

## Target Audience 
The target audience for this application is employees at Aether Automation. This includes project managers, technical consultants, and developers who need architecture diagrams based on Zoho. 

## Scope of the Project
The application has one central feature: converting organized project information into a visual architecture diagram. However, this main feature consists of multiple substantial components that require separate development.

## User Workflow
User logs into your app
1. They see a list of projects pulled from Zoho (like a dropdown or dashboard)
2. They select a project they want to generate a diagram for
3. They click a "Generate Diagram" button
4. Your app fetches that project's full data from Zoho, sends it to the AI, gets XML back
5. User gets a download button for the XML file


## Main Features (Epics)
Each feature/epic represents a significant feature of the application and is suitable for assignment to individual team members. The overall scope is sufficient for a five-member group. However, each member is not set to one role, and is free to help develop other features when desired.

1. **Authentication (OAuth)** - Users log in securely via OAuth with their account. This handles session management.

2. **Zoho API Data Retrieval** - The application utilizes the Zoho Creator API and fetches the relevant project data for a selected project, and normalizes it into a clean, consistent format ready to be processed.

3. **AI Integration** - The cleaned project data is combined as input into a pre-written system prompt template and sent to an AI API(like OpenAI). The API then returns a structured response that will be utilized to build the XML file.

4. **XML Generation** - The AI's response gets converted into a properly formatted Lucidchart-compatible XML file.

5. **Frontend / UI** - The client side of the app, such as the login page, a dashboard to select or view projects, a button to trigger generation, and a way to preview or download the result. This ties everything together visually.

6. **CI/CD and deployment strategy** - To ensure high quality code and seamless collaboration among each member, we will implement a CI/CD pipeline. This eliminates repetitive manual testing and deployment.
