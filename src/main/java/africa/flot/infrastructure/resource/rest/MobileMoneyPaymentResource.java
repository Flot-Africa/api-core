package africa.flot.infrastructure.resource.rest;

import africa.flot.application.dto.command.MobileMoneyPaymentCommand;
import africa.flot.application.service.Hub2PaymentService;
import africa.flot.domain.model.FlotLoan;
import africa.flot.domain.model.LeadPaymentIntent;
import africa.flot.domain.model.enums.TransactionStatus;
import africa.flot.infrastructure.security.SecurityService;
import africa.flot.infrastructure.util.ApiResponseBuilder;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Path("/loans-v2/{loanId}/mobile-money-payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Mobile Money Payments", description = "APIs pour les paiements via Mobile Money")
public class MobileMoneyPaymentResource {

    private static final Logger LOG = Logger.getLogger(MobileMoneyPaymentResource.class);

    @Inject
    Hub2PaymentService hub2PaymentService;

    @Inject
    SecurityService securityService;

    @POST
    @RolesAllowed({"ADMIN", "SUBSCRIBER"})
    @Operation(summary = "Initier un paiement Mobile Money",
            description = "Démarre une transaction de paiement via Mobile Money (Orange, MTN, etc.)")
    @APIResponse(responseCode = "202", description = "Paiement initié avec succès")
    @APIResponse(responseCode = "400", description = "Données invalides")
    @APIResponse(responseCode = "404", description = "Prêt introuvable")
    public Uni<Response> initiatePayment(
            @Parameter(description = "ID du prêt") @PathParam("loanId") UUID loanId,
            @Valid MobileMoneyPaymentCommand command) {

        command.setLoanId(loanId);
        LOG.infof("Initiation paiement Mobile Money %.2f pour prêt %s avec %s",
                command.getAmount(), loanId, command.getProvider());

        return securityService.validateLeadAccess(loanId.toString())
                .chain(() -> hub2PaymentService.initiatePayment(command))
                .map(result -> {
                    LOG.infof("Paiement Mobile Money initié - ID: %s", result.getPaymentIntentId());

                    if (result.getTransactionStatus() == TransactionStatus.PENDING) {
                        return ApiResponseBuilder.success(Map.of(
                                "status", "pending",
                                "paymentIntentId", result.getPaymentIntentId(),
                                "paymentIntentToken", result.getPaymentIntentToken(),
                                "message", "Paiement initié. Veuillez valider la transaction sur votre téléphone."
                        ), Response.Status.ACCEPTED);
                    } else if (result.getTransactionStatus() == TransactionStatus.COMPLETED) {
                        return ApiResponseBuilder.success(Map.of(
                                "status", "completed",
                                "paymentIntentId", result.getPaymentIntentId(),
                                "message", "Paiement traité avec succès."
                        ));
                    } else {
                        return ApiResponseBuilder.success(Map.of(
                                "status", result.getTransactionStatus().toString().toLowerCase(),
                                "paymentIntentId", result.getPaymentIntentId(),
                                "paymentIntentToken", result.getPaymentIntentToken(),
                                "message", "Statut du paiement: " + result.getTransactionStatus()
                        ), Response.Status.ACCEPTED);
                    }
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.errorf("Erreur lors de l'initiation du paiement: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.BAD_REQUEST
                    );
                });
    }

    @POST
    @Path("/{paymentIntentId}/complete")
    @RolesAllowed({"ADMIN", "SUBSCRIBER"})
    @Operation(summary = "Compléter un paiement avec OTP",
            description = "Finalise une transaction Mobile Money avec un code OTP")
    @APIResponse(responseCode = "200", description = "Paiement complété avec succès")
    @APIResponse(responseCode = "400", description = "Données invalides ou OTP incorrect")
    public Uni<Response> completePayment(
            @Parameter(description = "ID du prêt") @PathParam("loanId") UUID loanId,
            @Parameter(description = "ID du payment intent") @PathParam("paymentIntentId") String paymentIntentId,
            @QueryParam("token") String token,
            @QueryParam("otp") String otp) {

        LOG.infof("Finalisation paiement Mobile Money pour prêt %s, intent: %s", loanId, paymentIntentId);

        MobileMoneyPaymentCommand command = new MobileMoneyPaymentCommand();
        command.setLoanId(loanId);
        command.setPaymentIntentId(paymentIntentId);
        command.setPaymentIntentToken(token);
        command.setOtp(otp);

        return securityService.validateLeadAccess(loanId.toString())
                .chain(() -> hub2PaymentService.completePayment(command))
                .map(result -> {
                    LOG.infof("Paiement Mobile Money finalisé - statut: %s", result.getTransactionStatus());

                    if (result.getTransactionStatus() == TransactionStatus.COMPLETED) {
                        return ApiResponseBuilder.success(Map.of(
                                "status", "completed",
                                "message", "Paiement traité avec succès."
                        ));
                    } else {
                        return ApiResponseBuilder.success(Map.of(
                                "status", result.getTransactionStatus().toString().toLowerCase(),
                                "message", "Statut du paiement: " + result.getTransactionStatus()
                        ), Response.Status.ACCEPTED);
                    }
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.errorf("Erreur lors de la finalisation du paiement: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.BAD_REQUEST
                    );
                });
    }

    @GET
    @Path("/{paymentIntentId}/status")
    @RolesAllowed({"ADMIN", "SUBSCRIBER"})
    @Operation(summary = "Vérifier le statut d'un paiement",
            description = "Vérifie le statut actuel d'une transaction Mobile Money")
    @APIResponse(responseCode = "200", description = "Statut du paiement")
    public Uni<Response> checkPaymentStatus(
            @Parameter(description = "ID du prêt") @PathParam("loanId") UUID loanId,
            @Parameter(description = "ID du payment intent") @PathParam("paymentIntentId") String paymentIntentId,
            @QueryParam("token") String token) {

        LOG.infof("Vérification statut paiement %s pour prêt %s", paymentIntentId, loanId);

        return securityService.validateLeadAccess(loanId.toString())
                .chain(() -> hub2PaymentService.checkPaymentStatus(paymentIntentId, token))
                .map(status -> {
                    LOG.infof("Statut du paiement récupéré: %s", status.get("status"));
                    return ApiResponseBuilder.success(status);
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.errorf("Erreur lors de la vérification du statut: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    @GET
    @WithSession
    @RolesAllowed({"ADMIN", "SUBSCRIBER"})
    @Operation(summary = "Obtenir les intents de paiement",
            description = "Récupère les intents de paiement Mobile Money pour un prêt")
    @APIResponse(responseCode = "200", description = "Liste des intents de paiement")
    public Uni<Response> getPaymentIntents(
            @Parameter(description = "ID du prêt") @PathParam("loanId") UUID loanId) {

        LOG.infof("Récupération des intents de paiement pour le prêt %s", loanId);

        return securityService.validateLeadAccess(loanId.toString())
                .chain(() -> {
                    // Récupérer le prêt pour obtenir le leadId
                    return FlotLoan.<FlotLoan>findById(loanId)
                            .flatMap(loan -> {
                                // Récupérer les intents de paiement actifs
                                return LeadPaymentIntent.<LeadPaymentIntent>find(
                                                "loanId = ?1 AND active = true ORDER BY createdAt DESC", loanId)
                                        .list();
                            });
                })
                .map(intents -> {
                    LOG.infof("Intents récupérés: %d", intents.size());
                    return ApiResponseBuilder.success(intents);
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.errorf("Erreur lors de la récupération des intents: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    @POST
    @Path("/amount-due")
    @RolesAllowed({"ADMIN", "SUBSCRIBER"})
    @Operation(summary = "Calculer le montant dû",
            description = "Calcule le montant dû pour le paiement du prêt")
    @APIResponse(responseCode = "200", description = "Montant calculé")
    public Uni<Response> calculateAmountDue(
            @Parameter(description = "ID du prêt") @PathParam("loanId") UUID loanId) {

        LOG.infof("Calcul du montant dû pour le prêt %s", loanId);

        return securityService.validateLeadAccess(loanId.toString())
                .chain(() -> FlotLoan.<FlotLoan>findById(loanId))
                .map(loan -> {
                    // Calculer le montant dû (échéance hebdomadaire ou montant en impayé si supérieur)
                    BigDecimal amountDue = loan.getWeeklyAmount();
                    if (loan.getOverdueAmount().compareTo(BigDecimal.ZERO) > 0) {
                        amountDue = loan.getOverdueAmount().max(amountDue);
                    }

                    // Limiter au montant restant à payer
                    amountDue = amountDue.min(loan.getOutstanding());

                    LOG.infof("Montant dû calculé: %.2f", amountDue);

                    return ApiResponseBuilder.success(Map.of(
                            "amountDue", amountDue,
                            "currency", "XOF",
                            "weeklyAmount", loan.getWeeklyAmount(),
                            "overdueAmount", loan.getOverdueAmount(),
                            "outstanding", loan.getOutstanding()
                    ));
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.errorf("Erreur lors du calcul du montant dû: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }
}