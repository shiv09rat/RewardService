# RewardService

A Spring Boot RESTful API that calculates loyalty reward points for a retailer's customers based on their purchase history.

---

## Table of Contents

- [Business Rules](#business-rules)
- [Design Details](#design-details)
- [Technical Details](#technical-details)
- [API Details](#api-details)
- [Sample Data](#sample-data)
- [Running the Application](#running-the-application)
- [Running Tests](#running-tests)
- [Project Structure](#project-structure)

---

## Business Rules

A customer earns points based on each recorded purchase:

| Purchase Amount          | Points Earned                                      |
|--------------------------|----------------------------------------------------|
| ≤ $50                    | 0 points                                           |
| $50 – $100               | 1 point per dollar above $50                       |
| > $100                   | 2 points per dollar above $100 **plus** 50 points  |

**Example:**  
A $120 purchase = (2 × $20) + (1 × $50) = **90 points**

---

## Design Details

### Architecture

```
┌─────────────────────────────────────────┐
│             REST Controller             │  ← HTTP layer (input validation)
│          RewardController               │
└───────────────────┬─────────────────────┘
                    │
┌───────────────────▼─────────────────────┐
│           Service Layer                 │  ← Business logic
│       RewardCalculatorService           │
└───────────────────┬─────────────────────┘
                    │
┌───────────────────▼─────────────────────┐
│          Repository Layer               │  ← Data access (in-memory)
│         CustomerRepository             │
└─────────────────────────────────────────┘
```

### Key Design Decisions

- **Layered architecture** — Controller → Service → Repository cleanly separates concerns.
- **Configurable thresholds** — Reward thresholds (`$50`, `$100`) and point multipliers are externalised to `application.properties` so they can be changed without a code update.
- **Dynamic time window** — The `months` query parameter (default 3, max 24) allows any lookback period, making the 3-month scenario the default test case while keeping the API flexible.
- **Global exception handling** — A `@RestControllerAdvice` maps domain exceptions to consistent JSON error responses with appropriate HTTP status codes.
- **In-memory data store** — `CustomerRepository` seeds realistic data for 4 customers spanning 5 months. Swap in a JPA repository for a production database without touching the service layer.

---

## Technical Details

| Technology          | Version   |
|---------------------|-----------|
| Java                | 8         |
| Spring Boot         | 2.7.18    |
| Maven               | 3.x       |
| JUnit 5             | 5.x       |
| Mockito             | 4.x       |

### Logging

- Structured console logging via SLF4J / Logback.
- Log level for `com.retailer` package: `DEBUG` (configurable in `application.properties`).
- Key events logged: incoming requests, transactions found, points calculated, warnings/errors.

### Exception Handling

| Exception                          | HTTP Status | Scenario                                     |
|------------------------------------|-------------|----------------------------------------------|
| `CustomerNotFoundException`        | 404         | Requested customer ID does not exist          |
| `InvalidRequestException`          | 400         | `months` parameter out of range (1–24)        |
| `MissingServletRequestParameterException` | 400  | Required parameter absent                    |
| `MethodArgumentTypeMismatchException`    | 400   | Non-integer value passed for `months`        |
| `Exception` (catch-all)            | 500         | Unexpected server-side error                  |

### Input Validation

- `customerId` is a path variable; non-existent IDs return a structured 404 response.
- `months` must be an integer between 1 and 24 (inclusive); values outside this range return 400.
- Non-integer values for `months` return 400 with a descriptive message.

---

## API Details

### Base URL

```
http://localhost:8080
```

---

### `GET /api/rewards/{customerId}`

Calculates reward points for a single customer.

#### Path Parameters

| Parameter    | Type   | Required | Description                |
|--------------|--------|----------|----------------------------|
| `customerId` | String | Yes      | Unique customer identifier |

#### Query Parameters

| Parameter | Type | Required | Default | Description                             |
|-----------|------|----------|---------|-----------------------------------------|
| `months`  | int  | No       | `3`     | Number of months to look back (1–24)    |

#### Example Request

```
GET /api/rewards/C001?months=3
```

#### Example Response (200 OK)

```json
{
  "customerId": "C001",
  "customerName": "Alice Johnson",
  "email": "alice.johnson@example.com",
  "periodStart": "2024-01-01",
  "periodEnd": "2024-03-31",
  "monthsCalculated": 3,
  "monthlyRewards": [
    {
      "month": "JANUARY 2024",
      "totalPoints": 115,
      "transactionCount": 2,
      "totalSpent": 195.5,
      "transactions": [
        {
          "transactionId": "T001",
          "transactionDate": "2024-01-05",
          "amount": 120.0,
          "pointsEarned": 90
        },
        {
          "transactionId": "T002",
          "transactionDate": "2024-01-20",
          "amount": 75.5,
          "pointsEarned": 25
        }
      ]
    }
  ],
  "totalRewardPoints": 320,
  "totalTransactions": 6,
  "totalAmountSpent": 659.25
}
```

#### Error Responses

| Status | Scenario                          |
|--------|-----------------------------------|
| 400    | `months` out of range or invalid  |
| 404    | Customer not found                |
| 500    | Unexpected server error           |

---

### `GET /api/rewards`

Calculates reward points for **all** customers, sorted by total points (highest first).

#### Query Parameters

| Parameter | Type | Required | Default | Description                             |
|-----------|------|----------|---------|-----------------------------------------|
| `months`  | int  | No       | `3`     | Number of months to look back (1–24)    |

#### Example Request

```
GET /api/rewards?months=3
```

#### Example Response (200 OK)

Returns an array of the same `RewardResponse` structure shown above, one element per customer.

---

## Sample Data

Four customers are pre-loaded with transactions spanning January–May 2024:

| Customer ID | Name           | Email                        |
|-------------|----------------|------------------------------|
| C001        | Alice Johnson  | alice.johnson@example.com    |
| C002        | Bob Martinez   | bob.martinez@example.com     |
| C003        | Carol Smith    | carol.smith@example.com      |
| C004        | David Lee      | david.lee@example.com        |

To test with the default 3-month window, adjust `periodStart`/`periodEnd` or modify transaction dates in `CustomerRepository.java` to be within the last 3 months from today.

---

## Running the Application

### Prerequisites

- Java 8+
- Maven 3.x

### Steps

```bash
# Clone the repository
git clone <repo-url>
cd RewardService

# Build and run
mvn spring-boot:run
```

The server starts on port **8080**.

### Quick test with curl

```bash
# Single customer – 3 months (default)
curl http://localhost:8080/api/rewards/C001

# Single customer – 5 months
curl "http://localhost:8080/api/rewards/C001?months=5"

# All customers – 3 months
curl http://localhost:8080/api/rewards

# All customers – 1 month
curl "http://localhost:8080/api/rewards?months=1"
```

---

## Running Tests

```bash
mvn test
```

Test output and results are saved to `target/surefire-reports/`.

### Test Coverage

| Test Class                    | Scenarios Covered                                              |
|-------------------------------|----------------------------------------------------------------|
| `RewardCalculatorServiceTest` | Point calculation edge cases, monthly aggregation, exceptions, time filtering |
| `RewardControllerTest`        | HTTP status codes, JSON structure, default parameters, error responses |

---

## Project Structure

```
RewardService/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/retailer/rewardservice/
    │   │   ├── RewardServiceApplication.java       ← Entry point
    │   │   ├── controller/
    │   │   │   └── RewardController.java           ← REST endpoints
    │   │   ├── service/
    │   │   │   └── RewardCalculatorService.java    ← Business logic & point calculation
    │   │   ├── repository/
    │   │   │   └── CustomerRepository.java         ← In-memory data store
    │   │   ├── model/
    │   │   │   ├── Customer.java
    │   │   │   └── Transaction.java
    │   │   ├── dto/
    │   │   │   ├── RewardResponse.java
    │   │   │   ├── MonthlyRewardSummary.java
    │   │   │   ├── TransactionDetail.java
    │   │   │   └── ApiErrorResponse.java
    │   │   └── exception/
    │   │       ├── CustomerNotFoundException.java
    │   │       ├── InvalidRequestException.java
    │   │       └── GlobalExceptionHandler.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/retailer/rewardservice/
            ├── service/
            │   └── RewardCalculatorServiceTest.java
            └── controller/
                └── RewardControllerTest.java
```
