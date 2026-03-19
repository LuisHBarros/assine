package br.com.assine.subscriptions.adapter.out.persistence;

import br.com.assine.subscriptions.domain.model.Plan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "plans")
public class PlanEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "price_cents", nullable = false)
    private Integer priceCents;

    @Column(nullable = false)
    private String interval;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PlanEntity() {}

    public static PlanEntity fromDomain(Plan plan) {
        PlanEntity entity = new PlanEntity();
        entity.setId(plan.id());
        entity.name = plan.name();
        entity.priceCents = plan.priceCents();
        entity.interval = plan.interval();
        entity.active = plan.active();
        entity.createdAt = LocalDateTime.now();
        return entity;
    }

    public Plan toDomain() {
        return new Plan(getId(), name, priceCents, interval, active);
    }

    // Getters
    public String getName() { return name; }
    public Integer getPriceCents() { return priceCents; }
    public String getInterval() { return interval; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
