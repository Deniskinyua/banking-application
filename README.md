#  Banking Backend Application

A robust Spring Boot application designed to handle core banking functionalities, specifically focusing on secure and efficient fund transfers between user accounts. It incorporates asynchronous notification processing via Azure Service Bus for decoupled communication.

##  Features

* **Secure Fund Transfers:** Facilitates atomic fund transfers between sender and recipient accounts.

* **Account Validation:** Includes checks for sufficient balance, daily transaction limits, and prevention of self-transfers.

* **Transaction Recording:** Automatically records debit and credit transactions for both sender and recipient accounts.

* **Asynchronous Notifications:** Sends real-time transaction notifications via Azure Service Bus, ensuring decoupled and resilient communication.

* **RESTful API:** Provides a clear and well-documented API endpoint for initiating transfers.

* **Data Persistence:** Manages account and transaction data using Spring Data JPA with a relational database.

* **Error Handling:** Comprehensive global exception handling for consistent API responses.

##  Architecture Overview

This application follows a **layered architecture** (Controller, Service, Repository, Model) and leverages **event-driven patterns** for notifications.

* **API Layer (`TransactionController`):** Exposes RESTful endpoints for external interaction, handles request validation, and delegates business logic.

* **Service Layer (`TransactionService`, `NotificationService`):** Encapsulates core business rules, orchestrates transactions, and manages asynchronous notification sending.

* **Data Access Layer (`AccountRepository`):** Provides an abstraction for database operations using Spring Data JPA.

* **Domain Layer (`Account`, `Transaction`):** Core business entities mapped to the database.

* **Messaging Layer (`NotificationProcessor`, Azure Service Bus):** Handles asynchronous message queuing and consumption for notifications, ensuring the core financial transaction is not blocked by notification delivery.

For a detailed visual representation of the application's structure and flow, please refer to the following diagrams:

* [**Sequence Diagram (Fund Transfer)**](https://www.google.com/search?q=docs/sequence-diagram-fund-transfer.puml)

* [**Component Diagram**](https://www.google.com/search?q=docs/component-diagram.puml)

* [**State Diagram (Transaction Lifecycle)**](https://www.google.com/search?q=docs/state-diagram-transaction.puml)

## ️ Technologies Used

* **Backend:**

    * Java 17+

    * Spring Boot 3.x

    * Spring Data JPA

    * Lombok

    * Jakarta Bean Validation (for DTO validation)

    * SLF4J / Logback (for logging)

    * Jackson (for JSON serialization/deserialization)

* **Database:**

    * Relational Database (e.g., PostgreSQL, MySQL, H2 for development)

* **Messaging:**

    * Azure Service Bus (via Azure SDK for Java)

* **API Documentation:**

    * Springdoc OpenAPI (Swagger UI)

* **Testing:**

    * JUnit 5

    * Mockito

    * AssertJ

## Prerequisites

Before you can run this application, ensure you have the following installed:

* **Java Development Kit (JDK) 17 or higher**

* **Maven 3.x**

* **Git**

* **A Relational Database:** (e.g., PostgreSQL, MySQL). For local development, H2 (in-memory) can be used, configured in `application.properties`.

* **Azure Subscription & Service Bus Namespace:** You'll need an Azure account with a Service Bus Namespace and a Queue configured.

### Configure Environment Variables
Create an `application.properties (or application.yml)` file in `src/main/resources/` if it doesn't exist, and add your Azure Service Bus connection string and queue name:


>spring.cloud.azure.servicebus.connection-string=<YOUR_AZURE_SERVICE_BUS_CONNECTION_STRING>
azure.servicebus.queue-name=<YOUR_AZURE_SERVICE_BUS_QUEUE_NAME>
>spring.datasource.url=jdbc:postgresql://localhost:5432/banking_db
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password
spring.jpa.hibernate.ddl-auto=update # or create, create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
