package africa.flot.application.service;

import africa.flot.domain.model.*;
import africa.flot.domain.model.enums.*;
import africa.flot.application.dto.response.LoanDetailsDTO;
import africa.flot.application.dto.command.CreateLoanCommand;
import africa.flot.application.dto.command.ProcessPaymentCommand;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class responsible for managing flot loans.
 * Provides operations for creating loans, processing payments, retrieving loan details,
 * and handling overdue loans.
 *
 * This class is part of a transactional system and integrates with the unpaid management service.
 */
@ApplicationScoped
public class FlotLoanService {

    private static final Logger LOG = Logger.getLogger(FlotLoanService.class);
    private static final int LOAN_DURATION_MONTHS = 36;
    private static final int WEEKS_PER_MONTH = 4;
    private static final int TOTAL_WEEKS = LOAN_DURATION_MONTHS * WEEKS_PER_MONTH; // 144 semaines

    @Inject
    UnpaidManagementService unpaidManagementService;

    @WithTransaction
    public Uni<FlotLoan> createLoan(CreateLoanCommand command) {
        LOG.infof("Création d'un nouveau prêt pour le lead %s, véhicule %s",
                command.getLeadId(), command.getVehicleId());

        return Vehicle.<Vehicle>findById(command.getVehicleId())
                .onItem().ifNull().failWith(() ->
                        new IllegalArgumentException("Véhicule introuvable: " + command.getVehicleId()))
                .flatMap(vehicle -> {
                    FlotLoan loan = new FlotLoan();
                    loan.setId(UUID.randomUUID());
                    loan.setLeadId(command.getLeadId());
                    loan.setVehicleId(command.getVehicleId());
                    loan.setPrincipal(vehicle.getPrice());

                    // Calcul du montant hebdomadaire (prix / 144 semaines)
                    BigDecimal weeklyAmount = vehicle.getPrice()
                            .divide(BigDecimal.valueOf(TOTAL_WEEKS), 2, RoundingMode.HALF_UP);
                    loan.setWeeklyAmount(weeklyAmount);

                    // Dates
                    loan.setStartDate(LocalDate.now());
                    loan.setEndDate(LocalDate.now().plusMonths(LOAN_DURATION_MONTHS));
                    loan.setNextDueDate(LocalDate.now().plusWeeks(1));

                    // État initial
                    loan.setStatus(LoanStatus.ACTIVE);
                    loan.setOutstanding(vehicle.getPrice());
                    loan.setUnpaidStatus(UnpaidStatus.ON_TIME);

                    LOG.infof("Prêt créé: principal=%.2f€, montant hebdomadaire=%.2f€",
                            loan.getPrincipal(), loan.getWeeklyAmount());

                    return loan.persist();
                });
    }

    @WithTransaction
    public Uni<LoanPayment> processPayment(ProcessPaymentCommand command) {
        LOG.infof("Traitement d'un paiement de %.2f€ pour le prêt %s",
                command.getAmount(), command.getLoanId());

        return FlotLoan.<FlotLoan>findById(command.getLoanId())
                .onItem().ifNull().failWith(() ->
                        new IllegalArgumentException("Prêt introuvable: " + command.getLoanId()))
                .flatMap(loan -> {
                    // Validation
                    if (loan.getStatus() != LoanStatus.ACTIVE) {
                        return Uni.createFrom().failure(
                                new IllegalStateException("Le prêt n'est pas actif"));
                    }

                    // Création du paiement
                    LoanPayment payment = new LoanPayment();
                    payment.setId(UUID.randomUUID());
                    payment.setLoanId(loan.getId());
                    payment.setAmount(BigDecimal.valueOf(command.getAmount()));
                    payment.setPaymentDate(LocalDate.now());
                    payment.setDueDate(loan.getNextDueDate());
                    payment.setPaymentMethod(command.getPaymentMethod());
                    payment.setExternalReference(command.getExternalReference());
                    payment.setNotes(command.getNotes());
                    payment.setCreatedBy(command.getCreatedBy());

                    // Informations Mobile Money
                    if (PaymentMethod.MOBILE_MONEY.equals(command.getPaymentMethod())) {
                        payment.setPaymentProvider(command.getPaymentProvider());
                        payment.setPaymentPhoneNumber(command.getPaymentPhoneNumber());
                        payment.setPaymentIntentId(command.getPaymentIntentId());
                        payment.setPaymentTransactionId(command.getExternalReference());
                    }

                    // Mise à jour du prêt
                    loan.setTotalPaid(loan.getTotalPaid().add(BigDecimal.valueOf(command.getAmount())));
                    loan.setOutstanding(loan.getOutstanding().subtract(BigDecimal.valueOf(command.getAmount())));
                    loan.setLastPaymentDate(LocalDate.now());

                    // Calcul de la prochaine échéance
                    updateNextDueDate(loan);

                    // Mise à jour du statut des impayés
                    updateUnpaidStatus(loan);

                    // Vérification si le prêt est terminé
                    if (loan.getOutstanding().compareTo(BigDecimal.ZERO) <= 0) {
                        loan.setStatus(LoanStatus.COMPLETED);
                        loan.setUnpaidStatus(UnpaidStatus.ON_TIME);
                        LOG.infof("Prêt %s terminé!", loan.getId());
                    }

                    return Uni.combine().all().unis(
                            payment.<LoanPayment>persistAndFlush(),
                            loan.persistAndFlush()
                    ).asTuple().map(tuple -> {
                        LOG.infof("Paiement traité: nouveau solde=%.2f€", loan.getOutstanding());

                        // Notifier les systèmes externes si nécessaire
                        if (PaymentMethod.MOBILE_MONEY.equals(command.getPaymentMethod())) {
                            LOG.infof("Paiement Mobile Money %s enregistré: provider=%s, téléphone=%s",
                                    payment.getId(), payment.getPaymentProvider(), payment.getPaymentPhoneNumber());
                        }

                        return tuple.getItem1(); // Retourne le LoanPayment
                    });
                });
    }

    @WithSession
    public Uni<LoanDetailsDTO> getLoanDetails(UUID loanId) {
        return FlotLoan.<FlotLoan>findById(loanId)
                .onItem().ifNull().failWith(() ->
                        new IllegalArgumentException("Prêt introuvable: " + loanId))
                .flatMap(loan ->
                        LoanPayment.<LoanPayment>list("loanId = ?1 ORDER BY paymentDate DESC", loanId)
                                .map(payments -> createLoanDetailsDTO(loan, payments))
                );
    }

    @WithSession
    public Uni<List<FlotLoan>> getActiveLoans() {
        return FlotLoan.<FlotLoan>list("status = ?1", LoanStatus.ACTIVE);
    }

    @WithSession
    public Uni<List<FlotLoan>> getOverdueLoans() {
        LocalDate today = LocalDate.now();
        return FlotLoan.<FlotLoan>list(
                "status = ?1 AND nextDueDate < ?2 AND outstanding > 0",
                LoanStatus.ACTIVE, today
        );
    }

    @WithTransaction
    public Uni<Void> processOverdueLoans() {
        LOG.info("Traitement des prêts en retard...");

        return getOverdueLoans()
                .flatMap(overdue -> {
                    List<Uni<FlotLoan>> updates = overdue.stream()
                            .map(this::updateUnpaidStatus)
                            .toList();

                    return Uni.join().all(updates).andFailFast();
                })
                .map(results -> {
                    LOG.infof("Traité %d prêts en retard", results.size());
                    return null;
                });
    }

    private Uni<FlotLoan> updateUnpaidStatus(FlotLoan loan) {
        LocalDate today = LocalDate.now();

        if (loan.getNextDueDate().isAfter(today) || loan.getOutstanding().equals(BigDecimal.ZERO)) {
            // Prêt à jour
            loan.setUnpaidStatus(UnpaidStatus.ON_TIME);
            loan.setDaysOverdue(0);
            loan.setWeeksOverdue(0);
            loan.setOverdueAmount(BigDecimal.ZERO);
        } else {
            // Calcul du retard
            long daysOverdue = ChronoUnit.DAYS.between(loan.getNextDueDate(), today);
            int weeksOverdue = (int) (daysOverdue / 7);

            loan.setDaysOverdue((int) daysOverdue);
            loan.setWeeksOverdue(weeksOverdue);

            // Calcul du montant en impayé (nombre de semaines * montant hebdomadaire)
            BigDecimal overdueAmount = loan.getWeeklyAmount()
                    .multiply(BigDecimal.valueOf(weeksOverdue + 1));
            loan.setOverdueAmount(overdueAmount.min(loan.getOutstanding()));

            // Mise à jour du statut selon le niveau de retard
            if (daysOverdue <= 7) {
                loan.setUnpaidStatus(UnpaidStatus.EN_RETARD);
            } else {
                // Le statut plus précis sera géré par UnpaidManagementService
                // basé sur les relances envoyées
                if (loan.getUnpaidStatus() == UnpaidStatus.ON_TIME) {
                    loan.setUnpaidStatus(UnpaidStatus.EN_RETARD);
                }
            }
        }

        return loan.persistAndFlush();
    }

    private void updateNextDueDate(FlotLoan loan) {
        // Si paiement complet, avancer d'une semaine
        if (loan.getLastPaymentDate() != null) {
            // Calculer combien de paiements complets ont été effectués
            int completedPayments = loan.getTotalPaid()
                    .divide(loan.getWeeklyAmount(), 0, RoundingMode.DOWN)
                    .intValue();

            // Nouvelle échéance = date de début + (paiements completés + 1) semaines
            loan.setNextDueDate(
                    loan.getStartDate().plusWeeks(completedPayments + 1)
            );
        }
    }

    private LoanDetailsDTO createLoanDetailsDTO(FlotLoan loan, List<LoanPayment> payments) {
        LoanDetailsDTO dto = new LoanDetailsDTO();

        // Informations de base
        dto.setLoanId(loan.getId());
        dto.setLeadId(loan.getLeadId());
        dto.setVehicleId(loan.getVehicleId());
        dto.setPrincipal(loan.getPrincipal());
        dto.setWeeklyAmount(loan.getWeeklyAmount());
        dto.setStartDate(loan.getStartDate());
        dto.setEndDate(loan.getEndDate());
        dto.setStatus(loan.getStatus());

        // Progression du paiement
        dto.setTotalPaid(loan.getTotalPaid());
        dto.setOutstanding(loan.getOutstanding());
        dto.setPaymentProgress(loan.getPaymentProgress());
        dto.setCompletedPayments(loan.getCompletedPayments());
        dto.setRemainingPayments(loan.getRemainingPayments());
        dto.setEstimatedEndDate(loan.getEstimatedEndDate());

        // Statut des échéances
        dto.setNextDueDate(loan.getNextDueDate());
        dto.setLastPaymentDate(loan.getLastPaymentDate());
        dto.setDaysOverdue(loan.getDaysOverdue());
        dto.setWeeksOverdue(loan.getWeeksOverdue());
        dto.setOverdueAmount(loan.getOverdueAmount());
        dto.setUnpaidStatus(loan.getUnpaidStatus());

        // Informations de relance
        dto.setReminderLevel(loan.getReminderLevel());
        dto.setLastReminderDate(loan.getLastReminderDate());
        dto.setNextReminderDueDate(loan.getNextReminderDueDate());

        // Historique des paiements (5 derniers)
        dto.setRecentPayments(payments.stream()
                .limit(5)
                .map(this::createPaymentSummary)
                .toList());

        return dto;
    }

    private LoanDetailsDTO.PaymentSummary createPaymentSummary(LoanPayment payment) {
        LoanDetailsDTO.PaymentSummary summary = new LoanDetailsDTO.PaymentSummary();
        summary.setPaymentId(payment.getId());
        summary.setAmount(payment.getAmount());
        summary.setPaymentDate(payment.getPaymentDate());
        summary.setDueDate(payment.getDueDate());
        summary.setStatus(payment.getStatus());
        summary.setPaymentMethod(payment.getPaymentMethod());
        summary.setDaysOverdue(payment.getDaysOverdue());
        return summary;
    }

    // Méthodes de support pour les statistiques

    @WithSession
    public Uni<BigDecimal> getTotalOutstanding() {
        return FlotLoan.find("SELECT SUM(f.outstanding) FROM FlotLoan f WHERE f.status = ?1",
                        LoanStatus.ACTIVE)
                .project(BigDecimal.class)
                .singleResult()
                .onItem().ifNull().continueWith(BigDecimal.ZERO);
    }

    @WithSession
    public Uni<BigDecimal> getTotalOverdueAmount() {
        return FlotLoan.find("SELECT SUM(f.overdueAmount) FROM FlotLoan f WHERE f.overdueAmount > 0",
                        LoanStatus.ACTIVE)
                .project(BigDecimal.class)
                .singleResult()
                .onItem().ifNull().continueWith(BigDecimal.ZERO);
    }

    @WithSession
    public Uni<Long> countActiveLoans() {
        return FlotLoan.count("status = ?1", LoanStatus.ACTIVE);
    }

    @WithSession
    public Uni<Long> countOverdueLoans() {
        LocalDate today = LocalDate.now();
        return FlotLoan.count("status = ?1 AND nextDueDate < ?2 AND outstanding > 0",
                LoanStatus.ACTIVE, today);
    }

    @WithSession
    public Uni<List<FlotLoan>> getLoansByLead(UUID leadId) {
        return FlotLoan.<FlotLoan>list("leadId = ?1 ORDER BY createdAt DESC", leadId);
    }

    @WithSession
    public Uni<Double> calculateDefaultRate() {
        return Uni.combine().all().unis(
                FlotLoan.count("status = ?1", LoanStatus.DEFAULTED),
                FlotLoan.count()
        ).asTuple().map(tuple -> {
            long defaulted = tuple.getItem1();
            long total = tuple.getItem2();
            return total > 0 ? (double) defaulted / total * 100 : 0.0;
        });
    }

    @WithSession
    public Uni<List<FlotLoan>> getAllLoans(int page, int size) {
        return FlotLoan.find("ORDER BY createdAt DESC")
                .page(page, size)
                .list();
    }

    @WithSession
    public Uni<List<LoanPayment>> getPaymentsByLead(UUID leadId) {
        // D'abord trouver tous les prêts du lead
        return FlotLoan.<FlotLoan>find("leadId = ?1", leadId)
                .list()
                .flatMap(loans -> {
                    if (loans.isEmpty()) {
                        return Uni.createFrom().item(List.<LoanPayment>of());
                    }

                    // Extraire les IDs des prêts
                    List<UUID> loanIds = loans.stream()
                            .map(FlotLoan::getId)
                            .collect(Collectors.toList());

                    // Trouver tous les paiements pour ces prêts
                    return LoanPayment.<LoanPayment>find("loanId IN ?1 ORDER BY paymentDate DESC", loanIds)
                            .list();
                });
    }

    @WithSession
    public Uni<Map<String, Object>> getPaymentSchedule(UUID loanId) {
        return FlotLoan.<FlotLoan>findById(loanId)
                .onItem().ifNull().failWith(() ->
                        new NotFoundException("Prêt introuvable: " + loanId))
                .map(loan -> {
                    Map<String, Object> schedule = new HashMap<>();

                    // Paiement du jour
                    LocalDate today = LocalDate.now();
                    BigDecimal dailyAmount = loan.getWeeklyAmount().divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);

                    schedule.put("dailyAmount", dailyAmount);
                    schedule.put("today", today);

                    // Date du prochain paiement hebdomadaire
                    schedule.put("nextWeeklyDueDate", loan.getNextDueDate());
                    schedule.put("weeklyAmount", loan.getWeeklyAmount());

                    // Générer les 4 prochaines échéances
                    List<Map<String, Object>> upcomingPayments = new ArrayList<>();
                    LocalDate nextDate = today;

                    for (int i = 0; i < 4; i++) {
                        nextDate = nextDate.plusDays(1);
                        Map<String, Object> payment = new HashMap<>();
                        payment.put("date", nextDate);
                        payment.put("amount", dailyAmount);
                        payment.put("isWeeklyDue", nextDate.equals(loan.getNextDueDate()));

                        upcomingPayments.add(payment);
                    }

                    schedule.put("upcomingPayments", upcomingPayments);
                    return schedule;
                });
    }
}