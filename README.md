# 🚀 JobForge: A Scalable Job Board RESTful API

## Overview
This personal practice project is a robust, professionally-architected backend built with **Spring Boot 3.5.5** and **Java**, designed to power a full-featured, scalable job board application.  

It is built with a focus on the **security**, **data integrity**, and **maintainability** required for professional, production environments.

The **business-rule-driven architecture** prioritizes industry best practices, including role-based access control, actor authentication and authorization and advanced transactional data handling. 

---

## ✨ Key Architectural Features
This project showcases expertise in robust backend development, relational data management & integrity as well as business rule driven design, all with security in mind:

### 🛡️ Security & Authorization
- **Token-Based Authentication (JWT):** API-wide support of secure JWT-based authentication with Refresh Token for a seamless, yet secure UX. Each request is validated through a custom `JwtAuthenticationFilter`, ensuring user identity and session integrity. Tokens are managed per user with automatic expiration, validation, and removal on sensitive events (e.g., password change, account soft-deletion/recovery).
- **Password hashing**: Featuring `BCrypt` for encrypted storage.
- **Role-Based Access Control (RBAC):** Access is governed based on defined user roles (e.g., ADMIN, EMPLOYER, CANDIDATE) and business logic.
- **Declarative Authorization:** API-wide authorization rules enforced using Spring Security's declarative `@PreAuthorize` annotations on the service layer. *(Fully implemented, Testing in progress)*

### 💾 Data Integrity & Persistence
- **Transactional Atomicity:** Multi-step database operations are wrapped in `@Transactional` to be executed as a single unit, ensuring **rollback safety and  data consistency**.
- **Resource Mapping (MapStruct) & Data Transfer Objects:** Clean, efficient, and type-safe mapping between JPA Entities and DTOs, **separating the internal domain model from the API contracts**.
- **JPA Dirty Checking:** Optimized database writes using JPA's automatic state change detection and commit efficiency.
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
- **Refined Data Management:** Secure creation and updates of User Profiles, Job postings and Company profiles. Candidates, Employers and Recruiters each have their own access rights and available functionality.
- **Workflow states**: Implemented lifecycle management for Jobs, Candidates, and Companies (e.g., draft → active → closed), enforcing logical and real-life state transitions.
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

## 🧪 API Testing
- **Postman**: Used to test endpoints, authentication flows, and workflows.
- Environment variables configured for easy management of endpoint URLs and JWT tokens.
  
---

## 💡 Why It Matters
JobForge demonstrates production-grade backend engineering practices — from secure authentication and transactional safety to modular architecture using a clear Repositoy-Service–Controller layering.
The design follows Domain-Driven Design to ensure clean separation of concerns, maintainability, and scalability.
It reflects my ability to build robust, real-world systems that prioritize **clarity, security, and long-term usability**.

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

