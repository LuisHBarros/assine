package br.com.assine.subscriptions.domain.model;

import java.util.UUID;

public record Plan(
    UUID id,
    String name,
    Integer priceCents,
    String interval,
    boolean active
) {}
