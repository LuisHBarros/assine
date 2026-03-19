package br.com.assine.content.domain.model;

import java.time.LocalDate;

public record NewsletterContent(
    String title,
    String bodyHtml,
    LocalDate scheduledDate
) {
}
