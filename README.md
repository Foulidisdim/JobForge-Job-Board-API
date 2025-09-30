# 🚀 JobForge RESTful API: A Scalable Job Board Backend

## Overview
This project is a robust, cleanly-architected RESTful API backend engineered using **Spring Boot 3.5.5** and **Java** to power a full-featured job board application.  
It is built with a focus on **security**, **data integrity**, and **maintainability**, making it suitable for high-load, production environments.

The architecture prioritizes industry best practices, including token-based authentication, role-based access control, and advanced transactional data handling.

---

## ✨ Key Architectural Features
This project showcases expertise in high-quality backend development and data management:

### 🛡️ Security & Authorization
- **Token-Based Authentication (JWT):** Designed and structured the complete security workflow using JSON Web Tokens (JWT) for secure user session management and validation. *(In progress)*
- **Role-Based Access Control (RBAC):** Established a granular `AuthorizationService` to govern access based on defined user roles (e.g., ADMIN, EMPLOYER, CANDIDATE).
- **Declarative Authorization:** Strategy to utilize Spring Security's `@PreAuthorize` annotations on the service layer to enforce authorization rules. *(In progress)*

### 💾 Data Integrity & Persistence
- **Transactional Atomicity:** Leveraged Spring's `@Transactional` to ensure multi-step database operations are atomic, guaranteeing data consistency.
- **JPA Dirty Checking:** Optimized database writes using JPA's automatic change detection and commit efficiency.
- **Soft Deletion:** Implemented `isDeleted` flags for core entities (Users, Jobs, Companies) to ensure auditability, relational integrity, and account/data recovery.

### 💻 Core Business Functionality
- **User Management:** Secure creation and management of User Profiles (Candidates and Employers).
- **Resource Mapping (MapStruct):** Clean, fast, and type-safe mapping between JPA Entities and DTOs, separating the internal domain model from the API contracts.
- **Search & Pagination (Planned):** Endpoints will support Spring Data `Pageable` for scalable data retrieval, filtering, and sorting (e.g., searching jobs by title, location, or salary range).

---

## 🛠️ Technology Stack

| Category      | Technology                | Purpose                                                |
|---------------|---------------------------|--------------------------------------------------------|
| Backend       | Java 17+                  | Core programming language.                             |
| Framework     | Spring Boot 3.5.5         | Application framework leveraging Jakarta EE standards. |
| Persistence   | Spring Data JPA / Hibernate | ORM layer for database interaction.                  |
| Database      | PostgreSQL                | Robust, reliable relational database.                 |
| Security      | Spring Security, JWT      | Authentication and Authorization management.          |
| Data Mapping  | MapStruct                 | Efficient DTO-Entity conversion.                   |

---

## ⚙️ Getting Started (Local Setup)

### Prerequisites
- **Java Development Kit (JDK) 17+**
- **Maven 3.6+**
- **PostgreSQL running locally**

### Steps

1. **Clone the Repository:**
```bash
  git clone https://github.com/Foulidisdim/JobForge-Job-Board-API
  cd JobForge-Job-Board-API
```
2. **Configure PostgreSQL:**

- **Create a PostgreSQL database named jobboard_db.**

- **Update database connection properties in `src/main/resources/application.properties` or `application.yml` (shown here):**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/jobboard_db
    username: your_user
    password: your_password
  jpa:
    hibernate:
      ddl-auto: create
    show-sql: true
```
3. **Run the Application:**

- **Using Maven:**
```bash
./mvnw spring-boot:run
```
- **Using IntelliJ IDEA: Open the project and run the `JobboardApplication.java` file.**
The API will start on http://localhost:8080.

## License
This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).

