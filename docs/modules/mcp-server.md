# MCP Server

## Overview
The MCP (Model Context Protocol) Server provides a set of tools that can be invoked by the AI system. It exposes RESTful APIs that the Tool Service calls to execute specific business functions. The server simulates real-world tools that an AI might need to interact with.

## Key Responsibilities
• Provide REST endpoints for tool execution
• Implement business logic for various tools
• Return structured data responses for tool calls
• Handle error cases gracefully

## Architecture
The service is a simple Spring Boot application that:
1. **Exposes REST Endpoints**: Provides a single endpoint `/mcp/tools/{tool}` that accepts POST requests
2. **Routes Tool Calls**: Uses path variables to determine which tool to execute
3. **Processes Arguments**: Accepts JSON arguments for tool execution
4. **Returns Results**: Provides structured JSON responses with tool execution results

## Configuration
The service is configured using the `application.yml` file:
```yaml
server.port: 8091
management.endpoints.web.exposure.include: health,info,prometheus
error:
  include-message: always
  include-exception: true
  include-stacktrace: always
```

## Dependencies
- Spring Boot
- Spring WebFlux
- Spring Actuator
- Micrometer (Prometheus)
- Jackson (JSON processing)
- SLF4J (Logging)

## Main Components

### ToolsController.java
Handles incoming tool requests:
- Maps `/mcp/tools/{tool}` endpoint
- Routes requests to appropriate service methods based on tool name
- Handles argument parsing and preprocessing

### ToolsService.java
Implements the business logic for all available tools with enhanced debugging:
- `launchCampaign`: Simulates launching a marketing campaign
- `lookupOrder`: Retrieves order information by ID
- `checkSanctionsList`: Checks if a name appears in a sanctions list with comprehensive debugging
- **Enhanced logging**: Detailed debug output for sanctions checking process
- **Improved error handling**: Robust exception management for JSON processing

### ApiErrorHandler.java
Handles exceptions and provides consistent error responses:
- **Enhanced logging**: Uses SLF4J for structured logging
- **Comprehensive error handling**: Covers various exception types
- **Consistent error format**: Standardized error response structure

### McpServerApplication.java
Main application class that bootstraps the Spring Boot application.

## Available Tools

### launchCampaign
Simulates launching a marketing campaign.

**Endpoint**: `POST /mcp/tools/launchCampaign`

**Arguments**:
```json
{
  "args": {
    "budget": 1000.0
  }
}
```

**Response**:
```json
{
  "campaignId": "CMP_A1B2C3D4",
  "status": "SCHEDULED",
  "budget": 1000.0
}
```

### lookupOrder
Retrieves order information by ID.

**Endpoint**: `POST /mcp/tools/lookupOrder`

**Arguments**:
```json
{
  "args": {
    "orderId": "A1234"
  }
}
```

**Response**:
```json
{
  "orderId": "A1234",
  "status": "SHIPPED",
  "total": 129.90,
  "items": [
    {
      "sku": "S-001",
      "qty": 2
    },
    {
      "sku": "S-099",
      "qty": 1
    }
  ]
}
```

### checkSanctionsList
Checks if a name appears in a sanctions list with comprehensive debugging.

**Endpoint**: `POST /mcp/tools/checkSanctionsList`

**Arguments**:
```json
{
  "args": {
    "name": "John Danger"
  }
}
```

**Response**:
```json
{
  "match": true,
  "score": 0.95,
  "rule": "contains"
}
```

**Enhanced Features**:
- **Comprehensive debugging**: Detailed console output showing the matching process
- **Case-insensitive matching**: Handles various name formats
- **Confidence scoring**: Returns score based on match quality (0.95 for hits, 0.02 for no matches)
- **Robust error handling**: Graceful handling of null or malformed inputs

**Debug Output Example**:
```
=== SANCTIONS CHECK DEBUG ===
Input name: 'John Danger'
Sanctions list: [dangerous person, john danger, ...]
Lowercase name: 'john danger'
Checking 'dangerous person' against 'john danger' -> false
Checking 'john danger' against 'john danger' -> true
Overall match result: true
Final result: {match=true, score=0.95, rule=contains}
=== END SANCTIONS CHECK DEBUG ===
```

## Data Files
The service uses static data files for demonstration purposes:
- `orders.json`: Contains sample order data with various order statuses
- `sanctions.json`: Contains sample sanctions list entries for compliance testing

## Enhanced Error Handling
**New in v0.2.0**: Improved error handling throughout the service:
- **Structured logging**: All exceptions are logged with appropriate levels
- **Graceful degradation**: Service continues operating even with malformed requests
- **Detailed error responses**: Clear error messages for debugging
- **Resource management**: Proper handling of file I/O operations

## Example Usage
1. Start the MCP Server
2. Call a tool:
```bash
curl -X POST http://localhost:8091/mcp/tools/lookupOrder \
  -H "Content-Type: application/json" \
  -d '{"args": {"orderId": "A1234"}}'
```

3. Test sanctions checking with debug output:
```bash
curl -X POST http://localhost:8091/mcp/tools/checkSanctionsList \
  -H "Content-Type: application/json" \
  -d '{"args": {"name": "John Danger"}}'
```
Check the console for detailed debugging information.

## Integration with Other Services
The MCP Server integrates with:
- **Tool Service**: Receives tool calls via HTTP REST API
- **AI Service**: Indirectly provides tools for AI-generated tool calls
- **Monitoring**: Exposes Prometheus metrics for observability

## Monitoring and Observability
- **Health endpoints**: `/actuator/health` for service health checks
- **Prometheus metrics**: `/actuator/prometheus` for metrics collection
- **Detailed logging**: Comprehensive logging for debugging and monitoring