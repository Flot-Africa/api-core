package africa.flot.infrastructure.resource.rest;

import africa.flot.application.dto.command.*;
import africa.flot.application.service.FlotLoanService;
import africa.flot.application.service.UnpaidManagementService;
import africa.flot.infrastructure.security.SecurityService;
import africa.flot.infrastructure.util.ApiResponseBuilder;
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

import java.util.UUID;

/**
 * The FlotLoanResource class provides a RESTful API for managing loans, payments, and related operations.
 * It supports administrative and subscriber roles, allowing actions such as loan creation, payment processing,
 * overdue loan management, and KPI calculations.
 *
 * Fields:
 * - AUDIT_LOG: Log for auditing activities.
 * - ERROR_LOG: Log for error-related activities.
 * - BUSINESS_LOG: Log for business-related information.
 * - flotLoanService: Service managing loan operations.
 * - unpaidManagementService: Service for handling unpaid loans and related processes.
 * - securityService: Service for handling security and authorization.
 *
 * Methods:
 * - createLoan: Creates a new loan for a given lead and vehicle.
 * - processPayment: Processes a payment for a specific loan.
 * - getLoanDetails: Retrieves the full details of a specific loan.
 * - getLoansByLead: Lists all loans associated with a specific lead.
 * - getOverdueLoans: Lists all overdue loans.
 * - sendReminder: Sends a manual reminder for a loan.
 * - getUnpaidKPIs: Calculates and retrieves KPIs related to unpaid loans.
 * - processOverdueLoans: Forces the processing of overdue loans.
 * - sendAutomaticReminders: Triggers the automatic sending of reminders.
 * - getAllLoans: Retrieves all loans with pagination.
 * - getPaymentSchedule: Retrieves the payment schedule for a specific loan.
 * - getPaymentsByLead: Lists all payments associated with a specific lead.
 */
@Path("/loans-v2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Flot Loans", description = "APIs pour la gestion des prêts Flot")
public class FlotLoanResource {

    private static final Logger AUDIT_LOG = Logger.getLogger("AUDIT");
    private static final Logger ERROR_LOG = Logger.getLogger("ERROR");
    private static final Logger BUSINESS_LOG = Logger.getLogger("BUSINESS");

    @Inject
    FlotLoanService flotLoanService;

    @Inject
    UnpaidManagementService unpaidManagementService;

    @Inject
    SecurityService securityService;

    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Créer un nouveau prêt", description = "Crée un prêt pour un lead et véhicule donnés")
    @APIResponse(responseCode = "201", description = "Prêt créé avec succès")
    @APIResponse(responseCode = "400", description = "Données invalides")
    @APIResponse(responseCode = "404", description = "Lead ou véhicule introuvable")
    public Uni<Response> createLoan(@Valid CreateLoanCommand command) {
        BUSINESS_LOG.infof("Création d'un prêt pour lead %s, véhicule %s",
                command.getLeadId(), command.getVehicleId());

        return flotLoanService.createLoan(command)
                .map(loan -> {
                    AUDIT_LOG.infof("Prêt créé - ID: %s, Lead: %s", loan.getId(), command.getLeadId());
                    return ApiResponseBuilder.success(loan, Response.Status.CREATED);
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.errorf("Erreur lors de la création du prêt: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.BAD_REQUEST
                    );
                });
    }


    @GET
    @Path("/{loanId}")
    @RolesAllowed({"ADMIN", "SUBSCRIBER"})
    @Operation(summary = "Détails d'un prêt", description = "Récupère les détails complets d'un prêt")
    @APIResponse(responseCode = "200", description = "Détails du prêt")
    @APIResponse(responseCode = "404", description = "Prêt introuvable")
    public Uni<Response> getLoanDetails(
            @Parameter(description = "ID du prêt") @PathParam("loanId") UUID loanId) {

        BUSINESS_LOG.debugf("Récupération détails prêt %s", loanId);

        return securityService.validateLeadAccess(loanId.toString())
                .chain(() -> flotLoanService.getLoanDetails(loanId))
                .map(details -> {
                    AUDIT_LOG.infof("Consultation détails prêt - ID: %s", loanId);
                    return ApiResponseBuilder.success(details);
                })
                .onFailure(NotFoundException.class).recoverWithItem(throwable -> {
                    ERROR_LOG.warnf("Prêt introuvable: %s", loanId);
                    return ApiResponseBuilder.failure("Prêt introuvable", Response.Status.NOT_FOUND);
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.errorf("Erreur lors de la récupération du prêt: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            "Erreur lors de la récupération",
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    @GET
    @Path("/lead/{leadId}")
    @RolesAllowed({"ADMIN", "SUBSCRIBER"})
    @Operation(summary = "Prêts d'un lead", description = "Liste tous les prêts d'un lead")
    @APIResponse(responseCode = "200", description = "Liste des prêts")
    public Uni<Response> getLoansByLead(
            @Parameter(description = "ID du lead") @PathParam("leadId") UUID leadId) {

        BUSINESS_LOG.debugf("Récupération prêts pour lead %s", leadId);

        return securityService.validateLeadAccess(leadId.toString())
                .chain(() -> flotLoanService.getLoansByLead(leadId))
                .map(loans -> {
                    AUDIT_LOG.infof("Consultation prêts lead - Lead: %s, Nombre: %d", leadId, loans.size());
                    return ApiResponseBuilder.success(loans);
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.errorf("Erreur lors de la récupération des prêts: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            "Erreur lors de la récupération",
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    @GET
    @Path("/overdue")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Prêts en retard", description = "Liste tous les prêts en retard")
    @APIResponse(responseCode = "200", description = "Liste des prêts en retard")
    public Uni<Response> getOverdueLoans() {
        BUSINESS_LOG.info("Récupération prêts en retard");

        return flotLoanService.getOverdueLoans()
                .map(loans -> {
                    AUDIT_LOG.infof("Consultation prêts en retard - Nombre: %d", loans.size());
                    return ApiResponseBuilder.success(loans);
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.errorf("Erreur lors de la récupération des prêts en retard: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            "Erreur lors de la récupération",
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    @POST
    @Path("/{loanId}/reminders")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Envoyer une relance", description = "Envoie une relance manuelle pour un prêt")
    @APIResponse(responseCode = "200", description = "Relance envoyée")
    @APIResponse(responseCode = "404", description = "Prêt introuvable")
    public Uni<Response> sendReminder(
            @Parameter(description = "ID du prêt") @PathParam("loanId") UUID loanId,
            @Valid SendReminderCommand command) {

        command.setLoanId(loanId);
        BUSINESS_LOG.infof("Envoi relance %s niveau %s pour prêt %s",
                command.getType(), command.getLevel(), loanId);

        return unpaidManagementService.sendReminder(
                        loanId, command.getType(), command.getLevel(), command.getCustomMessage())
                .map(reminder -> {
                    AUDIT_LOG.infof("Relance envoyée - ID: %s, Type: %s",
                            reminder.getId(), reminder.getType());
                    return ApiResponseBuilder.success(reminder);
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.errorf("Erreur lors de l'envoi de la relance: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.BAD_REQUEST
                    );
                });
    }

    @GET
    @Path("/kpis/unpaid")
    @RolesAllowed("ADMIN")
    @Operation(summary = "KPIs des impayés", description = "Calcule et retourne les KPIs des impayés")
    @APIResponse(responseCode = "200", description = "KPIs calculés")
    public Uni<Response> getUnpaidKPIs() {
        BUSINESS_LOG.info("Calcul des KPIs d'impayés");

        return unpaidManagementService.calculateUnpaidKPIs()
                .map(kpis -> {
                    AUDIT_LOG.info("Consultation KPIs impayés");
                    return ApiResponseBuilder.success(kpis);
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.errorf("Erreur lors du calcul des KPIs: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            "Erreur lors du calcul",
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    @POST
    @Path("/maintenance/process-overdue")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Traiter les impayés", description = "Force le traitement des prêts en retard")
    @APIResponse(responseCode = "200", description = "Traitement effectué")
    public Uni<Response> processOverdueLoans() {
        BUSINESS_LOG.info("Traitement manuel des prêts en retard");

        return flotLoanService.processOverdueLoans()
                .map(v -> {
                    AUDIT_LOG.info("Traitement manuel des impayés effectué");
                    return ApiResponseBuilder.success("Traitement effectué avec succès");
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.errorf("Erreur lors du traitement des impayés: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    @POST
    @Path("/maintenance/send-reminders")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Envoyer relances auto", description = "Force l'envoi des relances automatiques")
    @APIResponse(responseCode = "200", description = "Relances envoyées")
    public Uni<Response> sendAutomaticReminders() {
        BUSINESS_LOG.info("Envoi manuel des relances automatiques");

        return unpaidManagementService.sendAutomaticReminders()
                .map(v -> {
                    AUDIT_LOG.info("Envoi manuel des relances effectué");
                    return ApiResponseBuilder.success("Relances envoyées avec succès");
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.errorf("Erreur lors de l'envoi des relances: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            throwable.getMessage(),
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    @GET
    @RolesAllowed("ADMIN")
    @Operation(summary = "Récupérer tous les prêts", description = "Liste tous les prêts avec pagination")
    @APIResponse(responseCode = "200", description = "Liste des prêts")
    public Uni<Response> getAllLoans(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        BUSINESS_LOG.info("Récupération de tous les prêts");

        return flotLoanService.getAllLoans(page, size)
                .map(loans -> {
                    AUDIT_LOG.info("Liste des prêts récupérée - Nombre: " + loans.size());
                    return ApiResponseBuilder.success(loans);
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.errorf("Erreur lors de la récupération des prêts: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            "Erreur lors de la récupération",
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    @GET
    @Path("/{loanId}/payment-schedule")
    @RolesAllowed({"ADMIN", "SUBSCRIBER"})
    @Operation(summary = "Récupérer le calendrier de paiement", description = "Récupère les montants à payer aujourd'hui et à venir")
    @APIResponse(responseCode = "200", description = "Calendrier de paiement")
    @APIResponse(responseCode = "404", description = "Prêt introuvable")
    public Uni<Response> getPaymentSchedule(
            @Parameter(description = "ID du prêt") @PathParam("loanId") UUID loanId) {

        BUSINESS_LOG.debugf("Récupération du calendrier de paiement pour le prêt %s", loanId);

        return securityService.validateLeadAccess(loanId.toString())
                .chain(() -> flotLoanService.getPaymentSchedule(loanId))
                .map(schedule -> {
                    AUDIT_LOG.infof("Calendrier de paiement récupéré - ID: %s", loanId);
                    return ApiResponseBuilder.success(schedule);
                })
                .onFailure(NotFoundException.class).recoverWithItem(throwable -> {
                    ERROR_LOG.warnf("Prêt introuvable: %s", loanId);
                    return ApiResponseBuilder.failure("Prêt introuvable", Response.Status.NOT_FOUND);
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.errorf("Erreur lors de la récupération du calendrier: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            "Erreur lors de la récupération",
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }

    @GET
    @Path("/lead/{leadId}/payments")
    @RolesAllowed({"ADMIN", "SUBSCRIBER"})
    @Operation(summary = "Paiements d'un lead", description = "Liste tous les paiements d'un lead")
    @APIResponse(responseCode = "200", description = "Liste des paiements")
    public Uni<Response> getPaymentsByLead(
            @Parameter(description = "ID du lead") @PathParam("leadId") UUID leadId) {

        BUSINESS_LOG.debugf("Récupération des paiements pour le lead %s", leadId);

        return securityService.validateLeadAccess(leadId.toString())
                .chain(() -> flotLoanService.getPaymentsByLead(leadId))
                .map(payments -> {
                    AUDIT_LOG.infof("Paiements récupérés - Lead: %s, Nombre: %d", leadId, payments.size());
                    return ApiResponseBuilder.success(payments);
                })
                .onFailure().recoverWithItem(throwable -> {
                    ERROR_LOG.errorf("Erreur lors de la récupération des paiements: %s", throwable.getMessage());
                    return ApiResponseBuilder.failure(
                            "Erreur lors de la récupération",
                            Response.Status.INTERNAL_SERVER_ERROR
                    );
                });
    }
}