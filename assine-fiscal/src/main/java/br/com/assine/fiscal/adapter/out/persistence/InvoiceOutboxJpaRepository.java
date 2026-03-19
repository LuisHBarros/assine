package br.com.assine.fiscal.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InvoiceOutboxJpaRepository extends JpaRepository<InvoiceOutboxEntity, UUID> {
    Optional<InvoiceOutboxEntity> findByPaymentId(UUID paymentId);

    @Query("SELECT e FROM InvoiceOutboxEntity e WHERE e.issued = false ORDER BY e.createdAt ASC")
    List<InvoiceOutboxEntity> findUnissued(Pageable pageable);
}
