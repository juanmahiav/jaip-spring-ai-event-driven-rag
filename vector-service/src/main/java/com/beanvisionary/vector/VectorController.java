package com.beanvisionary.vector;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vectors")
public class VectorController {
    private final VectorStore store;
    public VectorController(VectorStore store) { this.store = store; }

    @PostMapping("/upsert")
    public void upsert(@RequestBody List<Map<String, String>> docs) {
        List<Document> toAdd = docs.stream()
                .map(m -> new Document(m.getOrDefault("text", ""), (Map<String, Object>) (Map) m))
                .toList();
        store.add(toAdd);
    }

    @GetMapping("/search")
    public List<Document> search(
            @RequestParam("q") String q,
            @RequestParam(name = "k", defaultValue = "4") int k) {
        return store.similaritySearch(SearchRequest.builder()
                .query(q)
                .topK(k)
                .build());
    }
}
