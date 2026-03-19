package br.com.assine.notifications.domain.port.out;

public interface EmailPort {
    void send(String to, String subject, String body);
}
