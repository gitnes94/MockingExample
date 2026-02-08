package com.example.payment;

/**
 * Interface för email notification service.
 * Detta interface ersätter den statiska EmailService-klassen från originalkoden.
 */
public interface EmailNotificationService {

    /**
     * Skickar betalningsbekräftelse via email.
     * Ersätter: EmailService.sendPaymentConfirmation("user@example.com", amount)
     * @param email Mottagarens email
     * @param amount Belopp som betalats
     */
    void sendPaymentConfirmation(String email, double amount);
}