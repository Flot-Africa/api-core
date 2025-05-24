package africa.flot.application.dto.request;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

/**
 * @author katinan.toure 20/05/2025 11:59
 * @project api-core
 */
@Data
@RegisterForReflection
public class PaymentIntentResponse {
    private String id;
    private String token;
    private String status;
    private Long amount;
    private String currency;
    private String createdAt;
    private String updatedAt;
}
