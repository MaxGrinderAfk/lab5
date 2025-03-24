# Student CRUD Microservice

## Description
This project is a RESTful microservice designed for managing student entities. It supports full CRUD (Create, Read, Update, Delete) operations and follows a layered architecture, including:

- **Model**: Defines the structure of student entities.
- **DAO (Data Access Object)**: Handles in-memory data operations.
- **Service**: Implements the business logic.
- **Controller**: Exposes RESTful endpoints for external interaction.

The application is built using Spring Framework and Java.

---

## Features

- Create new student records
- Retrieve a list of all students
- Retrieve a specific student by ID
- Update existing student records
- Delete student records
- API documentation with Swagger
- Exception handling for robust error management
- Logging for better debugging and monitoring

---

## Technologies Used

- **Language**: Java 23
- **Framework**: Spring Boot
- **Database**: PostgreSQL
- **Build Tool**: Maven
- **API Documentation**: Swagger
- **Logging**: SLF4J with Logback

---

## Getting Started

### Prerequisites

- JDK 23 or later
- Maven 3.8 or later
- Git

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/MaxGrinderAfk/lab1.git
   cd lab1/src/main/java/idespring/lab1
   ```

2. Build the project using Maven:
   ```bash
   mvn clean install
   ```

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

---

## API Documentation with Swagger

This project integrates Swagger for API documentation. To access the Swagger UI, start the application and navigate to:

```
http://localhost:8080/swagger-ui/
```

Swagger provides an interactive interface to explore and test API endpoints.

---

## Exception Handling

The project implements centralized exception handling using `@ControllerAdvice`. Custom exception classes provide meaningful error messages and HTTP status codes. Common exceptions handled include:

- `StudentNotFoundException` (404 Not Found)
- `InvalidDataException` (400 Bad Request)
- `GlobalExceptionHandler` for generic error handling

---

## Logging

The application uses SLF4J with Logback for logging. Important events and errors are logged to assist with debugging and monitoring. Logs include:

- Request handling in controllers
- Errors and exceptions
- Database operations

To configure logging, modify `src/main/resources/logback.xml`.

---

## API Endpoints

| Method | Endpoint            | Description                       |
|--------|---------------------|-----------------------------------|
| GET    | `/students`         | Get all students                 |
| GET    | `/students/{id}`    | Get a student by ID              |
| POST   | `/students`         | Create a new student             |
| PUT    | `/students/{id}`    | Update an existing student       |
| DELETE | `/students/{id}`    | Delete a student by ID           |

---

## Example Request and Response

### Create Student
**Request:**
```json
POST /students
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "age": 22
}
```

**Response:**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "age": 22
}
```

### Retrieve Student by ID
**Request:**
```bash
GET /students/1
```

**Response:**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "age": 22
}
```

---

## Project Structure

```plaintext
src/main/java/idespring/lab1
├── model
│   └── Student.java
├── dao
│   └── StudentRepository.java
├── service
│   └── StudentService.java
├── controller
│   └── StudentController.java
├── exception
│   └── GlobalExceptionHandler.java
│   └── StudentNotFoundException.java
├── config
│   └── SwaggerConfig.java
```

- **Model**: Defines the `Student` class with fields like `id`, `name`, `email`, and `age`.
- **DAO**: Uses `StudentRepository` class for database operations.
- **Service**: Implements methods for business logic, such as creating, updating, and deleting students.
- **Controller**: Exposes endpoints for HTTP requests.
- **Exception**: Handles errors and exceptions centrally.
- **Config**: Configures Swagger for API documentation.

---

## Code Quality

This project is analyzed by SonarCloud for code quality and maintainability. Check the latest analysis [here](https://sonarcloud.io/summary/new_code?id=MaxGrinderAfk_lab1&branch=master).

---

## Running Tests

Run the unit tests with Maven:
```bash
mvn test
```

---

## Future Enhancements

- Add advanced filtering and search functionality.
- Implement pagination and sorting for large datasets.
- Add role-based authentication and authorization.

---

## License

This project is licensed under the MIT License. See the LICENSE file for details.

