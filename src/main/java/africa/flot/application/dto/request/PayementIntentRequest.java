package africa.flot.application.dto.request;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author katinan.toure 20/05/2025 11:58
 * @project api-core
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class PayementIntentRequest {
    private String customerReference;
    private String purchaseReference;
    private BigDecimal amount; // Montant en centimes
    private String currency; // Ex: "
}
