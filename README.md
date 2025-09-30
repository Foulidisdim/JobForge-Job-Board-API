# 🚀 JobForge: A Scalable Job Board RESTful API

## Overview
This personal practice project is a robust, profesionally-architected backend engineered using **Spring Boot 3.5.5** and **Java** to power a full-featured job board application.  

It is built with a focus on the **security**, **data integrity**, and **maintainability** required for professional, production environments.

The architecture prioritizes industry best practices, including role-based access control, actor authorization and advanced transactional data handling.

---

## ✨ Key Architectural Features
This project showcases expertise in robust backend development, relational data management & integrity as well as business rule driven design, all with security in mind:

### 🛡️ Security & Authorization
- **Token-Based Authentication (JWT):** Design and structure ready on all services and security workflows to implement JSON Web Tokens (JWT) for safe user session management and validation. *(In progress)*
- **Password hashing**: Featuring BCrypt for encrypted storage.
- **Role-Based Access Control (RBAC):** Established a centralized `AuthorizationService` to govern access based on defined user roles (e.g., ADMIN, EMPLOYER, CANDIDATE) and business logic.
- **Declarative Authorization:** Strategic groundwork to further streamline the authorization rules and their enforcement using Spring Security's declarative `@PreAuthorize` annotations on the service layer. *(In progress)*

### 💾 Data Integrity & Persistence
- **Transactional Atomicity:** Used Spring's `@Transactional` to ensure multi-step database operations are executed as a single unit, rolling back if any operation fails, guaranteeing data consistency.
- **Resource Mapping (MapStruct) & Data Transfer Objects:** Clean, fast, and type-safe mapping between JPA Entities and DTOs, **separating the internal domain model from the API contracts**.
- **JPA Dirty Checking:** Optimized database writes using JPA's automatic change detection and commit efficiency.
- **Soft Deletion:** Implemented `isDeleted` flags for core entities (Users, Jobs, Companies) to ensure auditability, relational integrity, account/data recovery and privacy law compliance.
- **Data Validation:** Enforced domain-specific rules (e.g., enums, currency) at the API layer with **custom annotations** `@EnumSubset` & `@ValidCurrency`, as well as SpringBoot's standard annotations like `@NotBlank`, `@Email` & `@CreatedDate` to ensure data integrity and consistent input.

### 🛠️ Error Handling
- **Global Exception Handler:** All exceptions are handled in a single place using `@ControllerAdvice`, ensuring uniform responses.
- **Custom Exceptions:** Domain-specific exceptions (e.g., `JobNotFoundException`, `UnauthorizedAccessException`) are defined to reflect meaningful business logic errors.
- **Standardized Error Response:** Clients receive a JSON response with the following fields:
  - `status`: HTTP status code
  - `message`: Detailed, customizable explanation
  - `timestamp`: The date and time of the error

### 💻 Core Business Functionality
- **Refined Data Management:** Secure creation and updates of User Profiles, Job postings and Company profiles. Candidates, Employers and Recruiters each have their own access rights.
- **Workflow states**: Implemented lifecycle management for Jobs, Candidates, and Companies (e.g., draft → active → closed), enforcing smooth and robust state transitions.
- **Search & Pagination:** Endpoints are architected to get further streamlined leveraging Spring Data's `Pageable` for scalable data retrieval, filtering, and sorting (e.g., searching jobs by title, location, or salary range).

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
- **Using IntelliJ IDEA: Open the project and run the `JobForgeApplication.java` file.**
The API will start on http://localhost:8080.

## License
This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).

