# AI Career Resume — Production Ready Platform

A full-stack enterprise web application built with **Java 17, Spring Boot 3, Spring Data JPA, Hibernate, Spring Security (JWT), MySQL, OpenPDF, and Vanilla HTML5/CSS3/JavaScript**.

---

## 🚀 Quick Start in IntelliJ IDEA

1. **Unzip the project**:
   Extract `ai-career-resume.zip` to your desired workspace folder.

2. **Open in IntelliJ IDEA**:
   - Open IntelliJ IDEA.
   - Click **File** -> **Open...**
   - Select the `ai-career-resume` folder containing `pom.xml`.
   - Choose **Open as Project**.
   - IntelliJ will automatically detect Maven, download all dependencies, and index the Java classes.

3. **MySQL Setup**:
   - Start your MySQL server.
   - Run in MySQL workbench or terminal:
     ```sql
     CREATE DATABASE ai_career_resume CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
     ```
   - In `src/main/resources/application.properties`, update your MySQL username and password:
     ```properties
     spring.datasource.username=root
     spring.datasource.password=your_password
     ```

4. **Run the Application**:
   - In IntelliJ IDEA, navigate to:
     `src/main/java/com/aicareerresume/AiCareerResumeApplication.java`
   - Click the green **Run** icon next to `main()`.
   - Or open terminal in IntelliJ and run:
     ```bash
     mvn spring-boot:run
     ```

5. **Access the App**:
   - Open your browser at: `http://localhost:8080`
   - Test Health: `http://localhost:8080/actuator/health`

---

## ✨ Features Included
- **Truth Protection AI**: Refines bullets without fabricating experience or technologies.
- **ATS Resume Analyzer**: PDF upload & real-time breakdown (0-100).
- **Job Description Matcher**: Side-by-side gap and keyword analysis.
- **AI Mock Interview Coach**: Voice/text response with evaluation rubrics.
- **Interactive PDF Resume Generator**: Vector ATS-friendly PDF streaming using OpenPDF.
- **Master Profile & Multi-Version Resume Management**: Generate role-specific resumes.
- **Career Roadmap & GitHub Analyzer**: Structured learning milestones and repository analysis.
