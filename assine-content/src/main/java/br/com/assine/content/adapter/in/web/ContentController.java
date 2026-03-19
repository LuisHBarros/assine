package br.com.assine.content.adapter.in.web;

import br.com.assine.content.domain.model.NewsletterContent;
import br.com.assine.content.domain.port.in.RetrieveContentUseCase;
import br.com.assine.content.domain.port.in.TriggerNewsletterRetryUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/content")
public class ContentController {

    private final RetrieveContentUseCase retrieveContentUseCase;
    private final TriggerNewsletterRetryUseCase triggerNewsletterRetryUseCase;

    public ContentController(RetrieveContentUseCase retrieveContentUseCase, TriggerNewsletterRetryUseCase triggerNewsletterRetryUseCase) {
        this.retrieveContentUseCase = retrieveContentUseCase;
        this.triggerNewsletterRetryUseCase = triggerNewsletterRetryUseCase;
    }

    @GetMapping("/today")
    public ResponseEntity<NewsletterContent> getTodayContent() {
        return retrieveContentUseCase.getTodayContent()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/retry-today")
    public ResponseEntity<Void> triggerRetry() {
        triggerNewsletterRetryUseCase.triggerRetry();
        return ResponseEntity.accepted().build();
    }
}
