package com.example.payment;

/*
 * Interface för payment repository.
 * Detta interface ersätter den direkta DatabaseConnection.getInstance() från originalkoden.
 * Implementationen kommer att hantera databasanslutningen.
 */
public interface PaymentRepository {

    /*
     * Sparar en betalning i databasen.
     * Ersätter: DatabaseConnection.getInstance().executeUpdate(
     *    "INSERT INTO payments (amount, status) VALUES (" + amount + ", 'SUCCESS')" )
     *
     * @param amount Belopp
     * @param status Status (t.ex. "SUCCESS" eller "FAILED")
     */
    void savePayment(double amount, String status);
}