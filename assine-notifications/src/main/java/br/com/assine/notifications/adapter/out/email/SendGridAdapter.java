package br.com.assine.notifications.adapter.out.email;

import br.com.assine.notifications.domain.port.out.EmailPort;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SendGridAdapter implements EmailPort {

    private static final Logger log = LoggerFactory.getLogger(SendGridAdapter.class);

    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public SendGridAdapter(
            @Value("${assine.sendgrid.api-key}") String apiKey,
            @Value("${assine.sendgrid.from-email}") String fromEmail,
            @Value("${assine.sendgrid.from-name}") String fromName
    ) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Override
    public void send(String to, String subject, String body) {
        Email from = new Email(fromEmail, fromName);
        Email recipient = new Email(to);
        Content content = new Content("text/plain", body); // We could use text/html here too
        Mail mail = new Mail(from, subject, recipient, content);

        SendGrid sg = new SendGrid(apiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Email sent to {} with subject: {}", to, subject);
            } else {
                log.error("Failed to send email to {}. Status Code: {}, Body: {}", to, response.getStatusCode(), response.getBody());
            }
        } catch (IOException ex) {
            log.error("Error sending email to {}: {}", to, ex.getMessage(), ex);
        }
    }
}
