# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

hl-ai-code-mother is an AI-powered code generation application that generates HTML, Vue, and multi-file projects from natural language prompts. It uses LangChain4j for AI integration and LangGraph4j for orchestrating complex code generation workflows.

**Tech Stack:**
- Backend: Spring Boot 3.5.11, Java 21, Maven
- Frontend: Vue 3, Vite, TypeScript, Ant Design Vue
- AI: LangChain4j (DeepSeek API), LangGraph4j for workflow orchestration
- Database: MySQL 8, MyBatis-Flex ORM
- Cache: Caffeine (L1) + Redis (L2), HotKey for hot key detection
- Message Queue: RabbitMQ
- Object Storage: Tencent COS

## Build Commands

```bash
# Backend (Maven wrapper)
./mvnw clean compile          # Compile
./mvnw spring-boot:run        # Run development server (port 8123)
./mvnw test                   # Run all tests
./mvnw test -Dtest=ClassName  # Run single test class
./mvnw clean package          # Build JAR

# Frontend
npm install                   # Install dependencies
npm run dev                   # Start dev server
npm run build                 # Build for production
npm run type-check            # TypeScript check
npm run lint                  # ESLint check
```

## Architecture

### Backend Structure

```
src/main/java/com/hl/hlaicodemother/
├── controller/          # REST API endpoints (AppController, UserController, etc.)
├── service/             # Business logic interface
│   └── impl/            # Service implementations
├── manager/             # Higher-level business components
│   ├── cache/           # Multi-level cache templates (CacheAsideTemplate, MultiLevelCacheTemplate, HotKeyCacheTemplate)
│   └── websocket/       # WebSocket handlers for real-time chat
├── core/                # Core code generation facade and parsers
│   ├── AiCodeGeneratorFacade.java   # Main facade for AI code generation
│   ├── parser/          # Code parsing utilities
│   └── saver/           # Code file saving utilities
├── ai/                  # LangChain4j AI services
│   ├── AiCodeGeneratorService.java  # Interface with @SystemMessage prompts
│   ├── AiCodeGeneratorServiceFactory.java
│   └── tools/           # AI tools (FileReadTool, FileWriteTool, etc.)
├── langgraph4j/         # Workflow orchestration
│   ├── CodeGenWorkflow.java         # Main workflow definition
│   ├── CodeGenSubgraphWorkflow.java # Concurrent image collection subgraph
│   ├── node/            # Workflow nodes (ImageCollectorNode, CodeGeneratorNode, etc.)
│   ├── state/           # Workflow state (WorkflowContext)
│   └── tools/           # Workflow tools (ImageSearchTool, LogoGeneratorTool)
├── config/              # Spring configurations
├── model/               # Entity, DTO, VO, Enums
├── mapper/              # MyBatis-Flex mappers
├── ratelimit/           # Rate limiting annotations and aspects
└── bizmq/               # RabbitMQ message producers/consumers
```

### Key Architectural Patterns

**1. AI Code Generation Flow**
```
User Request → AppController.chatToGenCode() → AppService.chatToGenCode()
    → AiCodeGeneratorFacade.generateAndSaveCodeStream()
    → LangChain4j AI Service → Streaming response via WebSocket/SSE
```

**2. Workflow-Based Generation (LangGraph4j)**
```
START → ImageCollectorNode → PromptEnhancerNode → RouterNode 
    → CodeGeneratorNode → CodeQualityCheckNode → ProjectBuilderNode → END
```

The workflow state is managed via `WorkflowContext` stored in `MessagesState`.

**3. Multi-Level Caching**
- L1: Caffeine (local JVM cache)
- L2: Redis (distributed cache)
- HotKey: JD HotKey for hot key detection and local caching

Use `CacheAsideTemplate`, `MultiLevelCacheTemplate`, or `HotKeyCacheTemplate` for consistent cache semantics.

**4. Streaming Response Architecture**
- AI responses stream via WebSocket (`AppChatWebSocketHandler`) for real-time chat
- SSE endpoints (`AppController.chatToGenCode`) for server-to-client streaming
- Flux/Mono reactive streams for async processing

## Configuration

**Application config:** `src/main/resources/application.yml`
- Server port: 8123, context-path: /api
- Database: MySQL (hl_ai_code_mother)
- Redis: localhost:6379
- RabbitMQ: localhost:5672

**AI Configuration:**
- Default model: DeepSeek Chat (deepseek-chat)
- Reasoning model: DeepSeek Reasoner (deepseek-reasoner) for complex tasks
- API base URL: https://api.deepseek.com

**System Prompts:** Located in `src/main/resources/prompt/`
- `codegen-html-system-prompt.txt`
- `codegen-multi-file-system-prompt.txt`
- `codegen-vue-project-system-prompt.txt`

## Important Implementation Notes

**1. Code Generation Types (CodeGenTypeEnum)**
- `HTML`: Single HTML file with embedded CSS/JS
- `MULTI_FILE`: Multi-file project with separate HTML/CSS/JS
- `VUE_PROJECT`: Full Vue 3 project with build setup

**2. Code Output Location**
```
/tmp/hl-ai-code-output/{codeGenType}_{appId}/v{version}/
```

**3. AI Service Factory Pattern**
AI services use Spring's prototype scope with runtime API key configuration:
```java
// AiCodeGeneratorServiceFactory.createService(apiKey)
```

**4. Rate Limiting**
Use `@RateLimit` annotation on controller methods. Configured via `ratelimit` package aspects.

**5. Caching Best Practices**
- Use `HotKeyCacheTemplate` for high-frequency read operations
- Use `MultiLevelCacheTemplate` for cache-aside pattern with Redis fallback
- Cache key format: `hl-ai-code-mother:{feature}:{identifier}`

## Testing

Test files mirror the main source structure under `src/test/java/`. Key test classes:
- `CodeGenWorkflowTest` - Workflow integration tests
- `AiCodeGeneratorFacadeTest` - Core facade tests
- `ImageSearchToolTest` - Image collection tool tests

## Frontend

Located in `hl-ai-code-mother-frontend/`:
- Vue 3 + TypeScript + Vite
- Ant Design Vue for UI components
- Pinia for state management
- API client generated via OpenAPI

API base URL: `http://localhost:8123/api`

## Database Schema

See `sql/create_table.sql` for full schema. Main tables:
- `app` - Generated applications
- `app_version` - Version control for apps
- `app_member` - Multi-user access control
- `user` - User accounts
- `chat_history` - AI chat history
