package com.beanvisionary.mcp;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(path = "/mcp/tools", produces = MediaType.APPLICATION_JSON_VALUE)
public class ToolsController {

    private final ToolsService svc;

    public ToolsController(ToolsService svc) {
        this.svc = svc;
    }

    @PostMapping("/{tool}")
    public Map<String, Object> call(@PathVariable("tool") String tool,
                                    @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> args = (body == null) ? Map.of() : body;

        Object inner = args.get("args");
        if (inner instanceof Map<?, ?> m) {
            args = (Map<String, Object>) m;
        }

        return switch (tool) {
            case "launchCampaign" -> svc.launchCampaign(args);
            case "lookupOrder" -> svc.lookupOrder((String) args.get("orderId"));
            case "checkSanctionsList" -> svc.checkSanctions((String) args.get("name"));
            default -> Map.of("error", "UNKNOWN_TOOL", "tool", tool);
        };
    }
}