package com.example.demo.incident;

import com.example.demo.incident.domain.AuditLog;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/incidents")
public class IncidentController {

    public record AlertRequest(String serviceName, String message) {
    }

    public record ApprovalRequest(boolean approved) {
    }

    public record AuditEntry(String node, String action, String detail, java.time.Instant createdAt) {
    }

    private final IncidentService service;

    public IncidentController(IncidentService service) {
        this.service = service;
    }

    @PostMapping
    public IncidentService.IncidentView open(@RequestBody AlertRequest request) {
        return service.open(request.serviceName(), request.message());
    }

    @GetMapping("/{id}")
    public IncidentService.IncidentView get(@PathVariable String id) {
        return service.view(id);
    }

    @GetMapping("/{id}/audit")
    public List<AuditEntry> audit(@PathVariable String id) {
        return service.auditTrail(id).stream()
                .map((AuditLog a) -> new AuditEntry(a.getNode(), a.getAction(), a.getDetail(), a.getCreatedAt()))
                .toList();
    }

    @PostMapping("/{id}/approval")
    public IncidentService.IncidentView decide(@PathVariable String id, @RequestBody ApprovalRequest request)
            throws Exception {
        try {
            return service.decide(id, request.approved());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }
}
