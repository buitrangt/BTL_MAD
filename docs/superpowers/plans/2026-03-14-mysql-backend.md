# SmartExpense MySQL Backend Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Spring Boot backend with MySQL database to SmartExpense, enabling user authentication and cloud sync while keeping Room for offline use.

**Architecture:** Monolith Spring Boot 3.x server exposing REST API. Android app adds Retrofit to call API, keeps Room as local cache. JWT-based authentication. Offline-first: writes go to Room first, SyncManager pushes to server when online.

**Tech Stack:** Spring Boot 3.x, Spring Security, JWT, Spring Data JPA, MySQL, Retrofit 2, OkHttp, Gson

---

## File Structure

### Backend (new project: `smartexpense-server/`)

```
smartexpense-server/
├── pom.xml
├── src/main/resources/
│   └── application.properties
├── src/main/java/com/smartexpense/server/
│   ├── SmartExpenseServerApplication.java
│   ├── config/
│   │   └── SecurityConfig.java
│   ├── security/
│   │   ├── JwtUtil.java
│   │   ├── JwtFilter.java
│   │   └── CustomUserDetailsService.java
│   ├── model/
│   │   ├── User.java
│   │   └── Expense.java
│   ├── dto/
│   │   ├── AuthRequest.java
│   │   ├── AuthResponse.java
│   │   ├── ExpenseRequest.java
│   │   ├── ExpenseResponse.java
│   │   └── StatsResponse.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── ExpenseRepository.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── ExpenseService.java
│   │   ├── StatsService.java
│   │   └── impl/
│   │       ├── AuthServiceImpl.java
│   │       ├── ExpenseServiceImpl.java
│   │       └── StatsServiceImpl.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── ExpenseController.java
│   │   └── StatsController.java
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       └── ResourceNotFoundException.java
```

### Android (modify existing + new files)

```
app/src/main/java/com/arijit/budgettracker/
├── LoginActivity.kt                    # NEW
├── RegisterActivity.kt                 # NEW
├── MainActivity.kt                     # MODIFY - redirect to login if not authenticated
├── AddExpenseActivity.kt               # MODIFY - sync after insert
├── db/
│   ├── Expense.kt                      # MODIFY - add synced flag
│   └── ExpenseDatabase.kt             # MODIFY - migration to version 2
├── api/
│   ├── ApiService.kt                   # NEW - Retrofit interface
│   ├── RetrofitClient.kt              # NEW - Retrofit singleton
│   └── AuthInterceptor.kt            # NEW - JWT header interceptor
├── utils/
│   ├── TokenManager.kt                # NEW - JWT token storage
│   └── SyncManager.kt                # NEW - Room <-> MySQL sync
app/src/main/res/layout/
├── activity_login.xml                  # NEW
├── activity_register.xml               # NEW
```

---

## Chunk 1: Backend Project Setup + Database

### Task 1: Initialize Spring Boot project

**Files:**
- Create: `smartexpense-server/pom.xml`
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/SmartExpenseServerApplication.java`
- Create: `smartexpense-server/src/main/resources/application.properties`

- [ ] **Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.3</version>
        <relativePath/>
    </parent>
    <groupId>com.smartexpense</groupId>
    <artifactId>smartexpense-server</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>smartexpense-server</name>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.6</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create application.properties**

```properties
spring.application.name=smartexpense-server
server.port=8080

# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/smartexpense_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# JWT
jwt.secret=smartexpense-secret-key-change-in-production-must-be-at-least-256-bits-long
jwt.expiration=86400000
```

- [ ] **Step 3: Create main application class**

```java
package com.smartexpense.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartExpenseServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartExpenseServerApplication.class, args);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add smartexpense-server/
git commit -m "feat: initialize Spring Boot project with MySQL config"
```

---

### Task 2: Create JPA Entities

**Files:**
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/model/User.java`
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/model/Expense.java`

- [ ] **Step 1: Create User entity**

```java
package com.smartexpense.server.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String name;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 2: Create Expense entity**

Migration from Room entity: same fields (amount, category, timeStamp) + user_id FK.

```java
package com.smartexpense.server.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String category;

    @Column(name = "time_stamp", nullable = false)
    private Long timeStamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add smartexpense-server/src/main/java/com/smartexpense/server/model/
git commit -m "feat: add User and Expense JPA entities"
```

---

### Task 3: Create Repositories

**Files:**
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/repository/UserRepository.java`
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/repository/ExpenseRepository.java`

- [ ] **Step 1: Create UserRepository**

```java
package com.smartexpense.server.repository;

import com.smartexpense.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 2: Create ExpenseRepository**

```java
package com.smartexpense.server.repository;

import com.smartexpense.server.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserIdOrderByTimeStampDesc(Long userId);

    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId AND e.timeStamp >= :startTime AND e.timeStamp <= :endTime")
    List<Expense> findByUserIdAndTimeStampBetween(
        @Param("userId") Long userId,
        @Param("startTime") Long startTime,
        @Param("endTime") Long endTime
    );

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND e.timeStamp >= :startTime AND e.timeStamp <= :endTime GROUP BY e.category")
    List<Object[]> sumByCategory(
        @Param("userId") Long userId,
        @Param("startTime") Long startTime,
        @Param("endTime") Long endTime
    );
}
```

- [ ] **Step 3: Commit**

```bash
git add smartexpense-server/src/main/java/com/smartexpense/server/repository/
git commit -m "feat: add User and Expense repositories"
```

---

### Task 4: Create DTOs

**Files:**
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/dto/AuthRequest.java`
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/dto/AuthResponse.java`
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/dto/ExpenseRequest.java`
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/dto/ExpenseResponse.java`
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/dto/StatsResponse.java`

- [ ] **Step 1: Create Auth DTOs**

```java
// AuthRequest.java
package com.smartexpense.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    private String name; // only used for register
}
```

```java
// AuthResponse.java
package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private String name;
}
```

- [ ] **Step 2: Create Expense DTOs**

```java
// ExpenseRequest.java
package com.smartexpense.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExpenseRequest {
    @NotNull
    private Double amount;

    @NotBlank
    private String category;

    @NotNull
    private Long timeStamp;
}
```

```java
// ExpenseResponse.java
package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ExpenseResponse {
    private Long id;
    private Double amount;
    private String category;
    private Long timeStamp;
}
```

- [ ] **Step 3: Create StatsResponse**

```java
// StatsResponse.java
package com.smartexpense.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class StatsResponse {
    private Double totalAmount;
    private Map<String, Double> categoryBreakdown;
}
```

- [ ] **Step 4: Commit**

```bash
git add smartexpense-server/src/main/java/com/smartexpense/server/dto/
git commit -m "feat: add request/response DTOs"
```

---

## Chunk 2: Backend Security + Auth

### Task 5: Create JWT Utilities

**Files:**
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/security/JwtUtil.java`

- [ ] **Step 1: Create JwtUtil**

```java
package com.smartexpense.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add smartexpense-server/src/main/java/com/smartexpense/server/security/JwtUtil.java
git commit -m "feat: add JWT utility class"
```

---

### Task 6: Create Security Config + JWT Filter

**Files:**
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/security/CustomUserDetailsService.java`
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/security/JwtFilter.java`
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/config/SecurityConfig.java`

- [ ] **Step 1: Create CustomUserDetailsService**

```java
package com.smartexpense.server.security;

import com.smartexpense.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPasswordHash(), Collections.emptyList());
    }
}
```

- [ ] **Step 2: Create JwtFilter**

```java
package com.smartexpense.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.isTokenValid(token)) {
                String email = jwtUtil.extractEmail(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                var auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 3: Create SecurityConfig**

```java
package com.smartexpense.server.config;

import com.smartexpense.server.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add smartexpense-server/src/main/java/com/smartexpense/server/security/ smartexpense-server/src/main/java/com/smartexpense/server/config/
git commit -m "feat: add Spring Security config with JWT filter"
```

---

### Task 7: Create Auth Service + Controller

**Files:**
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/service/AuthService.java`
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/service/impl/AuthServiceImpl.java`
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/controller/AuthController.java`

- [ ] **Step 1: Create AuthService interface**

```java
package com.smartexpense.server.service;

import com.smartexpense.server.dto.AuthRequest;
import com.smartexpense.server.dto.AuthResponse;

public interface AuthService {
    AuthResponse register(AuthRequest request);
    AuthResponse login(AuthRequest request);
}
```

- [ ] **Step 2: Create AuthServiceImpl**

```java
package com.smartexpense.server.service.impl;

import com.smartexpense.server.dto.AuthRequest;
import com.smartexpense.server.dto.AuthResponse;
import com.smartexpense.server.model.User;
import com.smartexpense.server.repository.UserRepository;
import com.smartexpense.server.security.JwtUtil;
import com.smartexpense.server.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .build();
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getName());
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getName());
    }
}
```

- [ ] **Step 3: Create AuthController**

```java
package com.smartexpense.server.controller;

import com.smartexpense.server.dto.AuthRequest;
import com.smartexpense.server.dto.AuthResponse;
import com.smartexpense.server.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add smartexpense-server/src/main/java/com/smartexpense/server/service/ smartexpense-server/src/main/java/com/smartexpense/server/controller/AuthController.java
git commit -m "feat: add auth service and controller (register/login)"
```

---

## Chunk 3: Backend Expense + Stats APIs

### Task 8: Create Expense Service + Controller

**Files:**
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/service/ExpenseService.java`
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/service/impl/ExpenseServiceImpl.java`
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/controller/ExpenseController.java`

- [ ] **Step 1: Create ExpenseService interface**

```java
package com.smartexpense.server.service;

import com.smartexpense.server.dto.ExpenseRequest;
import com.smartexpense.server.dto.ExpenseResponse;
import java.util.List;

public interface ExpenseService {
    List<ExpenseResponse> getAllExpenses(String userEmail);
    ExpenseResponse createExpense(String userEmail, ExpenseRequest request);
    void deleteExpense(String userEmail, Long expenseId);
    List<ExpenseResponse> syncExpenses(String userEmail, List<ExpenseRequest> requests);
}
```

- [ ] **Step 2: Create ExpenseServiceImpl**

```java
package com.smartexpense.server.service.impl;

import com.smartexpense.server.dto.ExpenseRequest;
import com.smartexpense.server.dto.ExpenseResponse;
import com.smartexpense.server.model.Expense;
import com.smartexpense.server.model.User;
import com.smartexpense.server.repository.ExpenseRepository;
import com.smartexpense.server.repository.UserRepository;
import com.smartexpense.server.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @Override
    public List<ExpenseResponse> getAllExpenses(String userEmail) {
        User user = findUser(userEmail);
        return expenseRepository.findByUserIdOrderByTimeStampDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ExpenseResponse createExpense(String userEmail, ExpenseRequest request) {
        User user = findUser(userEmail);
        Expense expense = Expense.builder()
                .amount(request.getAmount())
                .category(request.getCategory())
                .timeStamp(request.getTimeStamp())
                .user(user)
                .build();
        return toResponse(expenseRepository.save(expense));
    }

    @Override
    public void deleteExpense(String userEmail, Long expenseId) {
        User user = findUser(userEmail);
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        if (!expense.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        expenseRepository.delete(expense);
    }

    @Override
    @Transactional
    public List<ExpenseResponse> syncExpenses(String userEmail, List<ExpenseRequest> requests) {
        User user = findUser(userEmail);
        List<ExpenseResponse> responses = new ArrayList<>();
        for (ExpenseRequest request : requests) {
            Expense expense = Expense.builder()
                    .amount(request.getAmount())
                    .category(request.getCategory())
                    .timeStamp(request.getTimeStamp())
                    .user(user)
                    .build();
            responses.add(toResponse(expenseRepository.save(expense)));
        }
        return responses;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private ExpenseResponse toResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .timeStamp(expense.getTimeStamp())
                .build();
    }
}
```

- [ ] **Step 3: Create ExpenseController**

```java
package com.smartexpense.server.controller;

import com.smartexpense.server.dto.ExpenseRequest;
import com.smartexpense.server.dto.ExpenseResponse;
import com.smartexpense.server.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getAllExpenses(Authentication auth) {
        return ResponseEntity.ok(expenseService.getAllExpenses(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            Authentication auth, @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.createExpense(auth.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(Authentication auth, @PathVariable Long id) {
        expenseService.deleteExpense(auth.getName(), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sync")
    public ResponseEntity<List<ExpenseResponse>> syncExpenses(
            Authentication auth, @Valid @RequestBody List<ExpenseRequest> requests) {
        return ResponseEntity.ok(expenseService.syncExpenses(auth.getName(), requests));
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add smartexpense-server/src/main/java/com/smartexpense/server/service/ExpenseService.java smartexpense-server/src/main/java/com/smartexpense/server/service/impl/ExpenseServiceImpl.java smartexpense-server/src/main/java/com/smartexpense/server/controller/ExpenseController.java
git commit -m "feat: add expense CRUD and sync endpoints"
```

---

### Task 9: Create Stats Service + Controller

**Files:**
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/service/StatsService.java`
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/service/impl/StatsServiceImpl.java`
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/controller/StatsController.java`

- [ ] **Step 1: Create StatsService interface**

```java
package com.smartexpense.server.service;

import com.smartexpense.server.dto.StatsResponse;

public interface StatsService {
    StatsResponse getDailyStats(String userEmail);
    StatsResponse getWeeklyStats(String userEmail);
    StatsResponse getMonthlyStats(String userEmail);
    StatsResponse getByCategoryStats(String userEmail);
}
```

- [ ] **Step 2: Create StatsServiceImpl**

```java
package com.smartexpense.server.service.impl;

import com.smartexpense.server.dto.StatsResponse;
import com.smartexpense.server.model.User;
import com.smartexpense.server.repository.ExpenseRepository;
import com.smartexpense.server.repository.UserRepository;
import com.smartexpense.server.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @Override
    public StatsResponse getDailyStats(String userEmail) {
        User user = findUser(userEmail);
        Calendar cal = getStartOfDay();
        long start = cal.getTimeInMillis();
        long end = System.currentTimeMillis();
        return buildStats(user.getId(), start, end);
    }

    @Override
    public StatsResponse getWeeklyStats(String userEmail) {
        User user = findUser(userEmail);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        long end = System.currentTimeMillis();
        return buildStats(user.getId(), start, end);
    }

    @Override
    public StatsResponse getMonthlyStats(String userEmail) {
        User user = findUser(userEmail);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        long end = System.currentTimeMillis();
        return buildStats(user.getId(), start, end);
    }

    @Override
    public StatsResponse getByCategoryStats(String userEmail) {
        User user = findUser(userEmail);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        long end = System.currentTimeMillis();
        return buildStats(user.getId(), start, end);
    }

    private StatsResponse buildStats(Long userId, long start, long end) {
        var expenses = expenseRepository.findByUserIdAndTimeStampBetween(userId, start, end);
        double total = expenses.stream().mapToDouble(e -> e.getAmount()).sum();

        Map<String, Double> categoryBreakdown = new LinkedHashMap<>();
        var categoryResults = expenseRepository.sumByCategory(userId, start, end);
        for (Object[] row : categoryResults) {
            categoryBreakdown.put((String) row[0], (Double) row[1]);
        }

        return StatsResponse.builder()
                .totalAmount(total)
                .categoryBreakdown(categoryBreakdown)
                .build();
    }

    private Calendar getStartOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
```

- [ ] **Step 3: Create StatsController**

```java
package com.smartexpense.server.controller;

import com.smartexpense.server.dto.StatsResponse;
import com.smartexpense.server.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @GetMapping("/daily")
    public ResponseEntity<StatsResponse> getDailyStats(Authentication auth) {
        return ResponseEntity.ok(statsService.getDailyStats(auth.getName()));
    }

    @GetMapping("/weekly")
    public ResponseEntity<StatsResponse> getWeeklyStats(Authentication auth) {
        return ResponseEntity.ok(statsService.getWeeklyStats(auth.getName()));
    }

    @GetMapping("/monthly")
    public ResponseEntity<StatsResponse> getMonthlyStats(Authentication auth) {
        return ResponseEntity.ok(statsService.getMonthlyStats(auth.getName()));
    }

    @GetMapping("/by-category")
    public ResponseEntity<StatsResponse> getByCategoryStats(Authentication auth) {
        return ResponseEntity.ok(statsService.getByCategoryStats(auth.getName()));
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add smartexpense-server/src/main/java/com/smartexpense/server/service/StatsService.java smartexpense-server/src/main/java/com/smartexpense/server/service/impl/StatsServiceImpl.java smartexpense-server/src/main/java/com/smartexpense/server/controller/StatsController.java
git commit -m "feat: add statistics endpoints (daily/weekly/monthly/by-category)"
```

---

### Task 10: Create Exception Handler

**Files:**
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/exception/ResourceNotFoundException.java`
- Create: `smartexpense-server/src/main/java/com/smartexpense/server/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Create ResourceNotFoundException**

```java
package com.smartexpense.server.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: Create GlobalExceptionHandler**

```java
package com.smartexpense.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
```

- [ ] **Step 3: Verify backend builds**

Run: `cd smartexpense-server && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add smartexpense-server/src/main/java/com/smartexpense/server/exception/
git commit -m "feat: add global exception handler"
```

---

## Chunk 4: Android — API Layer + Auth

### Task 11: Add Retrofit dependencies

**Files:**
- Modify: `app/build.gradle.kts:39-55`

- [ ] **Step 1: Add Retrofit, OkHttp, Gson dependencies to app/build.gradle.kts**

Add these lines after the existing dependencies block:

```kotlin
// Retrofit + OkHttp
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-gson:2.11.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
```

- [ ] **Step 2: Add internet permission to AndroidManifest.xml**

Add before existing permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

- [ ] **Step 3: Sync gradle and commit**

```bash
git add app/build.gradle.kts app/src/main/AndroidManifest.xml
git commit -m "feat: add Retrofit and networking dependencies"
```

---

### Task 12: Create API layer

**Files:**
- Create: `app/src/main/java/com/arijit/budgettracker/api/ApiService.kt`
- Create: `app/src/main/java/com/arijit/budgettracker/api/RetrofitClient.kt`
- Create: `app/src/main/java/com/arijit/budgettracker/api/AuthInterceptor.kt`
- Create: `app/src/main/java/com/arijit/budgettracker/utils/TokenManager.kt`

- [ ] **Step 1: Create TokenManager**

```kotlin
package com.arijit.budgettracker.utils

import android.content.Context

object TokenManager {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_NAME = "user_name"

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)
    }

    fun saveUser(context: Context, email: String, name: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, name)
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return getToken(context) != null
    }

    fun logout(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}
```

- [ ] **Step 2: Create AuthInterceptor**

```kotlin
package com.arijit.budgettracker.api

import android.content.Context
import com.arijit.budgettracker.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
        TokenManager.getToken(context)?.let { token ->
            request.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(request.build())
    }
}
```

- [ ] **Step 3: Create ApiService**

```kotlin
package com.arijit.budgettracker.api

import retrofit2.Response
import retrofit2.http.*

// DTOs
data class AuthRequest(val email: String, val password: String, val name: String? = null)
data class AuthResponse(val token: String, val email: String, val name: String?)
data class ExpenseRequest(val amount: Double, val category: String, val timeStamp: Long)
data class ExpenseResponse(val id: Long, val amount: Double, val category: String, val timeStamp: Long)
data class StatsResponse(val totalAmount: Double, val categoryBreakdown: Map<String, Double>?)

interface ApiService {
    // Auth
    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    // Expenses
    @GET("api/expenses")
    suspend fun getAllExpenses(): Response<List<ExpenseResponse>>

    @POST("api/expenses")
    suspend fun createExpense(@Body request: ExpenseRequest): Response<ExpenseResponse>

    @DELETE("api/expenses/{id}")
    suspend fun deleteExpense(@PathVariable("id") id: Long): Response<Void>

    @POST("api/expenses/sync")
    suspend fun syncExpenses(@Body requests: List<ExpenseRequest>): Response<List<ExpenseResponse>>

    // Stats
    @GET("api/stats/daily")
    suspend fun getDailyStats(): Response<StatsResponse>

    @GET("api/stats/weekly")
    suspend fun getWeeklyStats(): Response<StatsResponse>

    @GET("api/stats/monthly")
    suspend fun getMonthlyStats(): Response<StatsResponse>

    @GET("api/stats/by-category")
    suspend fun getByCategoryStats(): Response<StatsResponse>
}
```

- [ ] **Step 4: Create RetrofitClient**

```kotlin
package com.arijit.budgettracker.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Change this to your server IP/URL
    private const val BASE_URL = "http://10.0.2.2:8080/"  // Android emulator -> localhost

    private var apiService: ApiService? = null

    fun getApiService(context: Context): ApiService {
        if (apiService == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(context.applicationContext))
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(ApiService::class.java)
        }
        return apiService!!
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arijit/budgettracker/api/ app/src/main/java/com/arijit/budgettracker/utils/TokenManager.kt
git commit -m "feat: add Retrofit API layer with JWT interceptor"
```

---

### Task 13: Create Login and Register screens

**Files:**
- Create: `app/src/main/res/layout/activity_login.xml`
- Create: `app/src/main/res/layout/activity_register.xml`
- Create: `app/src/main/java/com/arijit/budgettracker/LoginActivity.kt`
- Create: `app/src/main/java/com/arijit/budgettracker/RegisterActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create activity_login.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="32dp"
    android:background="@color/white">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="SmartExpense"
        android:textSize="28sp"
        android:textStyle="bold"
        android:layout_marginBottom="48dp" />

    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/et_email"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Email"
            android:inputType="textEmailAddress" />
    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="24dp">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/et_password"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Password"
            android:inputType="textPassword" />
    </com.google.android.material.textfield.TextInputLayout>

    <TextView
        android:id="@+id/tv_error"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="@android:color/holo_red_dark"
        android:visibility="gone"
        android:layout_marginBottom="16dp" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btn_login"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Login"
        android:layout_marginBottom="16dp" />

    <TextView
        android:id="@+id/tv_register"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Don't have an account? Register"
        android:textColor="@color/design_default_color_primary" />
</LinearLayout>
```

- [ ] **Step 2: Create activity_register.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="32dp"
    android:background="@color/white">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Create Account"
        android:textSize="28sp"
        android:textStyle="bold"
        android:layout_marginBottom="48dp" />

    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/et_name"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Name"
            android:inputType="textPersonName" />
    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/et_email"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Email"
            android:inputType="textEmailAddress" />
    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="24dp">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/et_password"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Password"
            android:inputType="textPassword" />
    </com.google.android.material.textfield.TextInputLayout>

    <TextView
        android:id="@+id/tv_error"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="@android:color/holo_red_dark"
        android:visibility="gone"
        android:layout_marginBottom="16dp" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btn_register"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Register"
        android:layout_marginBottom="16dp" />

    <TextView
        android:id="@+id/tv_login"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Already have an account? Login"
        android:textColor="@color/design_default_color_primary" />
</LinearLayout>
```

- [ ] **Step 3: Create LoginActivity.kt**

```kotlin
package com.arijit.budgettracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.arijit.budgettracker.api.AuthRequest
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.utils.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Skip login if already authenticated
        if (TokenManager.isLoggedIn(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val btnLogin = findViewById<MaterialButton>(R.id.btn_login)
        val tvError = findViewById<TextView>(R.id.tv_error)
        val tvRegister = findViewById<TextView>(R.id.tv_register)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                tvError.text = "Please fill all fields"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.getApiService(this@LoginActivity)
                        .login(AuthRequest(email, password))

                    if (response.isSuccessful && response.body() != null) {
                        val authResponse = response.body()!!
                        TokenManager.saveToken(this@LoginActivity, authResponse.token)
                        TokenManager.saveUser(this@LoginActivity, authResponse.email, authResponse.name)
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        tvError.text = "Invalid email or password"
                        tvError.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    tvError.text = "Connection error. Using offline mode."
                    tvError.visibility = View.VISIBLE
                    // Allow offline access
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
                btnLogin.isEnabled = true
            }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
```

- [ ] **Step 4: Create RegisterActivity.kt**

```kotlin
package com.arijit.budgettracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.arijit.budgettracker.api.AuthRequest
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.utils.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etName = findViewById<TextInputEditText>(R.id.et_name)
        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val btnRegister = findViewById<MaterialButton>(R.id.btn_register)
        val tvError = findViewById<TextView>(R.id.tv_error)
        val tvLogin = findViewById<TextView>(R.id.tv_login)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                tvError.text = "Please fill email and password"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.getApiService(this@RegisterActivity)
                        .register(AuthRequest(email, password, name))

                    if (response.isSuccessful && response.body() != null) {
                        val authResponse = response.body()!!
                        TokenManager.saveToken(this@RegisterActivity, authResponse.token)
                        TokenManager.saveUser(this@RegisterActivity, authResponse.email, authResponse.name)
                        startActivity(Intent(this@RegisterActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
                        finish()
                    } else {
                        tvError.text = "Registration failed. Email may already exist."
                        tvError.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    tvError.text = "Connection error: ${e.message}"
                    tvError.visibility = View.VISIBLE
                }
                btnRegister.isEnabled = true
            }
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }
}
```

- [ ] **Step 5: Update AndroidManifest.xml — add activities, change launcher**

Change launcher activity from `MainActivity` to `LoginActivity`. Add `LoginActivity` and `RegisterActivity`:

```xml
<!-- Change MainActivity: remove intent-filter, add exported="false" -->
<activity android:name=".MainActivity" android:exported="false" />

<!-- Add LoginActivity as launcher -->
<activity android:name=".LoginActivity" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<!-- Add RegisterActivity -->
<activity android:name=".RegisterActivity" android:exported="false" />
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/arijit/budgettracker/LoginActivity.kt app/src/main/java/com/arijit/budgettracker/RegisterActivity.kt app/src/main/res/layout/activity_login.xml app/src/main/res/layout/activity_register.xml app/src/main/AndroidManifest.xml
git commit -m "feat: add login and register screens"
```

---

## Chunk 5: Android — Sync + Integration

### Task 14: Add sync flag to Room Expense + migration

**Files:**
- Modify: `app/src/main/java/com/arijit/budgettracker/db/Expense.kt`
- Modify: `app/src/main/java/com/arijit/budgettracker/db/ExpenseDao.kt`
- Modify: `app/src/main/java/com/arijit/budgettracker/db/ExpenseDatabase.kt`

- [ ] **Step 1: Add synced flag to Expense entity**

```kotlin
// Expense.kt — add synced field
package com.arijit.budgettracker.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val category: String,
    val timeStamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
```

- [ ] **Step 2: Add sync queries to ExpenseDao**

Add to `ExpenseDao.kt`:

```kotlin
@Query("SELECT * FROM expenses WHERE synced = 0")
suspend fun getUnsyncedExpenses(): List<Expense>

@Query("UPDATE expenses SET synced = 1 WHERE id IN (:ids)")
suspend fun markAsSynced(ids: List<Int>)
```

- [ ] **Step 3: Add migration to ExpenseDatabase**

```kotlin
// ExpenseDatabase.kt — bump version to 2, add migration
package com.arijit.budgettracker.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Expense::class], version = 2, exportSchema = false)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN synced INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arijit/budgettracker/db/
git commit -m "feat: add synced flag to Expense with Room migration v2"
```

---

### Task 15: Create SyncManager

**Files:**
- Create: `app/src/main/java/com/arijit/budgettracker/utils/SyncManager.kt`

- [ ] **Step 1: Create SyncManager**

```kotlin
package com.arijit.budgettracker.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.arijit.budgettracker.api.ExpenseRequest
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.db.ExpenseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SyncManager {

    suspend fun syncIfOnline(context: Context) {
        if (!isOnline(context)) return
        if (!TokenManager.isLoggedIn(context)) return

        withContext(Dispatchers.IO) {
            try {
                val dao = ExpenseDatabase.getDatabase(context).expenseDao()
                val unsynced = dao.getUnsyncedExpenses()
                if (unsynced.isEmpty()) return@withContext

                val requests = unsynced.map { expense ->
                    ExpenseRequest(
                        amount = expense.amount,
                        category = expense.category,
                        timeStamp = expense.timeStamp
                    )
                }

                val response = RetrofitClient.getApiService(context).syncExpenses(requests)
                if (response.isSuccessful) {
                    dao.markAsSynced(unsynced.map { it.id })
                }
            } catch (e: Exception) {
                // Sync failed silently — will retry next time
                e.printStackTrace()
            }
        }
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/arijit/budgettracker/utils/SyncManager.kt
git commit -m "feat: add SyncManager for offline-first sync"
```

---

### Task 16: Integrate sync into existing flows

**Files:**
- Modify: `app/src/main/java/com/arijit/budgettracker/AddExpenseActivity.kt:84-93`
- Modify: `app/src/main/java/com/arijit/budgettracker/MainActivity.kt:28-91`
- Modify: `app/src/main/java/com/arijit/budgettracker/SettingsActivity.kt` (add logout button)

- [ ] **Step 1: Trigger sync after adding expense in AddExpenseActivity**

In `AddExpenseActivity.kt`, after `dao.insertExpense(expense)` (line 90), add:

```kotlin
SyncManager.syncIfOnline(applicationContext)
```

Add import: `import com.arijit.budgettracker.utils.SyncManager`

- [ ] **Step 2: Trigger sync on MainActivity launch**

In `MainActivity.kt`, at the end of `onCreate()`, add:

```kotlin
// Sync unsynced expenses when app opens
lifecycleScope.launch {
    SyncManager.syncIfOnline(applicationContext)
}
```

Add import: `import com.arijit.budgettracker.utils.SyncManager`

- [ ] **Step 3: Add logout to SettingsActivity**

Add a logout button in `activity_settings.xml` and handle click in `SettingsActivity.kt`:

```kotlin
// In SettingsActivity.onCreate(), add after the projects click listener:
val logout = findViewById<CardView>(R.id.logout)
logout.setOnClickListener {
    Vibration.vibrate(this, 50)
    TokenManager.logout(this)
    startActivity(Intent(this, LoginActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
    finish()
}
```

Add import: `import com.arijit.budgettracker.utils.TokenManager`

Add a logout CardView to `activity_settings.xml` (similar style to existing CardViews).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arijit/budgettracker/AddExpenseActivity.kt app/src/main/java/com/arijit/budgettracker/MainActivity.kt app/src/main/java/com/arijit/budgettracker/SettingsActivity.kt app/src/main/res/layout/activity_settings.xml
git commit -m "feat: integrate sync into expense flow and add logout"
```

---

## Chunk 6: Verify + Test

### Task 17: Backend verification

- [ ] **Step 1: Start MySQL and create database**

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS smartexpense_db;"
```

- [ ] **Step 2: Build and run Spring Boot**

```bash
cd smartexpense-server && mvn spring-boot:run
```

Expected: Application starts on port 8080

- [ ] **Step 3: Test register endpoint**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"123456","name":"Test User"}'
```

Expected: `{"token":"...","email":"test@test.com","name":"Test User"}`

- [ ] **Step 4: Test login endpoint**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"123456"}'
```

Expected: `{"token":"...","email":"test@test.com","name":"Test User"}`

- [ ] **Step 5: Test expense CRUD with JWT**

```bash
TOKEN="<token from login>"
curl -X POST http://localhost:8080/api/expenses \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount":50.0,"category":"Food","timeStamp":1710000000000}'
```

Expected: `{"id":1,"amount":50.0,"category":"Food","timeStamp":1710000000000}`

### Task 18: Android verification

- [ ] **Step 1: Build Android app**

Run from Android Studio: Build > Make Project
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Test login flow on emulator**

1. Launch app — should show LoginActivity
2. Tap "Register" — fill form — should redirect to MainActivity
3. Close app, reopen — should skip login (token saved)

- [ ] **Step 3: Test offline-first flow**

1. Turn off server
2. Add expense in app — should save to Room successfully
3. Turn on server
4. Reopen app — SyncManager should push unsynced expenses

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "feat: complete MySQL backend integration with offline-first sync"
```
