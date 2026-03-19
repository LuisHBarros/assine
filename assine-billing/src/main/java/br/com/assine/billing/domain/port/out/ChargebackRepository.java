package br.com.assine.billing.domain.port.out;

import br.com.assine.billing.domain.model.Chargeback;

public interface ChargebackRepository {
    Chargeback save(Chargeback chargeback);
}
