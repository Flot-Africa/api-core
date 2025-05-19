package africa.flot.application.service;

import africa.flot.application.client.Hub2Client;
import africa.flot.application.dto.query.RepaymentRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class Hub2PaymentService {

    @Inject
    @RestClient
    Hub2Client hub2Client;

    @Inject
    Hub2HeadersProvider headersProvider;

    public void repayLoan(Long loanId, RepaymentRequest request) {
        Map<String, String> headers = headersProvider.getDefaultHeaders();

        // 1. Créer un PaymentIntent
        Map<String, Object> intentPayload = new HashMap<>();
        intentPayload.put("customerReference", "driver_" + loanId);
        intentPayload.put("purchaseReference", "loan_" + loanId + "_" + System.currentTimeMillis());
        intentPayload.put("amount", request.amount());
        intentPayload.put("currency", "XOF");

        var intent = hub2Client.createPaymentIntent(headers, intentPayload);

        // 2. Tenter un paiement Mobile Money
        Map<String, Object> paymentPayload = new HashMap<>();
        paymentPayload.put("token", intent.token());
        paymentPayload.put("paymentMethod", "mobile_money");
        paymentPayload.put("country", "CI");
        paymentPayload.put("provider", request.provider());

        Map<String, Object> mobileMoney = new HashMap<>();
        mobileMoney.put("msisdn", request.msisdn());
        mobileMoney.put("otp", request.otp());

        paymentPayload.put("mobileMoney", mobileMoney);

        var payment = hub2Client.payWithMobileMoney(headers, intent.id(), paymentPayload);

        // 3. Authentifier si nécessaire
        if ("action_required".equals(payment.status())) {
            Map<String, Object> authPayload = new HashMap<>();
            authPayload.put("token", intent.token());
            authPayload.put("confirmationCode", request.otp());
            hub2Client.authenticate(headers, intent.id(), authPayload);
        }

        // 4. Si succès, mettre à jour le prêt
        if ("succeeded".equals(payment.status())) {
            // appeler LoanService pour mettre à jour l'état du prêt
        }
    }
}