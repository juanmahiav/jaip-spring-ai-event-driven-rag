# Edge Gateway

## Overview
The Edge Gateway serves as the primary entry point for client applications to interact with the AI system. It provides HTTP REST endpoints for chat ingestion and WebSocket/SockJS endpoints for real-time streaming responses. The service acts as a bridge between client applications and the event-driven microservices architecture.

## Key Responsibilities
• Accept chat requests via HTTP REST API
• Publish chat requests to Kafka for asynchronous processing
• Provide WebSocket endpoints for real-time response streaming
• Forward AI responses from Kafka to connected WebSocket clients
• Handle CORS configuration for web client integration
• Manage session-based response routing

## Architecture
The service operates as a gateway with dual communication patterns:
1. **HTTP Ingestion**: Accepts REST requests and converts them to Kafka events
2. **WebSocket Streaming**: Provides real-time bidirectional communication with clients
3. **Event Forwarding**: Consumes AI responses from Kafka and forwards them to appropriate WebSocket clients
4. **Session Management**: Routes responses to specific client sessions via topic-based subscriptions

## Configuration
The service is configured using the `application.yml` file:
```yaml
server.port: 8083
spring:
  kafka:
    bootstrap-servers: localhost:29092
    consumer:
      group-id: edge-gateway
      properties:
        spring.json.trusted.packages: "*"
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.type.mapping: chatresponse:com.beanvisionary.common.ChatResponse
    producer:
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.type.mapping: chatresponse:com.beanvisionary.common.ChatResponse,chatrequest:com.beanvisionary.common.ChatRequest
```

## Dependencies
- Spring Boot
- Spring Web
- Spring WebSocket
- Spring Kafka
- Spring Messaging (STOMP)
- Jackson (JSON processing)
- Common (internal module with shared classes)

## Main Components

### ChatIngestController.java
Handles HTTP chat request ingestion:
- Maps `/api/chat` endpoint for POST requests
- Converts HTTP requests to ChatRequest Kafka events
- Provides request validation and ID generation
- Implements CORS support for web clients
- Returns ingestion status and WebSocket subscription information

### ResponseConsumer.java
Processes AI responses from Kafka and forwards to WebSocket clients:
- Listens to `ai.responses.v1` Kafka topic
- Routes responses to session-specific WebSocket topics
- Supports both streaming and final responses
- Maintains client session mapping

### WebSocketConfig.java
Configures WebSocket and SockJS endpoints:
- **Native WebSocket**: `/ws` endpoint for modern browsers
- **SockJS Fallback**: `/ws-sockjs` endpoint with fallback mechanisms
- **STOMP Protocol**: Message broker configuration for topic-based subscriptions
- **Cross-origin support**: Allows connections from configured origins

### CorsConfig.java
Configures Cross-Origin Resource Sharing (CORS):
- Allows specific origins (localhost:8000, 127.0.0.1:8000)
- Supports GET, POST, and OPTIONS methods
- Configures allowed headers for API requests
- Sets appropriate cache timing for preflight requests

### EdgeGatewayApplication.java
Main application class that bootstraps the Spring Boot application.

## API Endpoints

### Chat Ingestion
```http
POST /api/chat
Content-Type: application/json
Origin: http://localhost:8000

{
  "requestId": "optional-custom-id",
  "userId": "user-123",
  "sessionId": "session-456", 
  "query": "What is Qdrant and how does it help with RAG?",
  "metadata": {
    "source": "web-client",
    "timestamp": "2025-10-05T10:30:00Z"
  }
}
```

**Response**:
```json
{
  "requestId": "generated-or-provided-id",
  "sessionId": "session-456",
  "subscription": "/topic/replies.session-456",
  "status": "QUEUED"
}
```

**Status Values**:
- `QUEUED`: Request successfully queued for processing
- `REJECTED_EMPTY_QUERY`: Request rejected due to empty or missing query

## WebSocket Endpoints

### Native WebSocket
```javascript
const ws = new WebSocket('ws://localhost:8083/ws');
```

### SockJS Fallback
```javascript
const socket = new SockJS('http://localhost:8083/ws-sockjs');
```

### STOMP Integration
```javascript
const stompClient = Stomp.over(socket);
stompClient.connect({}, function(frame) {
    stompClient.subscribe('/topic/replies.session-456', function(message) {
        const response = JSON.parse(message.body);
        console.log('Received:', response);
    });
});
```

## Message Flow

### Request Processing Flow
1. Client sends HTTP POST to `/api/chat`
2. Edge Gateway validates and enriches the request
3. Request is published to `ai.requests.v1` Kafka topic
4. AI Service processes the request asynchronously
5. Response is published to `ai.responses.v1` Kafka topic
6. Edge Gateway forwards response to appropriate WebSocket topic
7. Connected clients receive real-time responses

### Session-Based Routing
- Each client session gets a unique WebSocket topic: `/topic/replies.{sessionId}`
- Responses are routed based on the original request's sessionId
- Multiple clients can connect to the same session topic
- Default session "default" is used if no sessionId is provided

## Enhanced Features

### Real-Time Streaming Support
- **Streaming Responses**: Supports real-time token-by-token streaming from AI service
- **Partial Messages**: Handles `[partial]` prefixed streaming tokens
- **Final Responses**: Delivers complete responses when processing is finished
- **Multiple Response Types**: Supports both tool-based and direct AI responses

### CORS Configuration
- **Flexible Origins**: Configurable allowed origins for web client integration
- **Method Support**: GET, POST, and OPTIONS for preflight requests
- **Header Management**: Supports Content-Type, Authorization, and Accept headers
- **Credential Handling**: Configurable credential support (disabled by default)

### Error Handling
- **Request Validation**: Validates required fields before processing
- **Kafka Integration**: Robust error handling for message publishing
- **WebSocket Management**: Graceful handling of client disconnections
- **Debug Logging**: Comprehensive logging for troubleshooting

## Integration with Other Services

The Edge Gateway integrates with:
- **AI Service**: Receives responses via Kafka for forwarding to clients
- **Kafka**: Message broker for asynchronous request/response handling
- **Web Clients**: Direct HTTP and WebSocket integration
- **Vector Service**: Indirect integration through AI Service for RAG functionality
- **Tool Services**: Indirect integration for tool-based responses

## Testing

### HTTP API Testing
```bash
curl -X POST http://localhost:8083/api/chat \
  -H "Content-Type: application/json" \
  -H "Origin: http://localhost:8000" \
  -d '{
    "userId": "test-user",
    "sessionId": "test-session",
    "query": "What is vector search?",
    "metadata": {"source": "curl-test"}
  }'
```

### WebSocket Testing
```javascript
const socket = new SockJS('http://localhost:8083/ws-sockjs');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected:', frame);
    
    stompClient.subscribe('/topic/replies.test-session', function(message) {
        const data = JSON.parse(message.body);
        console.log('Response:', data);
    });
    
    fetch('http://localhost:8083/api/chat', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            userId: 'test-user',
            sessionId: 'test-session',
            query: 'Hello AI!'
        })
    });
});
```

## Monitoring and Observability

### Health Checks
- Standard Spring Boot actuator endpoints available
- WebSocket connection health monitoring
- Kafka producer/consumer health status

### Performance Metrics
- HTTP request/response times
- WebSocket connection counts
- Kafka message throughput
- Session-based routing efficiency

### Debug Features
- **Console Logging**: Detailed debug output for request processing
- **Message Tracing**: Full request/response cycle tracking
- **Error Logging**: Comprehensive error reporting with stack traces
- **Connection Monitoring**: WebSocket connection state tracking

## Deployment Considerations

### Client Integration
- **CORS Configuration**: Update allowed origins for production domains
- **WebSocket Support**: Ensure proxy/load balancer supports WebSocket upgrades
- **Session Management**: Consider session persistence for high availability
- **Rate Limiting**: Implement rate limiting for production use

### Scalability
- **Horizontal Scaling**: Multiple gateway instances can run concurrently
- **Session Affinity**: Consider sticky sessions for WebSocket connections
- **Load Balancing**: WebSocket-aware load balancing required
- **Message Delivery**: Kafka ensures reliable message delivery across instances