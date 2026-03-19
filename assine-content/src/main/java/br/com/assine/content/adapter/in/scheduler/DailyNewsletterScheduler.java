package br.com.assine.content.adapter.in.scheduler;

import br.com.assine.content.application.service.ContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyNewsletterScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyNewsletterScheduler.class);
    private final ContentService contentService;

    public DailyNewsletterScheduler(ContentService contentService) {
        this.contentService = contentService;
    }

    @Scheduled(cron = "${app.newsletter.cron:0 0 7 * * *}", zone = "America/Sao_Paulo")
    public void scheduleDailyNewsletter() {
        log.info("Scheduled task triggered for daily newsletter");
        contentService.processDailyNewsletter();
    }
}
