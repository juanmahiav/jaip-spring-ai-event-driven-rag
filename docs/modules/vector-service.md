# Vector Service

## Overview
The Vector Service is a RESTful API that enables storing, managing, and searching vector embeddings for text documents. It uses Ollama's nomic-embed-text model to generate embeddings and stores them in a Qdrant vector database. The service provides endpoints for upserting documents and performing semantic search based on text queries.

### API Endpoints
### Upsert Documents
```http
POST /vectors/upsert
Content-Type: application/json

[
  {
    "id": "doc:1",
    "text": "Document content to be embedded",
    "metadata": "any additional metadata"
  }
]
```

**Features**:
- **Batch processing**: Accepts multiple documents in a single request
- **Flexible metadata**: Supports arbitrary metadata fields
- **Automatic embedding**: Generates embeddings using Ollama's nomic-embed-text model
- **Qdrant integration**: Stores vectors with associated metadata in Qdrant collections

### Search Documents
```http
GET /vectors/search?q=query text&k=4
```

**Parameters**:
- `q` (required): Query text to search for similar documents
- `k` (optional, default=4): Number of similar documents to return

**Features**:
- **Semantic search**: Uses vector similarity for finding relevant documents
- **Configurable results**: Adjustable number of returned documents
- **Score-based ranking**: Results ordered by similarity scoresging vector embeddings and performing similarity searches using Qdrant as the vector database. It provides REST APIs for upserting documents and searching for similar documents based on text queries.

## Key Responsibilities
• Generate embeddings for text documents using Ollama's embedding model
• Store document embeddings in Qdrant vector database
• Perform similarity searches to find relevant documents
• Provide REST APIs for document management and search operations

## Architecture
The service uses Spring AI to integrate with Ollama for embedding generation and Qdrant for vector storage:
1. **Document Ingestion**: Documents are received via REST API and converted to embeddings
2. **Embedding Generation**: Ollama's `nomic-embed-text` model generates vector embeddings
3. **Vector Storage**: Embeddings are stored in Qdrant with associated metadata
4. **Similarity Search**: Queries are converted to embeddings and used to find similar documents

## Configuration
The service is configured using the `application.yml` file:
```yaml
server.port: 8081
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      embedding.options.model: nomic-embed-text
    vectorstore:
      qdrant:
        host: localhost
        port: 6334
        collection-name: rag_docs
        initialize-schema: true
management.endpoints.web.exposure.include: health,info,prometheus
```

## Dependencies
- Spring Boot
- Spring AI (Ollama, Qdrant)
- Spring Web
- Micrometer (Prometheus)

## Main Components

### VectorController.java
Provides REST endpoints for vector operations:
- `POST /vectors/upsert`: Accepts a list of documents and stores their embeddings in Qdrant
- `GET /vectors/search`: Performs similarity search based on a query string

### VectorServiceApplication.java
Main application class that bootstraps the Spring Boot application.

## API Endpoints

### Upsert Documents
```http
POST /vectors/upsert
Content-Type: application/json

[
  {
    "text": "Document content to be embedded",
    "metadata": "any additional metadata"
  }
]
```

### Search Documents
```http
GET /vectors/search?q=query text&k=4
```
Returns the k most similar documents to the query text.

## Example Usage
1. Start the Vector Service (ensure Qdrant is running)
2. Upsert documents:
```bash
curl -X POST http://localhost:8081/vectors/upsert \
  -H "Content-Type: application/json" \
  -d '[
    {"id": "doc:1", "text": "Qdrant is a vector database and vector search engine"},
    {"id": "doc:2", "text": "Ollama is a tool for running LLMs locally"}
  ]'
```
3. Search for similar documents:
```bash
curl "http://localhost:8081/vectors/search?q=vector database&k=2"
```

## Performance Considerations
- **Embedding generation**: Processing time depends on document length and Ollama model performance
- **Vector storage**: Qdrant provides efficient storage and retrieval of high-dimensional vectors
- **Search performance**: Similarity search is optimized for real-time retrieval
- **Scalability**: Service can handle concurrent requests for both upsert and search operations

## Integration with Other Services
The Vector Service integrates with:
- **AI Service**: Provides vector search capabilities for RAG functionality
- **Qdrant**: Stores and manages vector embeddings with collection management
- **Ollama**: Generates embeddings for text documents using the nomic-embed-text model

## Monitoring and Observability
- **Health endpoints**: `/actuator/health` for service health checks
- **Prometheus metrics**: `/actuator/prometheus` for metrics collection
- **Performance monitoring**: Track embedding generation and search response times