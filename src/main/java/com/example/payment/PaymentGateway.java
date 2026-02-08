package com.example.payment;

/*
 * Interface för payment gateway.
 * Detta interface ersätter den statiska PaymentApi-klassen från originalkoden.
 * Implementationen kommer att hantera API-nyckeln internt.
 */
public interface PaymentGateway {

    /*
     * Tar betalt för ett belopp.
     * Ersätter: PaymentApi.charge(API_KEY, amount)
     * @param amount Belopp som ska betalas
     * @return PaymentApiResponse med resultatet
     */
    PaymentApiResponse charge(double amount);
}