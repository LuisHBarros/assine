package br.com.assine.notifications.template;

import br.com.assine.notifications.domain.event.MagicLinkRequestedEvent;
import br.com.assine.notifications.domain.event.PaymentConfirmedEvent;
import br.com.assine.notifications.domain.event.SubscriptionActivatedEvent;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateResolver {

    public String resolveMagicLinkBody(MagicLinkRequestedEvent event) {
        return "Olá! Clique no link abaixo para entrar no Assine:\n\n" +
                "https://assine.com.br/auth/magic-link?token=" + event.token() + "\n\n" +
                "Se você não solicitou este link, por favor ignore este e-mail.";
    }

    public String resolvePaymentConfirmedBody(PaymentConfirmedEvent event) {
        return "Olá " + event.userName() + ",\n\n" +
                "Seu pagamento de R$ " + event.amount() + " foi confirmado com sucesso!\n" +
                "Sua assinatura (" + event.subscriptionId() + ") está ativa.";
    }

    public String resolveSubscriptionActivatedBody(SubscriptionActivatedEvent event) {
        return "Olá " + event.userName() + ",\n\n" +
                "Sua assinatura do plano " + event.planName() + " foi ativada com sucesso!\n" +
                "Bem-vindo ao Assine.";
    }
}
