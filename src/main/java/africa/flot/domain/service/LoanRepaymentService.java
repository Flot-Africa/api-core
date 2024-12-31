package africa.flot.domain.service;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;

public interface LoanRepaymentService {
    /**
     * Effectue un remboursement pour un prêt donné
     * @param loanExternalId Identifiant externe du prêt
     * @param amount Montant du remboursement (optionnel, utilisera le montant suggéré si null)
     * @return Réponse du remboursement
     */
    Uni<Response> makeRepayment(String loanExternalId, BigDecimal amount);

    /**
     * Récupère les détails du prochain remboursement
     * @param loanExternalId Identifiant externe du prêt
     * @return Détails du prochain remboursement
     */
    Uni<Response> getNextRepaymentDetails(String loanExternalId);
}
