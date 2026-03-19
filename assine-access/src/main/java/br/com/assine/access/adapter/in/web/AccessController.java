package br.com.assine.access.adapter.in.web;

import br.com.assine.access.domain.port.in.CheckAccessUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/access")
public class AccessController {

    private final CheckAccessUseCase checkAccessUseCase;

    public AccessController(CheckAccessUseCase checkAccessUseCase) {
        this.checkAccessUseCase = checkAccessUseCase;
    }

    @GetMapping("/{userId}/resource/{resource}")
    public ResponseEntity<Map<String, Boolean>> checkAccess(
            @PathVariable UUID userId,
            @PathVariable String resource) {
        
        boolean hasAccess = checkAccessUseCase.hasAccess(userId, resource);
        return ResponseEntity.ok(Map.of("hasAccess", hasAccess));
    }
}
