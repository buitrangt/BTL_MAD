# SmartExpense — MySQL Backend Integration Design

## Overview

Add a Spring Boot backend server with MySQL database to the SmartExpense Android app. The app will follow an **offline-first** architecture: Room (SQLite) remains for local storage, data syncs to MySQL via REST API when network is available.

## Architecture

```
[Android App (Kotlin)] <--REST API--> [Spring Boot Server] <---> [MySQL Database]
     (Mobile + Room)                    (Backend)                   (Data)
```

- **Android app**: Keeps Room for offline, adds Retrofit for API calls
- **Spring Boot**: Monolith backend exposing REST API
- **MySQL**: Centralized persistent storage

## Database Schema (MySQL)

Migration from current Room entity + new tables:

```sql
-- Users table (new)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Expenses table (migrated from Room + user_id)
CREATE TABLE expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    amount DOUBLE NOT NULL,
    category VARCHAR(255) NOT NULL,
    timestamp BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## REST API Endpoints

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login, returns JWT token |

### Expenses
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/expenses` | Get all expenses for authenticated user |
| POST | `/api/expenses` | Create new expense |
| DELETE | `/api/expenses/{id}` | Delete expense |
| POST | `/api/expenses/sync` | Batch sync from mobile |

### Statistics
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/stats/daily` | Total by day |
| GET | `/api/stats/weekly` | Total by week |
| GET | `/api/stats/monthly` | Total by month |
| GET | `/api/stats/by-category` | Total by category |

## Spring Boot Project Structure

```
com.smartexpense.server
├── config/              # SecurityConfig, JwtConfig
├── controller/          # AuthController, ExpenseController, StatsController
├── service/
│   ├── AuthService.kt              # Interface
│   ├── ExpenseService.kt           # Interface
│   ├── StatsService.kt             # Interface
│   └── impl/
│       ├── AuthServiceImpl.kt      # Implementation
│       ├── ExpenseServiceImpl.kt   # Implementation
│       └── StatsServiceImpl.kt     # Implementation
├── repository/          # UserRepository, ExpenseRepository (Spring Data JPA)
├── model/               # User, Expense (JPA Entity)
├── dto/                 # Request/Response DTOs
├── security/            # JwtFilter, JwtUtil
└── exception/           # GlobalExceptionHandler
```

**Tech stack:**
- Spring Boot 3.x
- Spring Security + JWT
- Spring Data JPA (Hibernate)
- MySQL Connector
- Lombok

## Android Changes

### New dependencies
- Retrofit + OkHttp (API calls)
- Gson/Moshi (JSON parsing)

### New files
```
utils/
├── api/
│   ├── ApiService.kt        # Retrofit interface
│   ├── RetrofitClient.kt    # Retrofit singleton
│   └── AuthInterceptor.kt   # Attach JWT to headers
├── sync/
│   └── SyncManager.kt       # Sync Room <-> MySQL
```

### New screens
- **LoginActivity** — Email/password login
- **RegisterActivity** — User registration

### Modifications
- Keep Room database as-is for offline storage
- Store JWT token in SharedPreferences/DataStore
- SyncManager handles data sync when network available

## Sync Strategy (Offline-First)

1. All writes go to Room first (immediate)
2. SyncManager detects network availability
3. On network available: batch sync unsynced records to server
4. On login from new device: pull all data from server to Room
