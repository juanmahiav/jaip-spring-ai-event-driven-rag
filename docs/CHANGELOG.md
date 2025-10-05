# Changelog

All notable changes to the Spring AI Event-Driven RAG project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - 2025-10-06

### Added
- **Streaming Response Capabilities**
  - Custom Ollama service with real-time token streaming
  - CustomOllamaService for enhanced streaming control
  - CustomChatModelAdapter for Spring AI integration
  - Real-time response delivery via Kafka and WebSocket

- **Enhanced AI Service**
  - Cloud model support (gpt-oss:120b-cloud)
  - Confidence-based tool result processing
  - Smart duplicate prevention for tool results
  - Enhanced error handling with graceful degradation
  - Comprehensive debug logging

- **Edge Gateway Module**
  - HTTP REST API for chat ingestion (`/api/chat`)
  - WebSocket streaming endpoints (`/ws`, `/ws-sockjs`)
  - SockJS fallback support for browser compatibility
  - Session-based response routing
  - CORS configuration for web client integration
  - Real-time response forwarding from Kafka to WebSocket clients

- **Enhanced Tool Processing**
  - Tool result structure now includes original arguments
  - Improved traceability for tool execution
  - Enhanced debugging for sanctions checking
  - Deterministic response generation based on tool type

- **Infrastructure Improvements**
  - Enhanced Kafka configuration with better serialization
  - Custom Unix timestamp deserializer for Instant support
  - Improved JSON type mappings for Kafka messages
  - Enhanced error handling across all services

- **Documentation**
  - Comprehensive module documentation for all services
  - Detailed testing scenarios and examples
  - Architecture diagrams reorganization
  - Enhanced API documentation with examples

### Changed
- **Model Configuration**
  - Upgraded from `gpt-oss:20b` to `gpt-oss:120b-cloud`
  - Enhanced embedding configuration with nomic-embed-text

- **Kafka Infrastructure**
  - Upgraded to Apache Kafka 4.1.0 (from Bitnami 4.0.0)
  - Enhanced producer/consumer configurations
  - Improved message serialization and type mapping

- **Database Updates**
  - Upgraded Qdrant to v1.13.0 (from v1.11.0)

- **Spring AI**
  - Updated to Spring AI v1.0.2 (from v1.0.1)

- **Code Quality**
  - Removed Lombok dependency across all modules
  - Replaced with explicit constructors and standard Java
  - Enhanced logging with SLF4J
  - Improved exception handling

- **Architecture**
  - Moved from `docs/diagrams/` to `docs/architecture/`
  - Enhanced documentation structure
  - Improved testing procedures

### Fixed
- Enhanced error handling for streaming responses
- Improved tool result confidence scoring
- Better handling of malformed JSON in tool responses
- Robust WebSocket connection management
- Improved CORS handling for web clients

### Removed
- Lombok dependency from all modules
- Old architecture diagrams location
- Deprecated configuration patterns

## [0.1.0] - 2025-08-13

### Added
- **Core Event-Driven Architecture**
  - Apache Kafka integration for asynchronous processing
  - Event-driven microservices communication
  - Kafka topics: `ai.requests.v1`, `ai.responses.v1`, `ai.tool.calls.v1`, `ai.tool.results.v1`

- **AI Service Module**
  - RAG (Retrieval-Augmented Generation) implementation
  - Ollama integration for local LLM inference
  - Qdrant vector database integration for semantic search
  - Tool call detection and processing
  - Spring AI framework integration

- **Vector Service Module**
  - Document embedding generation using Ollama
  - Vector storage and retrieval with Qdrant
  - REST API for document upsert and similarity search
  - Semantic search capabilities for RAG context

- **Tool Service Module**
  - Tool execution via Model Context Protocol (MCP)
  - Kafka-based tool call processing
  - WebClient integration with MCP Server
  - Asynchronous tool result delivery

- **MCP Server Module**
  - RESTful tool execution endpoints
  - Business logic implementation for various tools:
    - `launchCampaign`: Marketing campaign simulation
    - `lookupOrder`: Order information retrieval
    - `checkSanctionsList`: Compliance checking
  - JSON-based tool arguments and responses
  - Spring WebFlux reactive implementation

- **Common Module**
  - Shared data models and DTOs
  - Kafka topic constants
  - Common utilities and configurations

- **Infrastructure**
  - Docker Compose setup for Kafka and Qdrant
  - Prometheus metrics integration
  - Health check endpoints
  - Multi-module Maven project structure

- **Documentation**
  - Architecture overview and flow diagrams
  - Module-specific documentation
  - Setup and testing instructions
  - API examples and usage guides

### Technical Specifications
- **Java 21** runtime requirement
- **Spring Boot 3.5.4** framework
- **Spring AI 1.0.1** for LLM integration
- **Apache Kafka** for event streaming
- **Qdrant** vector database
- **Ollama** for local LLM and embeddings
- **Maven** multi-module build system

### Features
- **Retrieval-Augmented Generation (RAG)**
  - Vector-based document retrieval
  - Context-aware AI responses
  - Semantic search integration

- **Tool Calling System**
  - AI-driven tool selection
  - Asynchronous tool execution
  - Structured tool results processing

- **Observability**
  - Prometheus metrics collection
  - Health monitoring endpoints
  - Comprehensive logging

- **Scalability**
  - Event-driven architecture
  - Microservices decomposition
  - Asynchronous processing patterns

## [Unreleased]

### Planned
- Enhanced streaming performance optimizations
- Additional tool implementations
- Improved monitoring and alerting
- Advanced RAG techniques
- Multi-tenant support
- Authentication and authorization
- Production deployment guides

---

## Version Comparison

| Feature | v0.1.0 | v0.2.0 |
|---------|--------|--------|
| **Streaming** | ❌ | ✅ Real-time streaming |
| **Edge Gateway** | ❌ | ✅ HTTP/WebSocket API |
| **Tool Result Quality** | Basic | ✅ Confidence scoring |
| **Error Handling** | Basic | ✅ Enhanced with fallbacks |
| **Documentation** | Basic | ✅ Comprehensive |
| **Model Support** | Local only | ✅ Local + Cloud |
| **WebSocket Support** | ❌ | ✅ Native + SockJS |
| **CORS Support** | ❌ | ✅ Full configuration |
| **Code Quality** | Lombok | ✅ Standard Java |

## Migration Guide

### From v0.1.0 to v0.2.0

1. **Update Dependencies**
   ```bash
   # Update Docker images
   docker-compose down
   docker-compose -f docker/compose.yml up -d
   ```

2. **Configuration Changes**
   - Update model configuration to use cloud models if desired
   - Add enhanced Kafka serialization settings
   - Configure Edge Gateway CORS settings

3. **Client Integration**
   - Implement WebSocket client for real-time responses
   - Update HTTP API calls to use Edge Gateway endpoints
   - Handle streaming response format

4. **Testing Updates**
   - Use new comprehensive testing scenarios
   - Test both streaming and non-streaming workflows
   - Validate tool result confidence scoring