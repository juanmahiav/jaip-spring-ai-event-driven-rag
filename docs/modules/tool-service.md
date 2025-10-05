# Tool Service

## Overview
The Tool Service is responsible for executing tool calls requested by the AI Service. It listens for tool call requests via Kafka, executes them by calling external services (specifically the MCP Server), and sends the results back via Kafka.

## Key Responsibilities
• Consume tool call requests from Kafka (`ai.tool.calls.v1`)
• Execute tool calls by communicating with the MCP Server
• Send tool results back to Kafka (`ai.tool.results.v1`)

## Architecture
The service operates using an event-driven architecture:
1. **Tool Call Reception**: Listens to `ai.tool.calls.v1` for incoming tool call requests
2. **Tool Execution**: Calls the appropriate endpoint on the MCP Server to execute the requested tool
3. **Result Publishing**: Sends the tool execution result to `ai.tool.results.v1`

## Configuration
The service is configured using the `application.yml` file:
```yaml
server.port: 8082
spring:
  kafka:
    bootstrap-servers: localhost:29092
    consumer.group-id: tool-service
    consumer.properties.spring.json.trusted.packages: "*"
    producer.value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
management.endpoints.web.exposure.include: health,info,prometheus
mcp:
  base-url: http://localhost:8091
```

## Dependencies
- Spring Boot
- Spring WebFlux
- Spring Kafka
- Micrometer (Prometheus)
- Jackson (JSON processing)
- Common (internal module with shared classes)

## Main Components

### ToolCallConsumer.java
Handles incoming tool call requests from Kafka with enhanced result structure:
- Listens to `ai.tool.calls.v1`
- Extracts tool name and arguments from the request
- Calls the MCP Server to execute the tool
- **Enhanced result structure**: Includes original arguments along with results for better traceability
- Sends comprehensive result to `ai.tool.results.v1`

### ToolServiceConfig.java
Configures the WebClient bean for communicating with the MCP Server.

### ToolKafkaConfig.java
Configures the message converter for Kafka messages with enhanced serialization.

### ToolServiceApplication.java
Main application class that bootstraps the Spring Boot application.

## Example Flow
1. AI Service detects a tool call is needed and sends a request to `ai.tool.calls.v1`
2. Tool Service receives the request and extracts:
   - `requestId`: The ID of the original request
   - `tool`: The name of the tool to call
   - `args`: Arguments for the tool
3. Tool Service calls the MCP Server at `http://localhost:8091/mcp/tools/{tool}` with the arguments
4. MCP Server executes the tool and returns the result
5. Tool Service sends enhanced result to `ai.tool.results.v1` with:
   - Original `requestId`
   - Tool name
   - **Original arguments** (for traceability)
   - Tool execution result

## Enhanced Features

### Improved Result Structure
**New in v0.2.0**: Tool results now include both original arguments and execution results:

**Previous result format**:
```json
{
  "requestId": "demo-1",
  "tool": "checkSanctionsList",
  "result": {"match": true, "score": 0.95, "rule": "contains"}
}
```

**Enhanced result format**:
```json
{
  "requestId": "demo-1",
  "tool": "checkSanctionsList",
  "args": {"name": "John Danger"},
  "result": {"match": true, "score": 0.95, "rule": "contains"}
}
```

This enhancement provides better traceability and allows the AI Service to generate more context-aware responses.

### Error Handling
- **Robust WebClient integration**: Handles MCP Server connectivity issues gracefully
- **Request validation**: Validates tool call format before processing
- **Comprehensive logging**: Detailed logging for debugging and monitoring

## Integration with Other Services
The Tool Service integrates with:
- **AI Service**: Receives tool calls and sends results via Kafka
- **MCP Server**: Executes tools via HTTP REST API
- **Kafka**: Message broker for event-driven communication

## Monitoring and Observability
- **Health endpoints**: `/actuator/health` for service health checks
- **Prometheus metrics**: `/actuator/prometheus` for metrics collection
- **Kafka consumer metrics**: Monitoring of message processing performance