package br.com.assine.content.domain.port.out;

import br.com.assine.content.domain.model.NewsletterContent;
import java.util.Optional;

public interface ContentSourcePort {
    Optional<NewsletterContent> fetchTodayContent();
}
