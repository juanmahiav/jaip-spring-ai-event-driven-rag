# AI Service

## Overview
The AI Service processes natural language queries using Retrieval-Augmented Generation (RAG) with streaming response capabilities. It integrates with Ollama (including cloud models) for language model inference, Qdrant for vector searches, and Apache Kafka to enable event-driven communication with other services. The service features custom streaming implementation and confidence-based tool result processing.

## Key Responsibilities
• Consume user queries from Kafka (`ai.requests.v1`)
• Perform vector searches in Qdrant to retrieve relevant context
• Generate streaming responses using Ollama models (local or cloud)
• Detect when a tool call is needed and send requests to the Tool Service via Kafka
• Process tool results with confidence scoring and duplicate prevention
• Provide real-time streaming responses via custom Ollama integration

## Architecture
The service operates using an event-driven architecture with streaming capabilities:
1. **Query Ingestion**: Listens to `ai.requests.v1` for incoming user queries.
2. **Context Retrieval**: Leverages Qdrant to retrieve relevant documents.
3. **Streaming LLM Processing**: Uses custom Ollama streaming service to generate real-time responses.
4. **Tool Detection**: Analyzes responses for tool call requirements and forwards to `ai.tool.calls.v1`.
5. **Confidence-Based Result Processing**: Handles multiple tool results by selecting the highest confidence response.
6. **Response Generation**: Delivers either direct streaming responses or processes tool results for final answers.

## Configuration
The service is configured using the `application.yml` file:
```yaml
server.port: 8080
spring:
  kafka:
    bootstrap-servers: localhost:29092
    consumer:
      group-id: ai-service
      auto-offset-reset: earliest
      enable-auto-commit: true
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      properties:
        session.timeout.ms: 30000
        heartbeat.interval.ms: 10000
    template:
      default-topic: ai.responses.v1
    producer:
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.type.mapping: chatresponse:com.beanvisionary.common.ChatResponse,chatrequest:com.beanvisionary.common.ChatRequest
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: gpt-oss:120b-cloud
        streaming: false
      embedding:
        options:
          model: nomic-embed-text
    vectorstore:
      qdrant:
        host: localhost
        port: 6334
        collection-name: rag_docs
        initialize-schema: true
```

## Dependencies
- Spring Boot
- Spring AI (Ollama, Qdrant)
- Spring Kafka
- Micrometer (Prometheus)
- Jackson (JSON processing)
- WebClient (for HTTP calls)

## Main Components

### CustomOllamaService.java
**New in v0.2.0**: Custom streaming service that provides enhanced Ollama integration:
- Implements streaming responses with real-time token delivery
- Supports cloud models (e.g., gpt-oss:120b-cloud)
- Handles tool call detection and parsing
- Provides robust error handling and fallback mechanisms
- Bypasses Spring AI's default streaming for better control

### CustomChatModelAdapter.java
**New in v0.2.0**: Adapter that integrates CustomOllamaService with Spring AI's ChatClient:
- Bridges custom streaming implementation with Spring AI interfaces
- Enables use of custom Ollama service in standard Spring AI workflows

### AiConsumer.java
Handles incoming queries from Kafka with enhanced streaming capabilities:
- Listens to `ai.requests.v1`
- Performs vector search with Qdrant
- Uses CustomOllamaService for streaming responses
- Implements comprehensive error handling with fallback responses
- Sends real-time streaming responses to Kafka

### ToolResultConsumer.java
Processes tool results with confidence-based selection:
- Listens to `ai.tool.results.v1`
- Implements confidence scoring to select the best tool result
- Prevents duplicate processing while allowing better results
- Stores request context for proper response routing
- Generates deterministic responses based on tool type
- Sends final response to `ai.responses.v1`

### TopicsConfig.java
**New in v0.2.0**: Centralizes Kafka topic configuration and constants.

### AiConfig.java
Configures the ChatClient with custom Ollama service integration:
- Creates ChatClient using CustomChatModelAdapter
- Integrates custom streaming implementation with Spring AI

### KafkaConfig.java
Enhanced Kafka configuration with improved serialization:
- Configures ObjectMapper with JavaTimeModule for Instant support
- Implements custom Unix timestamp deserializer
- Provides enhanced JSON message conversion

## Key Features

### Streaming Responses
- **Real-time streaming**: Responses are streamed token-by-token as they're generated
- **Custom implementation**: Bypasses Spring AI's default streaming for better control
- **Cloud model support**: Works with both local and cloud Ollama models
- **Error resilience**: Graceful handling of streaming interruptions

### Confidence-Based Tool Result Processing
- **Smart result selection**: Chooses the best tool result based on confidence scores
- **Duplicate prevention**: Avoids processing redundant tool executions
- **Result quality optimization**: Prioritizes higher confidence responses
- **Deterministic responses**: Generates consistent responses based on tool type

### Enhanced Error Handling
- **Graceful degradation**: Provides meaningful error responses when services fail
- **Fallback mechanisms**: Multiple layers of error recovery
- **Comprehensive logging**: Detailed logging for debugging and monitoring

## Example Flow
1. User sends a query to `ai.requests.v1`
2. AI Service retrieves context from Qdrant
3. CustomOllamaService generates streaming response:
   - If it's a direct answer, streaming tokens are sent to `ai.responses.v1`
   - If it's a tool call, the request is sent to `ai.tool.calls.v1`
4. Tool Service executes the tool and sends result to `ai.tool.results.v1`
5. AI Service processes the result with confidence scoring and sends final answer to `ai.responses.v1`

## Testing
To test the service:
1. Ensure Kafka, Qdrant, and Ollama are running
2. Start the AI Service
3. Send a test query to `ai.requests.v1`:
```json
{
  "requestId": "test-1",
  "userId": "user-1",
  "sessionId": "session-1",
  "query": "What is Qdrant?",
  "metadata": {},
  "ts": "2025-10-05T00:00:00Z"
}
```
4. Monitor `ai.responses.v1` for streaming responses (prefixed with `[partial]` for streaming tokens)

## Tool Call Testing
To test tool functionality:
```json
{
  "requestId": "test-tool-1",
  "userId": "user-1", 
  "sessionId": "session-1",
  "query": "I need to check if customer John Danger is on our sanctions list",
  "metadata": {},
  "ts": "2025-10-05T00:00:00Z"
}
```
This should trigger the `checkSanctionsList` tool and return a confidence-scored result.