# Directory Structure

> How backend code is organized in this project.

---

## Overview

The backend follows a standard Spring Boot layered architecture: **Controller → Service → Repository**, with separate packages for models, DTOs, configuration, security, and WebSocket handlers.

Base package: `com.smarttraffic.backend`

---

## Directory Layout

```
src/main/java/com/smarttraffic/backend/
├── BackendSpringApplication.java       # Spring Boot entry point
├── config/                             # Application configuration
│   ├── AppRuntimeProperties.java       # Runtime properties (site URL, etc.)
│   ├── DbRuntimeProperties.java        # Database runtime properties
│   ├── InitAdminProperties.java        # Initial admin user config
│   ├── JwtProperties.java              # JWT secret/expiration config
│   ├── MaasProperties.java             # MaaS API config
│   ├── MirrorDataSourceConfig.java     # MySQL mirror datasource config
│   ├── MySqlMirrorProperties.java      # MySQL mirror properties
│   ├── NetworkProperties.java          # Network allowlist config
│   ├── PostgresMirrorProperties.java   # Postgres mirror properties
│   ├── SecurityConfig.java             # Spring Security configuration
│   ├── SecurityProperties.java         # Security-related properties
│   ├── SiteSettingsInitializer.java    # Site settings bootstrap
│   ├── TrafficProperties.java          # Traffic polling config
│   └── WebSocketConfig.java            # WebSocket endpoint config
├── controller/                         # REST API endpoints
│   ├── AdminController.java            # Admin CRUD (users, cameras, settings)
│   ├── AdminEventController.java       # Traffic event management
│   ├── AdminNodeController.java        # Edge node management
│   ├── AiAssistantController.java      # AI chat SSE endpoint
│   ├── AlertController.java            # Alert queries
│   ├── ApiClientController.java        # API key management
│   ├── ApiDocsController.java          # API documentation
│   ├── AuthController.java             # Login/register/profile
│   ├── EdgeTelemetryController.java    # Edge telemetry ingestion
│   ├── MaasController.java             # MaaS congestion data
│   ├── MapController.java              # Map overview data
│   ├── ReportController.java           # Traffic report export
│   ├── SiteSettingsController.java     # Site settings API
│   ├── TrafficController.java          # Real-time traffic data
│   └── UserController.java             # User profile management
├── dto/                                # Data Transfer Objects
│   ├── admin/                          # Admin-related DTOs
│   ├── auth/                           # Auth-related DTOs
│   ├── common/                         # Shared DTOs (CountResponse, MessageResponse)
│   ├── edge/                           # Edge telemetry DTOs
│   ├── maas/                           # MaaS congestion DTOs
│   ├── report/                         # Report export DTOs
│   └── traffic/                        # Traffic data DTOs
├── exception/                          # Global error handling
│   ├── ApiExceptionHandler.java        # @ControllerAdvice
│   └── AppException.java               # Custom business exception
├── model/                              # JPA entities
│   ├── AiChatConversationEntity.java   # AI conversation
│   ├── AiChatMessageEntity.java        # AI message
│   ├── ApiClientEntity.java            # API key
│   ├── ApiUsageLogEntity.java          # API usage log
│   ├── CameraEntity.java               # Camera
│   ├── SiteSettingsEntity.java         # Site settings
│   ├── TokenLlmEntity.java             # LLM token config
│   ├── TrafficEventEntity.java         # Traffic event
│   ├── TrafficSampleEntity.java        # Traffic sample
│   └── UserEntity.java                 # User
├── repository/                         # Spring Data JPA repositories
│   ├── AiChatConversationRepository.java
│   ├── AiChatMessageRepository.java
│   ├── AlertRepository.java
│   ├── ApiClientRepository.java
│   ├── ApiUsageLogRepository.java
│   ├── CameraRepository.java
│   ├── SiteSettingsRepository.java
│   ├── TokenLlmRepository.java
│   ├── TrafficEventRepository.java
│   ├── TrafficSampleRepository.java
│   └── UserRepository.java
├── security/                           # Authentication & authorization
│   ├── ApiKeyAuthenticationFilter.java  # API key auth filter
│   ├── ApiUsageLoggingFilter.java       # API usage tracking
│   ├── CurrentUser.java                 # Current user annotation
│   ├── CurrentUserAuthentication.java   # Current user auth token
│   ├── EdgeNodeAuthenticationFilter.java# Edge node auth
│   ├── JwtAuthenticationFilter.java     # JWT auth filter
│   ├── JwtService.java                  # JWT creation/validation
│   ├── RateLimitFilter.java             # Rate limiting
│   ├── SecurityUtils.java               # Security utilities
│   └── TokenExtractionService.java      # Token extraction
├── service/                            # Business logic
│   ├── AdminEventService.java           # Event management
│   ├── AiToolExecutor.java              # LLM tool execution
│   ├── AiTools.java                     # Tool JSON Schema definitions
│   ├── AlertService.java                # Alert queries
│   ├── ApiClientService.java            # API key management
│   ├── AuthService.java                 # Authentication logic
│   ├── CameraPollerService.java         # Camera status polling
│   ├── EdgeNodeConfigService.java       # Edge node config
│   ├── GeocodingService.java            # Reverse geocoding (Haversine)
│   ├── LlmClient.java                   # LLM client interface
│   ├── LlmService.java                  # LLM orchestration + tool loop
│   ├── MapOverviewService.java          # Map overview aggregation
│   ├── RoadService.java                 # Road name queries
│   ├── SystemMetricsService.java        # System metrics
│   ├── TrafficService.java              # Traffic data queries
│   ├── UserService.java                 # User management
│   └── analytics/                       # Analytics sub-package
│       ├── MaasService.java             # MaaS data integration
│       ├── MirrorWriteService.java      # MySQL mirror write
│       ├── RedisCacheService.java       # Redis caching
│       ├── RedisRateLimitService.java   # Redis rate limiting
│       ├── TelemetryIngestionService.java# Telemetry ingestion
│       └── TrafficReportExportService.java# Report export
└── websocket/                          # WebSocket handlers
    ├── AdminMetricsWebSocketHandler.java
    ├── FrameWebSocketHandler.java
    ├── TrafficInfoWebSocketHandler.java
    ├── WebSocketAttributes.java
    ├── WebSocketAuthInterceptor.java
    ├── WebSocketConnectionLimiter.java
    ├── WebSocketSchedulerConfig.java
    └── WebSocketUtils.java
```

---

## Module Organization

### Adding a New Feature

1. **Model**: Create entity in `model/` with JPA annotations
2. **Repository**: Create Spring Data JPA interface in `repository/`
3. **DTO**: Create request/response DTOs in `dto/<domain>/`
4. **Service**: Create service class in `service/` with `@Service`
5. **Controller**: Create controller in `controller/` with `@RestController`
6. **Migration**: Add Flyway migration in `src/main/resources/db/migration/`

### AI Module Specific

AI-related services follow a specialized pattern:
- `LlmClient.java` — Interface for LLM providers (OpenAI, Claude)
- `LlmService.java` — Orchestrates streaming, tool call loop, message persistence
- `AiTools.java` — Static tool JSON Schema definitions
- `AiToolExecutor.java` — Executes tool calls by dispatching to domain services
- `GeocodingService.java` — Location-based queries (Haversine distance)

---

## Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Entity | `*Entity.java` | `CameraEntity.java` |
| Repository | `*Repository.java` | `CameraRepository.java` |
| Service | `*Service.java` | `TrafficService.java` |
| Controller | `*Controller.java` | `TrafficController.java` |
| DTO (Request) | `*Request.java` | `LoginRequest.java` |
| DTO (Response) | `*Response.java` | `LoginResponse.java` |
| Config Properties | `*Properties.java` | `JwtProperties.java` |
| Filter | `*Filter.java` | `RateLimitFilter.java` |

---

## Examples

Well-organized modules to reference:
- **AI Module**: `service/LlmService.java` + `service/AiToolExecutor.java` + `service/AiTools.java` — Clean separation of orchestration, tool definition, and execution
- **Auth Module**: `controller/AuthController.java` + `service/AuthService.java` + `security/JwtService.java` — Standard layered auth flow
- **Traffic Module**: `controller/TrafficController.java` + `service/TrafficService.java` + `repository/TrafficSampleRepository.java` — Simple CRUD + query pattern

---

**Language**: All documentation should be written in **English**.