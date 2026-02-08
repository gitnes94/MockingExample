package com.example.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Enhetstester för refaktorerade PaymentProcessor-klassen.
 *
 * Originalklassen hade följande problem:
 * - Hård-kodat API_KEY
 * - Statiska metoder (PaymentApi.charge, EmailService.sendPaymentConfirmation)
 * - Singleton (DatabaseConnection.getInstance())
 * - SQL injection risk
 * - Omöjlig att testa
 *
 * Efter refaktorering:
 * - Alla beroenden injiceras via constructor
 * - Använder interfaces istället för konkreta implementationer
 * - Fullständigt testbar med mocks
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentProcessor Tests (Refactored)")
class PaymentProcessorTest {

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EmailNotificationService emailService;

    private PaymentProcessor paymentProcessor;

    @BeforeEach
    void setUp() {
        paymentProcessor = new PaymentProcessor(paymentGateway, paymentRepository, emailService);
    }

    @Nested
    @DisplayName("Constructor Validation Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Ska skapa PaymentProcessor med giltiga beroenden")
        void shouldCreateProcessorWithValidDependencies() {
            // Act & Assert
            assertThatCode(() -> new PaymentProcessor(paymentGateway, paymentRepository, emailService))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Ska kasta exception om PaymentGateway är null")
        void shouldThrowExceptionWhenPaymentGatewayIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> new PaymentProcessor(null, paymentRepository, emailService))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PaymentGateway kan inte vara null");
        }

        @Test
        @DisplayName("Ska kasta exception om PaymentRepository är null")
        void shouldThrowExceptionWhenPaymentRepositoryIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> new PaymentProcessor(paymentGateway, null, emailService))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PaymentRepository kan inte vara null");
        }

        @Test
        @DisplayName("Ska kasta exception om EmailNotificationService är null")
        void shouldThrowExceptionWhenEmailServiceIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> new PaymentProcessor(paymentGateway, paymentRepository, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("EmailNotificationService kan inte vara null");
        }
    }

    @Nested
    @DisplayName("processPayment(double) Success Tests")
    class ProcessPaymentSuccessTests {

        @Test
        @DisplayName("Ska processa betalning framgångsrikt")
        void shouldProcessPaymentSuccessfully() {
            // Arrange
            double amount = 100.0;
            PaymentApiResponse successResponse = PaymentApiResponse.success();
            when(paymentGateway.charge(amount)).thenReturn(successResponse);

            // Act
            boolean result = paymentProcessor.processPayment(amount);

            // Assert
            assertThat(result).isTrue();
            verify(paymentGateway).charge(amount);
            verify(paymentRepository).savePayment(amount, "SUCCESS");
            verify(emailService).sendPaymentConfirmation("user@example.com", amount);
        }

        @Test
        @DisplayName("Ska anropa alla steg i rätt ordning")
        void shouldCallAllStepsInCorrectOrder() {
            // Arrange
            double amount = 50.0;
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.success());

            // Act
            paymentProcessor.processPayment(amount);

            // Assert - verifiera ordning
            InOrder inOrder = inOrder(paymentGateway, paymentRepository, emailService);
            inOrder.verify(paymentGateway).charge(amount);
            inOrder.verify(paymentRepository).savePayment(amount, "SUCCESS");
            inOrder.verify(emailService).sendPaymentConfirmation("user@example.com", amount);
        }

        @ParameterizedTest(name = "Amount: {0} SEK")
        @ValueSource(doubles = {0.01, 10.0, 100.0, 999.99, 10000.0})
        @DisplayName("Ska hantera olika belopp korrekt")
        void shouldHandleVariousAmounts(double amount) {
            // Arrange
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.success());

            // Act
            boolean result = paymentProcessor.processPayment(amount);

            // Assert
            assertThat(result).isTrue();
            verify(paymentGateway).charge(amount);
        }

        @Test
        @DisplayName("Ska spara betalning med korrekt status")
        void shouldSavePaymentWithCorrectStatus() {
            // Arrange
            double amount = 100.0;
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.success());

            // Act
            paymentProcessor.processPayment(amount);

            // Assert
            ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
            verify(paymentRepository).savePayment(eq(amount), statusCaptor.capture());
            assertThat(statusCaptor.getValue()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("Ska skicka email med korrekt amount")
        void shouldSendEmailWithCorrectAmount() {
            // Arrange
            double amount = 250.50;
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.success());

            // Act
            paymentProcessor.processPayment(amount);

            // Assert
            verify(emailService).sendPaymentConfirmation("user@example.com", amount);
        }
    }

    @Nested
    @DisplayName("processPayment(double) Failure Tests")
    class ProcessPaymentFailureTests {

        @Test
        @DisplayName("Ska returnera false om payment gateway misslyckas")
        void shouldReturnFalseWhenGatewayFails() {
            // Arrange
            double amount = 100.0;
            PaymentApiResponse failureResponse = PaymentApiResponse.failure("Insufficient funds");
            when(paymentGateway.charge(amount)).thenReturn(failureResponse);

            // Act
            boolean result = paymentProcessor.processPayment(amount);

            // Assert
            assertThat(result).isFalse();
            verify(paymentGateway).charge(amount);
        }

        @Test
        @DisplayName("Ska inte spara till databas om betalning misslyckas")
        void shouldNotSaveToDatabaseWhenPaymentFails() {
            // Arrange
            double amount = 100.0;
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.failure("Card declined"));

            // Act
            paymentProcessor.processPayment(amount);

            // Assert
            verify(paymentRepository, never()).savePayment(anyDouble(), anyString());
        }

        @Test
        @DisplayName("Ska inte skicka email om betalning misslyckas")
        void shouldNotSendEmailWhenPaymentFails() {
            // Arrange
            double amount = 100.0;
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.failure("Invalid card"));

            // Act
            paymentProcessor.processPayment(amount);

            // Assert
            verify(emailService, never()).sendPaymentConfirmation(anyString(), anyDouble());
        }

        @Test
        @DisplayName("Ska returnera false för ogiltigt belopp (noll)")
        void shouldReturnFalseForZeroAmount() {
            // Act
            boolean result = paymentProcessor.processPayment(0.0);

            // Assert
            assertThat(result).isFalse();
            verify(paymentGateway, never()).charge(anyDouble());
            verify(paymentRepository, never()).savePayment(anyDouble(), anyString());
            verify(emailService, never()).sendPaymentConfirmation(anyString(), anyDouble());
        }

        @ParameterizedTest(name = "Invalid amount: {0}")
        @ValueSource(doubles = {-100.0, -1.0, -0.01, 0.0})
        @DisplayName("Ska returnera false för ogiltiga belopp")
        void shouldReturnFalseForInvalidAmounts(double amount) {
            // Act
            boolean result = paymentProcessor.processPayment(amount);

            // Assert
            assertThat(result).isFalse();
            verifyNoInteractions(paymentGateway, paymentRepository, emailService);
        }
    }

    @Nested
    @DisplayName("processPayment(double, String) with Custom Email Tests")
    class ProcessPaymentWithEmailTests {

        @Test
        @DisplayName("Ska processa betalning med custom email")
        void shouldProcessPaymentWithCustomEmail() {
            // Arrange
            double amount = 100.0;
            String email = "customer@example.com";
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.success());

            // Act
            boolean result = paymentProcessor.processPayment(amount, email);

            // Assert
            assertThat(result).isTrue();
            verify(paymentGateway).charge(amount);
            verify(paymentRepository).savePayment(amount, "SUCCESS");
            verify(emailService).sendPaymentConfirmation(email, amount);
        }

        @Test
        @DisplayName("Ska skicka email till rätt mottagare")
        void shouldSendEmailToCorrectRecipient() {
            // Arrange
            double amount = 50.0;
            String email = "john.doe@example.com";
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.success());

            // Act
            paymentProcessor.processPayment(amount, email);

            // Assert
            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendPaymentConfirmation(emailCaptor.capture(), eq(amount));
            assertThat(emailCaptor.getValue()).isEqualTo(email);
        }

        @Test
        @DisplayName("Ska spara FAILED status om betalning misslyckas")
        void shouldSaveFailedStatusWhenPaymentFails() {
            // Arrange
            double amount = 100.0;
            String email = "test@example.com";
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.failure("Card expired"));

            // Act
            boolean result = paymentProcessor.processPayment(amount, email);

            // Assert
            assertThat(result).isFalse();
            verify(paymentRepository).savePayment(amount, "FAILED");
            verify(emailService, never()).sendPaymentConfirmation(anyString(), anyDouble());
        }

        @Test
        @DisplayName("Ska returnera false för null email")
        void shouldReturnFalseForNullEmail() {
            // Act
            boolean result = paymentProcessor.processPayment(100.0, null);

            // Assert
            assertThat(result).isFalse();
            verifyNoInteractions(paymentGateway, paymentRepository, emailService);
        }

        @Test
        @DisplayName("Ska returnera false för tom email")
        void shouldReturnFalseForEmptyEmail() {
            // Act
            boolean result = paymentProcessor.processPayment(100.0, "   ");

            // Assert
            assertThat(result).isFalse();
            verifyNoInteractions(paymentGateway, paymentRepository, emailService);
        }

        @Test
        @DisplayName("Ska returnera false för ogiltigt belopp med custom email")
        void shouldReturnFalseForInvalidAmountWithCustomEmail() {
            // Act
            boolean result = paymentProcessor.processPayment(-50.0, "test@example.com");

            // Assert
            assertThat(result).isFalse();
            verifyNoInteractions(paymentGateway, paymentRepository, emailService);
        }

        @ParameterizedTest(name = "Email: {0}")
        @ValueSource(strings = {"test@example.com", "user@domain.se", "info@company.org"})
        @DisplayName("Ska hantera olika email-adresser")
        void shouldHandleVariousEmails(String email) {
            // Arrange
            double amount = 100.0;
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.success());

            // Act
            boolean result = paymentProcessor.processPayment(amount, email);

            // Assert
            assertThat(result).isTrue();
            verify(emailService).sendPaymentConfirmation(email, amount);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Ska hantera komplett framgångsrik betalningsprocess")
        void shouldHandleCompleteSuccessfulPaymentProcess() {
            // Arrange
            double amount = 150.0;
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.success());

            // Act
            boolean result = paymentProcessor.processPayment(amount);

            // Assert
            assertThat(result).isTrue();

            // Verifiera att alla steg kördes exakt en gång
            verify(paymentGateway, times(1)).charge(amount);
            verify(paymentRepository, times(1)).savePayment(amount, "SUCCESS");
            verify(emailService, times(1)).sendPaymentConfirmation("user@example.com", amount);

            // Verifiera att inget annat anropades
            verifyNoMoreInteractions(paymentGateway, paymentRepository, emailService);
        }

        @Test
        @DisplayName("Ska hantera misslyckad betalning korrekt utan sidoeffekter")
        void shouldHandleFailedPaymentWithoutSideEffects() {
            // Arrange
            double amount = 200.0;
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.failure("Network error"));

            // Act
            boolean result = paymentProcessor.processPayment(amount);

            // Assert
            assertThat(result).isFalse();
            verify(paymentGateway, times(1)).charge(amount);
            verifyNoInteractions(paymentRepository, emailService);
        }

        @Test
        @DisplayName("Ska verifiera att alla dependencies används korrekt")
        void shouldVerifyAllDependenciesAreUsedCorrectly() {
            // Arrange
            double amount = 75.0;
            String email = "customer@test.com";
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.success());

            // Act
            paymentProcessor.processPayment(amount, email);

            // Assert
            verify(paymentGateway).charge(amount);
            verify(paymentRepository).savePayment(amount, "SUCCESS");
            verify(emailService).sendPaymentConfirmation(email, amount);
        }

        @Test
        @DisplayName("Ska hantera flera betalningar efter varandra")
        void shouldHandleMultiplePaymentsInSequence() {
            // Arrange
            when(paymentGateway.charge(anyDouble())).thenReturn(PaymentApiResponse.success());

            // Act
            boolean result1 = paymentProcessor.processPayment(50.0);
            boolean result2 = paymentProcessor.processPayment(75.0);
            boolean result3 = paymentProcessor.processPayment(100.0);

            // Assert
            assertThat(result1).isTrue();
            assertThat(result2).isTrue();
            assertThat(result3).isTrue();

            verify(paymentGateway, times(3)).charge(anyDouble());
            verify(paymentRepository, times(3)).savePayment(anyDouble(), eq("SUCCESS"));
            verify(emailService, times(3)).sendPaymentConfirmation(anyString(), anyDouble());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Ska hantera mycket små belopp korrekt")
        void shouldHandleVerySmallAmount() {
            // Arrange
            double amount = 0.01;
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.success());

            // Act
            boolean result = paymentProcessor.processPayment(amount);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Ska hantera mycket stora belopp korrekt")
        void shouldHandleVeryLargeAmount() {
            // Arrange
            double amount = 999999.99;
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.success());

            // Act
            boolean result = paymentProcessor.processPayment(amount);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Ska hantera decimala belopp korrekt")
        void shouldHandleDecimalAmounts() {
            // Arrange
            double amount = 99.99;
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.success());

            // Act
            boolean result = paymentProcessor.processPayment(amount);

            // Assert
            assertThat(result).isTrue();
            verify(paymentRepository).savePayment(amount, "SUCCESS");
        }

        @Test
        @DisplayName("Ska hantera runda belopp korrekt")
        void shouldHandleRoundAmounts() {
            // Arrange
            double amount = 100.0;
            when(paymentGateway.charge(amount)).thenReturn(PaymentApiResponse.success());

            // Act
            boolean result = paymentProcessor.processPayment(amount);

            // Assert
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Mock Verification Tests")
    class MockVerificationTests {

        @Test
        @DisplayName("Ska inte interagera med dependencies vid ogiltigt belopp")
        void shouldNotInteractWithDependenciesOnInvalidAmount() {
            // Act
            paymentProcessor.processPayment(-10.0);

            // Assert
            verifyNoInteractions(paymentGateway, paymentRepository, emailService);
        }

        @Test
        @DisplayName("Ska verifiera exakt antal anrop till varje dependency")
        void shouldVerifyExactNumberOfCallsToEachDependency() {
            // Arrange
            when(paymentGateway.charge(100.0)).thenReturn(PaymentApiResponse.success());

            // Act
            paymentProcessor.processPayment(100.0);

            // Assert
            verify(paymentGateway, times(1)).charge(100.0);
            verify(paymentRepository, times(1)).savePayment(100.0, "SUCCESS");
            verify(emailService, times(1)).sendPaymentConfirmation("user@example.com", 100.0);

            verifyNoMoreInteractions(paymentGateway, paymentRepository, emailService);
        }

        @Test
        @DisplayName("Ska aldrig anropa savePayment vid misslyckad gateway-betalning")
        void shouldNeverCallSavePaymentOnGatewayFailure() {
            // Arrange
            when(paymentGateway.charge(anyDouble())).thenReturn(PaymentApiResponse.failure("Error"));

            // Act
            paymentProcessor.processPayment(100.0);

            // Assert
            verify(paymentRepository, never()).savePayment(anyDouble(), anyString());
        }
    }
}