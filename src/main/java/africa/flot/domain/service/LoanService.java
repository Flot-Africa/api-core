package africa.flot.domain.service;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LoanService {
    /**
     * Récupère un produit de prêt par ID.
     */
    Uni<Response> getLoanProduct(Integer productId);

    /**
     * Récupère un client par External ID.
     */
    Uni<Response> getClientByExternalId(UUID externalId);

    /**
     * Crée un prêt pour un client.
     */
    Uni<Response> createLoan(Integer clientId, Integer productId, BigDecimal amount, UUID externalId);

    /**
     * Récupère les détails d'un prêt pour l'application mobile.
     */
    Uni<JsonObject> getLoanDetailsForMobile(UUID externalId);

    /**
     * Récupère les détails d'un prêt pour le back-office.
     */
    Uni<JsonObject> getLoanDetailsForBackOffice(UUID externalId);

    /**
     * Récupère l'historique des paiements d'un prêt.
     */
    Uni<List<JsonObject>> getLoanRepaymentHistory(UUID externalId);
}
