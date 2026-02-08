package com.example.payment;

/*
 * Refaktorerad PaymentProcessor-klass som är testbar genom dependency injection.
 *
 * ORIGINAL KOD (längst ner) hade följande problem:
 * - Hård-kodat API_KEY i koden
 * - Direkt beroende till PaymentApi (statisk metod)
 * - Direkt beroende till DatabaseConnection (singleton)
 * - Direkt beroende till EmailService (statisk metod)
 * - Omöjligt att testa utan externa dependencies.
 *
 * REFAKTORERING:
 * 1. Extraherade PaymentGateway interface (ersätter PaymentApi)
 * 2. Extraherade PaymentRepository interface (ersätter DatabaseConnection)
 * 3. Extraherade EmailNotificationService interface (ersätter EmailService)
 * 4. API-nyckeln injiceras nu via PaymentGateway implementation
 * 5. Använder constructor injection för alla beroenden
 */
public class PaymentProcessor {

    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final EmailNotificationService emailService;

    /*
     * Constructor för dependency injection.
     * @param paymentGateway Gateway för att processa betalningar (ersätter PaymentApi)
     * @param paymentRepository Repository för att spara betalningar (ersätter DatabaseConnection)
     * @param emailService Service för att skicka emails (ersätter EmailService)
     */
    public PaymentProcessor(PaymentGateway paymentGateway,
                            PaymentRepository paymentRepository,
                            EmailNotificationService emailService) {
        if (paymentGateway == null) {
            throw new IllegalArgumentException("PaymentGateway kan inte vara null");
        }
        if (paymentRepository == null) {
            throw new IllegalArgumentException("PaymentRepository kan inte vara null");
        }
        if (emailService == null) {
            throw new IllegalArgumentException("EmailNotificationService kan inte vara null");
        }

        this.paymentGateway = paymentGateway;
        this.paymentRepository = paymentRepository;
        this.emailService = emailService;
    }

    /*
     * Processar en betalning.
     * Denna metod matchar originalmetodens signatur: processPayment(double amount)
     * @param amount Belopp som ska betalas
     * @return true om betalningen lyckades, annars false
     */
    public boolean processPayment(double amount) {
        // Validera input
        if (amount <= 0) {
            return false;
        }

        // Anropa betaltjänst via interface istället för statisk metod
        PaymentApiResponse response = paymentGateway.charge(amount);

        // Spara till databas via interface istället för direkt singleton-access
        if (response.isSuccess()) {
            paymentRepository.savePayment(amount, "SUCCESS");
        }

        // Skicka e-post via interface istället för statisk metod
        if (response.isSuccess()) {
            emailService.sendPaymentConfirmation("user@example.com", amount);
        }

        return response.isSuccess();
    }

    /*
     * Överlagrad metod för att stödja custom email.
     *
     * @param amount Belopp
     * @param email Kundens email
     * @return true om betalningen lyckades
     */
    public boolean processPayment(double amount, String email) {
        if (amount <= 0) {
            return false;
        }

        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        PaymentApiResponse response = paymentGateway.charge(amount);

        if (response.isSuccess()) {
            paymentRepository.savePayment(amount, "SUCCESS");
            emailService.sendPaymentConfirmation(email, amount);
        } else {
            paymentRepository.savePayment(amount, "FAILED");
        }

        return response.isSuccess();
    }
}

/*
 * (innan refaktorering):
 *
 * public class PaymentProcessor {
 *     private static final String API_KEY = "sk_test_123456";
 *
 *     public boolean processPayment(double amount) {
 *         // Anropar extern betaltjänst direkt med statisk API-nyckel
 *         PaymentApiResponse response = PaymentApi.charge(API_KEY, amount);
 *
 *         // Skriver till databas direkt
 *         if (response.isSuccess()) {
 *             DatabaseConnection.getInstance()
 *                     .executeUpdate("INSERT INTO payments (amount, status) VALUES (" + amount + ", 'SUCCESS')");
 *         }
 *
 *         // Skickar e-post direkt
 *         if (response.isSuccess()) {
 *             EmailService.sendPaymentConfirmation("user@example.com", amount);
 *         }
 *
 *         return response.isSuccess();
 *     }
 * }
 */