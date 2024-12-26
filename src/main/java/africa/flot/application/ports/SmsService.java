package africa.flot.application.ports;

import africa.flot.domain.model.Account;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

public interface SmsService {
    Uni<Response> sendSMS(String phoneNumber, String message, Account account);
}