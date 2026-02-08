package com.example.payment;

/**
 * Representerar svaret från Payment API.
 *
 * Detta motsvarar PaymentApiResponse från originalkoden.
 */
public class PaymentApiResponse {

    private final boolean success;
    private final String message;

    public PaymentApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static PaymentApiResponse success() {
        return new PaymentApiResponse(true, "Payment successful");
    }

    public static PaymentApiResponse failure(String reason) {
        return new PaymentApiResponse(false, reason);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}