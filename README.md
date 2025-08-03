# Market Data Service using Upstox API

## Table of Contents

1. [Overview](#overview)
2. [Goals](#goals)
3. [Non-Goals](#non-goals)
4. [Architecture](#architecture)
5. [Module Details](#module-details)
6. [Technology Stack](#technology-stack)
7. [API Design](#api-design)
8. [Security Implementation](#security-implementation)
9. [Data Storage Strategy](#data-storage-strategy)
10. [Caching Strategy](#caching-strategy)
11. [Development Roadmap](#development-roadmap)
12. [Testing Strategy](#testing-strategy)
13. [Deployment Considerations](#deployment-considerations)
14. [Performance & Monitoring](#performance--monitoring)

## 1. Overview

This service provides comprehensive market information by integrating with the Upstox API. It supports real-time data querying and basic market analysis features, including the ability to identify stocks that have dropped over the last 3 trading sessions. The system is built using Spring Boot and serves as a foundational component for future integration with other services like MCP (Model Context Protocol).

The service acts as a middleware layer between external market data sources and internal applications, providing cached, processed, and analyzed market data through well-defined REST endpoints.

## 2. Goals

### Primary Goals

- **Secure Integration**: Establish a secure and reliable connection with Upstox Market Data APIs using OAuth2 authentication
- **Data Storage & Caching**: Implement efficient storage and caching mechanisms for market data to optimize query performance
- **REST API Exposure**: Provide comprehensive REST endpoints for market data analytics and querying
- **Modular Architecture**: Design a modular and extensible system architecture for future integrations (e.g., LLMs, additional data sources)
- **Real-time Processing**: Enable real-time market data processing and analysis capabilities

### Secondary Goals

- **Scalability**: Design the system to handle increasing load and data volume
- **Reliability**: Implement robust error handling and fault tolerance mechanisms
- **Monitoring**: Integrate comprehensive logging and monitoring capabilities
- **Documentation**: Maintain clear API documentation and system architecture documentation

## 3. Non-Goals

- **Trade Execution**: No trade execution or order management capabilities
- **Portfolio Management**: No portfolio tracking or management features
- **Frontend/UI Layer**: No user interface components or frontend development
- **Real-time Trading Signals**: No automated trading signal generation
- **User Account Management**: No user registration or profile management

## 4. Architecture

### 4.1 High-Level System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                Spring Boot Application                   │
├─────────────────────────────────────────────────────────┤
│                 Controller Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │   Market    │  │  Analytics  │  │   Health    │    │
│  │ Controller  │  │ Controller  │  │ Controller  │    │
│  └─────────────┘  └─────────────┘  └─────────────┘    │
├─────────────────────────────────────────────────────────┤
│                 Service Layer                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │   Market    │  │  Analytics  │  │   Cache     │    │
│  │  Service    │  │  Service    │  │  Service    │    │
│  └─────────────┘  └─────────────┘  └─────────────┘    │
├─────────────────────────────────────────────────────────┤
│                Integration Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │   Upstox    │  │    Data     │  │  Security   │    │
│  │   Client    │  │ Processor   │  │  Manager    │    │
│  └─────────────┘  └─────────────┘  └─────────────┘    │
├─────────────────────────────────────────────────────────┤
│                 Data Layer                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │    Redis    │  │ PostgreSQL  │  │     H2      │    │
│  │   Cache     │  │ Production  │  │Development  │    │
│  └─────────────┘  └─────────────┘  └─────────────┘    │
└─────────────────────────────────────────────────────────┘
```

### 4.2 Data Flow Architecture

```
External API Request
        │
        v
┌───────────────┐
│   Controller  │
└───────┬───────┘
        │
        v
┌───────────────┐    ┌─────────────┐
│    Service    │───▶│    Cache    │
└───────┬───────┘    └─────────────┘
        │
        v
┌───────────────┐    ┌─────────────┐
│ Upstox Client │───▶│ Data Store  │
└───────────────┘    └─────────────┘
```

## 5. Module Details

### 5.1 Upstox Integration Module

**Purpose**: Handles authentication and data fetching from Upstox API

**Key Components**:

- **UpstoxApiClient**: Primary client for API communication
- **AuthenticationService**: Manages OAuth2 authentication flow
- **RateLimitManager**: Handles API rate limiting and throttling

**Technologies**:

- Spring WebClient (preferred over RestTemplate for better performance)
- OAuth2 Client support
- Circuit breaker pattern for fault tolerance

**Upstox API Endpoints**:

- Market Quote API: Real-time stock quotes
- OHLC API: Open, High, Low, Close data
- Historical Data API: Historical price data
- Instrument Master: List of available instruments
- WebSocket Feed: Real-time market data streaming

**Configuration Properties**:

```yaml
upstox:
  api:
    base-url: https://api-v2.upstox.com
    client-id: ${UPSTOX_CLIENT_ID}
    client-secret: ${UPSTOX_CLIENT_SECRET}
    redirect-uri: ${UPSTOX_REDIRECT_URI}
  rate-limits:
    orders: 10
    ohlc: 10
    quotes: 10
```

### 5.2 Data Storage Module

**Storage Options**:

#### Option 1: In-Memory (Development)

- Use ConcurrentHashMap for rapid prototyping
- Suitable for early development and testing
- No persistence across application restarts

#### Option 2: Redis + PostgreSQL (Production)

- **Redis**: High-speed caching layer for frequently accessed data
- **PostgreSQL**: Persistent storage for historical data and metadata
- **H2**: Embedded database for local development and testing

**Data Models**:

```java
// Market Data Entity
@Entity
public class MarketData {
    private String instrumentKey;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Long volume;
    private LocalDateTime timestamp;
    private LocalDate tradingDate;
}

// Instrument Metadata
@Entity
public class Instrument {
    private String instrumentKey;
    private String symbol;
    private String name;
    private String exchange;
    private String segment;
    private boolean isActive;
}
```

**Caching Strategy**:

- **L1 Cache**: Application-level caching using Caffeine
- **L2 Cache**: Redis for distributed caching
- **TTL Configuration**: Different TTL for different data types
  - Real-time quotes: 30 seconds
  - OHLC data: 5 minutes
  - Historical data: 1 hour
  - Instrument metadata: 24 hours

### 5.3 Market Data Processor

**Responsibilities**:

- Data normalization and validation
- Format standardization across different data sources
- Delta calculation over trading sessions
- Technical indicator computation
- Data quality checks and anomaly detection

**Processing Pipeline**:

1. **Raw Data Ingestion**: Receive data from Upstox API
2. **Validation**: Check data completeness and accuracy
3. **Normalization**: Convert to standard format
4. **Enrichment**: Add calculated fields and indicators
5. **Storage**: Persist to database and cache

**Key Features**:

- Configurable processing rules
- Error handling and retry mechanisms
- Audit trail for all data transformations
- Support for batch and real-time processing

### 5.4 Analytics & Query Layer

**Core Analytics Functions**:

- Price drop analysis over N trading sessions
- Volume analysis and trending
- Moving averages calculation
- Volatility analysis
- Correlation analysis between instruments

**REST API Endpoints**:

#### Market Data Endpoints

```
GET /api/v1/market/quote/{instrumentKey}
GET /api/v1/market/quotes?instruments={list}
GET /api/v1/market/ohlc/{instrumentKey}
GET /api/v1/market/historical/{instrumentKey}?from={date}&to={date}
```

#### Analytics Endpoints

```
GET /api/v1/analytics/drops/3days
GET /api/v1/analytics/volume/top?limit={n}
GET /api/v1/analytics/movers/gainers
GET /api/v1/analytics/movers/losers
GET /api/v1/analytics/volatility/{instrumentKey}
```

#### Health and Monitoring

```
GET /api/v1/health
GET /api/v1/metrics
GET /api/v1/cache/stats
```

## 6. Technology Stack

### 6.1 Core Framework

- **Spring Boot 3.2+**: Main application framework
- **Spring WebFlux**: For reactive programming (if needed)
- **Spring Data JPA**: Database access layer
- **Spring Security**: Security and authentication
- **Spring Cache**: Caching abstraction

### 6.2 HTTP Client

- **WebClient**: Primary choice for non-blocking HTTP calls
- **RestTemplate**: Fallback option for synchronous operations
- **RestClient (Spring 6.1+)**: Modern alternative combining WebClient and RestTemplate benefits

### 6.3 Database Technologies

- **PostgreSQL**: Primary database for production
- **Redis**: Caching and session storage
- **H2**: In-memory database for development/testing
- **HikariCP**: Connection pooling

### 6.4 Security

- **Spring Security OAuth2 Client**: OAuth2 integration
- **JWT**: Token-based authentication
- **BCrypt**: Password hashing

### 6.5 Monitoring & Observability

- **Spring Boot Actuator**: Application metrics and health checks
- **Micrometer**: Metrics collection
- **Logback**: Logging framework
- **Prometheus**: Metrics aggregation (optional)

## 7. API Design

### 7.1 RESTful Design Principles

- Use standard HTTP methods (GET, POST, PUT, DELETE)
- Resource-based URLs
- Consistent response formats
- Proper HTTP status codes
- Pagination for large datasets

### 7.2 Response Format Standards

```json
{
  "status": "success|error",
  "timestamp": "2024-01-15T10:30:00Z",
  "data": {},
  "pagination": {
    "page": 1,
    "size": 20,
    "total": 100
  },
  "error": {
    "code": "ERROR_CODE",
    "message": "Error description"
  }
}
```

### 7.3 API Versioning

- URL-based versioning: `/api/v1/`
- Backward compatibility for at least 2 versions
- Clear deprecation notices

## 8. Security Implementation

### 8.1 OAuth2 Integration with Upstox

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .oauth2Client(Customizer.withDefaults())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/v1/health").permitAll()
                .anyRequest().authenticated()
            )
            .build();
    }
}
```

### 8.2 API Key Management

- Secure storage of API credentials using Spring Boot configuration
- Environment-specific configuration
- Credential rotation support

### 8.3 Rate Limiting

- Implement client-side rate limiting to respect Upstox API limits
- Graceful degradation when limits are reached
- Queue management for burst traffic

## 9. Data Storage Strategy

### 9.1 Database Schema Design

```sql
-- Instruments table
CREATE TABLE instruments (
    instrument_key VARCHAR(50) PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    name VARCHAR(100),
    exchange VARCHAR(10),
    segment VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Market data table (time-series)
CREATE TABLE market_data (
    id BIGSERIAL PRIMARY KEY,
    instrument_key VARCHAR(50) REFERENCES instruments(instrument_key),
    open_price DECIMAL(12,4),
    high_price DECIMAL(12,4),
    low_price DECIMAL(12,4),
    close_price DECIMAL(12,4),
    volume BIGINT,
    trading_date DATE,
    data_timestamp TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_market_data_instrument_date ON market_data(instrument_key, trading_date);
CREATE INDEX idx_market_data_timestamp ON market_data(data_timestamp);
```

### 9.2 Data Retention Policy

- Real-time data: 7 days in hot storage
- Daily OHLC: 2 years in active storage
- Historical data: Archival after 5 years
- Cache data: Configurable TTL based on data type

## 10. Caching Strategy

### 10.1 Multi-Level Caching

```java
@Service
@CacheConfig(cacheNames = "marketData")
public class MarketDataService {

    @Cacheable(key = "#instrumentKey", unless = "#result == null")
    public MarketData getLatestQuote(String instrumentKey) {
        // Implementation
    }

    @CacheEvict(key = "#instrumentKey")
    public void refreshQuote(String instrumentKey) {
        // Implementation
    }
}
```

### 10.2 Cache Configuration

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 300000 # 5 minutes default
      cache-null-values: false
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-wait: -1ms
```

## 11. Development Roadmap

### Phase 1: Foundation (Weeks 1-2)

- [ ] Project setup and configuration
- [ ] Basic Upstox API integration
- [ ] Simple in-memory storage
- [ ] Basic REST endpoints
- [ ] Unit tests setup

### Phase 2: Core Features (Weeks 3-4)

- [ ] Database integration (PostgreSQL + H2)
- [ ] Redis caching implementation
- [ ] OAuth2 security setup
- [ ] Error handling and logging
- [ ] Integration tests

### Phase 3: Analytics (Weeks 5-6)

- [ ] Market data processor
- [ ] Basic analytics features
- [ ] Drop detection algorithm
- [ ] Volume analysis
- [ ] Performance optimization

### Phase 4: Production Ready (Weeks 7-8)

- [ ] Monitoring and metrics
- [ ] Docker containerization
- [ ] Documentation completion
- [ ] Performance testing
- [ ] Security audit

## 12. Testing Strategy

### 12.1 Testing Pyramid

- **Unit Tests**: 70% coverage target
- **Integration Tests**: API and database layer testing
- **Contract Tests**: Upstox API integration testing
- **Performance Tests**: Load and stress testing

### 12.2 Test Configuration

```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class MarketDataServiceTest {

    @MockBean
    private UpstoxApiClient upstoxClient;

    @Test
    void shouldFetchMarketData() {
        // Test implementation
    }
}
```

## 13. Deployment Considerations

### 13.1 Environment Configurations

- **Development**: H2 database, embedded Redis
- **Staging**: PostgreSQL, Redis cluster
- **Production**: Managed PostgreSQL, Redis cluster with persistence

### 13.2 Docker Configuration

```dockerfile
FROM openjdk:17-jre-slim

COPY target/market-data-service.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 13.3 Health Checks

- Database connectivity
- Redis availability
- Upstox API accessibility
- Memory and CPU utilization

## 14. Performance & Monitoring

### 14.1 Key Performance Indicators

- API response time: < 200ms for cached data
- Database query time: < 100ms average
- Cache hit ratio: > 80%
- API rate limit utilization: < 80%

### 14.2 Monitoring Setup

```java
@Component
public class MetricsConfiguration {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @EventListener
    public void handleCacheHit(CacheHitEvent event) {
        // Metrics collection
    }
}
```

### 14.3 Alerting Rules

- High error rate (> 5%)
- Low cache hit ratio (< 70%)
- Database connection pool exhaustion
- Memory usage > 85%
- API rate limit approaching

---

## Configuration Examples

### application.yml (Development)

```yaml
spring:
  application:
    name: market-data-service

  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: ""

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

  cache:
    type: simple

upstox:
  api:
    base-url: https://api-v2.upstox.com
    client-id: ${UPSTOX_CLIENT_ID:demo_client}
    client-secret: ${UPSTOX_CLIENT_SECRET:demo_secret}

logging:
  level:
    com.example.marketdata: DEBUG
```

### application-prod.yml (Production)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

  cache:
    type: redis
    redis:
      time-to-live: 300000

  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.example.marketdata: INFO
  file:
    name: /var/log/market-data-service.log
```

---

This document serves as a comprehensive guide for implementing the Market Data Service using Upstox API. It provides detailed specifications for each component while maintaining flexibility for future enhancements and integrations.
