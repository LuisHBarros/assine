package br.com.assine.content.domain.port.in;

import br.com.assine.content.domain.model.NewsletterContent;
import java.util.Optional;

public interface RetrieveContentUseCase {
    Optional<NewsletterContent> getTodayContent();
}
