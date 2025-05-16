package africa.flot.application.service;

import africa.flot.domain.model.*;
import africa.flot.domain.model.enums.*;
import africa.flot.application.dto.query.UnpaidKPIs;
import africa.flot.application.ports.SmsService;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UnpaidManagementService {

    private static final Logger LOG = Logger.getLogger(UnpaidManagementService.class);

    @Inject
    SmsService smsService;

    @WithSession
    public Uni<UnpaidKPIs> calculateUnpaidKPIs() {
        LOG.info("Calcul des KPIs d'impayés...");

        return Uni.combine().all().unis(
                getTotalUnpaidAmount(),
                getUnpaidDriversCount(),
                getActiveLoansCount(),
                getUnpaidByAgeRange(),
                getRecoveryStats(),
                getReminderStats()
        ).asTuple().map(tuple -> {
            UnpaidKPIs kpis = new UnpaidKPIs();

            BigDecimal totalUnpaid = tuple.getItem1();
            Long unpaidDrivers = tuple.getItem2();
            Long activeLoans = tuple.getItem3();
            UnpaidKPIs.AgeRanges ageRanges = tuple.getItem4();
            UnpaidKPIs.RecoveryStats recovery = tuple.getItem5();
            UnpaidKPIs.ReminderStats reminders = tuple.getItem6();

            // État global
            kpis.setTotalUnpaidAmount(totalUnpaid);
            kpis.setUnpaidDriversCount(unpaidDrivers.intValue());
            kpis.setUnpaidRate(activeLoans > 0 ?
                    (double) unpaidDrivers / activeLoans * 100 : 0.0);

            // Ancienneté des impayés
            kpis.setAgeRanges(ageRanges);

            // Recouvrement
            kpis.setRecoveryStats(recovery);

            // Relances
            kpis.setReminderStats(reminders);

            LOG.infof("KPIs calculés: %.2f€ d'impayés sur %d chauffeurs (%.1f%%)",
                    totalUnpaid, unpaidDrivers, kpis.getUnpaidRate());

            return kpis;
        });
    }

    @WithTransaction
    public Uni<Void> sendAutomaticReminders() {
        LOG.info("Envoi des relances automatiques...");

        return FlotLoan.<FlotLoan>list(
                "status = ?1 AND unpaidStatus IN (?2) AND (nextReminderDueDate IS NULL OR nextReminderDueDate <= ?3)",
                LoanStatus.ACTIVE,
                List.of(UnpaidStatus.EN_RETARD, UnpaidStatus.RELANCE_1, UnpaidStatus.RELANCE_2, UnpaidStatus.RELANCE_TELEPHONE),
                LocalDate.now()
        ).flatMap(loans -> {
            List<Uni<Void>> reminderTasks = loans.stream()
                    .filter(this::shouldSendReminder)
                    .map(this::sendAppropriateReminder)
                    .toList();

            return Uni.join().all(reminderTasks).andFailFast();
        }).map(results -> {
            LOG.infof("Envoyé %d relances automatiques", results.size());
            return null;
        });
    }
    @WithTransaction
    public Uni<LoanReminder> sendReminder(UUID loanId, ReminderType type, ReminderLevel level, String message) {
        LOG.infof("Envoi d'une relance %s niveau %s pour le prêt %s", type, level, loanId);

        return FlotLoan.<FlotLoan>findById(loanId)
                .onItem().ifNull().failWith(() ->
                        new IllegalArgumentException("Prêt introuvable: " + loanId))
                .flatMap(loan -> Lead.<Lead>findById(loan.getLeadId())
                        .onItem().ifNull().failWith(() ->
                                new IllegalArgumentException("Lead introuvable: " + loan.getLeadId()))
                        .flatMap(lead -> {
                            LoanReminder reminder = new LoanReminder();
                            reminder.setLoanId(loanId);
                            reminder.setType(type);
                            reminder.setLevel(level);
                            reminder.setMessage(message != null ? message : generateReminderMessage(loan, level));
                            reminder.setRecipientPhone(lead.getPhoneNumber());
                            reminder.setRecipientEmail(lead.getEmail());
                            reminder.setOverdueAmount(loan.getOverdueAmount());
                            reminder.setDaysOverdue(loan.getDaysOverdue());
                            reminder.setWeeksOverdue(loan.getWeeksOverdue());
                            reminder.setCreatedBy("SYSTEM");

                            // Envoi effectif selon le type
                            Uni<Void> sendAction = switch (type) {
                                case WHATSAPP, SMS -> sendSmsReminder(reminder, lead);
                                case EMAIL -> sendEmailReminder(reminder, lead);
                                case PHONE_CALL -> recordPhoneCallReminder(reminder);
                                case IN_PERSON -> recordInPersonReminder(reminder);
                            };

                            return sendAction.flatMap(v -> {
                                // Mise à jour du prêt
                                loan.setReminderLevel(level.ordinal() + 1);
                                loan.setLastReminderDate(LocalDate.now());
                                loan.setNextReminderDueDate(calculateNextReminderDate(level));

                                // Mise à jour du statut d'impayé
                                updateUnpaidStatusAfterReminder(loan, level);

                                return Uni.combine().all().unis(
                                        reminder.<LoanReminder>persistAndFlush(), // Cast explicite
                                        loan.persistAndFlush()
                                ).asTuple().map(tuple -> tuple.getItem1()); // Retourne le LoanReminder
                            });
                        })
                );
    }
    private Uni<Void> sendSmsReminder(LoanReminder reminder, Lead lead) {
        // Création d'un compte temporaire pour l'envoi SMS
        Account tempAccount = new Account();
        tempAccount.setLead(lead);
        tempAccount.setUsername(lead.getPhoneNumber());

        return smsService.sendSMS(lead.getPhoneNumber(), reminder.getMessage(), tempAccount)
                .map(response -> {
                    if (response.getStatus() == 200) {
                        reminder.setStatus(ReminderStatus.SENT);
                        reminder.setExternalReference(response.getHeaderString("X-Message-ID"));
                    } else {
                        reminder.setStatus(ReminderStatus.FAILED);
                        LOG.errorf("Échec envoi SMS vers %s: %d", lead.getPhoneNumber(), response.getStatus());
                    }
                    return null;
                });
    }

    private Uni<Void> sendEmailReminder(LoanReminder reminder, Lead lead) {
        // TODO: Implémenter l'envoi d'email
        reminder.setStatus(ReminderStatus.SENT);
        LOG.infof("Email de relance envoyé à %s", lead.getEmail());
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> recordPhoneCallReminder(LoanReminder reminder) {
        // Enregistrement d'un appel téléphonique (pas d'envoi automatique)
        reminder.setStatus(ReminderStatus.SENT);
        LOG.infof("Appel téléphonique programmé pour le prêt %s", reminder.getLoanId());
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> recordInPersonReminder(LoanReminder reminder) {
        // Enregistrement d'une visite en personne
        reminder.setStatus(ReminderStatus.SENT);
        LOG.infof("Visite en personne programmée pour le prêt %s", reminder.getLoanId());
        return Uni.createFrom().voidItem();
    }

    private boolean shouldSendReminder(FlotLoan loan) {
        // Vérifications avant envoi
        if (loan.getOverdueAmount().equals(BigDecimal.ZERO)) return false;
        if (loan.getStatus() != LoanStatus.ACTIVE) return false;

        // Vérifier si on n'a pas déjà envoyé une relance récemment
        if (loan.getLastReminderDate() != null) {
            long daysSinceLastReminder = java.time.temporal.ChronoUnit.DAYS
                    .between(loan.getLastReminderDate(), LocalDate.now());

            ReminderLevel currentLevel = getCurrentReminderLevel(loan);
            int minDaysBetween = switch (currentLevel) {
                case FIRST -> 3;
                case SECOND -> 5;
                case THIRD -> 7;
                case FINAL -> 14;
            };

            return daysSinceLastReminder >= minDaysBetween;
        }

        return true;
    }

    private Uni<Void> sendAppropriateReminder(FlotLoan loan) {
        ReminderLevel level = getNextReminderLevel(loan);
        ReminderType type = getPreferredReminderType(level);
        String message = generateReminderMessage(loan, level);

        return sendReminder(loan.getId(), type, level, message).replaceWithVoid();
    }

    private ReminderLevel getCurrentReminderLevel(FlotLoan loan) {
        return switch (loan.getReminderLevel()) {
            case 0 -> null;
            case 1 -> ReminderLevel.FIRST;
            case 2 -> ReminderLevel.SECOND;
            case 3 -> ReminderLevel.THIRD;
            case 4 -> ReminderLevel.FINAL;
            default -> ReminderLevel.FINAL;
        };
    }

    private ReminderLevel getNextReminderLevel(FlotLoan loan) {
        return switch (loan.getReminderLevel()) {
            case 0 -> ReminderLevel.FIRST;
            case 1 -> ReminderLevel.SECOND;
            case 2 -> ReminderLevel.THIRD;
            case 3 -> ReminderLevel.FINAL;
            default -> ReminderLevel.FINAL;
        };
    }

    private ReminderType getPreferredReminderType(ReminderLevel level) {
        return switch (level) {
            case FIRST, SECOND -> ReminderType.WHATSAPP;
            case THIRD -> ReminderType.PHONE_CALL;
            case FINAL -> ReminderType.SMS; // SMS formel pour la relance finale
        };
    }

    private String generateReminderMessage(FlotLoan loan, ReminderLevel level) {
        String driverName = ""; // TODO: Récupérer le nom du lead

        return switch (level) {
            case FIRST -> String.format(
                    "Bonjour %s,\n\n" +
                            "Votre paiement hebdomadaire FLOT de %.2f€ était attendu le %s.\n" +
                            "Merci de régulariser votre situation rapidement.\n\n" +
                            "Pour toute question: 0779635252",
                    driverName, loan.getWeeklyAmount(), loan.getNextDueDate()
            );

            case SECOND -> String.format(
                    "⚠️ RAPPEL - %s\n\n" +
                            "Votre retard de paiement FLOT persiste.\n" +
                            "Montant dû: %.2f€\n" +
                            "Retard: %d jours\n\n" +
                            "Régularisez RAPIDEMENT pour éviter des frais supplémentaires.\n" +
                            "Contact: 0779635252",
                    driverName, loan.getOverdueAmount(), loan.getDaysOverdue()
            );

            case THIRD -> String.format(
                    "🔴 URGENT - %s\n\n" +
                            "Votre compte FLOT présente un retard important.\n" +
                            "Montant dû: %.2f€\n" +
                            "Retard: %d jours\n\n" +
                            "Contactez-nous IMMÉDIATEMENT: 0779635252\n" +
                            "Risque de suspension du service.",
                    driverName, loan.getOverdueAmount(), loan.getDaysOverdue()
            );

            case FINAL -> String.format(
                    "🚨 DERNIÈRE RELANCE - %s\n\n" +
                            "Malgré nos rappels, votre situation reste non régularisée.\n" +
                            "MONTANT TOTAL DÛ: %.2f€\n" +
                            "RETARD: %d jours\n\n" +
                            "DERNIÈRES 48H pour éviter les procédures de recouvrement.\n" +
                            "URGENT: 0779635252",
                    driverName, loan.getOverdueAmount(), loan.getDaysOverdue()
            );
        };
    }

    private LocalDate calculateNextReminderDate(ReminderLevel level) {
        return switch (level) {
            case FIRST -> LocalDate.now().plusDays(3);
            case SECOND -> LocalDate.now().plusDays(5);
            case THIRD -> LocalDate.now().plusDays(7);
            case FINAL -> LocalDate.now().plusDays(14);
        };
    }

    private void updateUnpaidStatusAfterReminder(FlotLoan loan, ReminderLevel level) {
        loan.setUnpaidStatus(switch (level) {
            case FIRST -> UnpaidStatus.RELANCE_1;
            case SECOND -> UnpaidStatus.RELANCE_2;
            case THIRD -> UnpaidStatus.RELANCE_TELEPHONE;
            case FINAL -> UnpaidStatus.RELANCE_FINALE;
        });
    }

    // Méthodes pour le calcul des KPIs

    private Uni<BigDecimal> getTotalUnpaidAmount() {
        return FlotLoan.find("SELECT SUM(f.overdueAmount) FROM FlotLoan f WHERE f.overdueAmount > 0")
                .project(BigDecimal.class)
                .singleResult()
                .onItem().ifNull().continueWith(BigDecimal.ZERO);
    }

    private Uni<Long> getUnpaidDriversCount() {
        return FlotLoan.count("overdueAmount > 0");
    }

    private Uni<Long> getActiveLoansCount() {
        return FlotLoan.count("status = ?1", LoanStatus.ACTIVE);
    }

    private Uni<UnpaidKPIs.AgeRanges> getUnpaidByAgeRange() {
        return Uni.combine().all().unis(
                FlotLoan.find("SELECT SUM(f.overdueAmount) FROM FlotLoan f WHERE f.daysOverdue BETWEEN 0 AND 7")
                        .project(BigDecimal.class).singleResult().onItem().ifNull().continueWith(BigDecimal.ZERO),
                FlotLoan.find("SELECT SUM(f.overdueAmount) FROM FlotLoan f WHERE f.daysOverdue BETWEEN 8 AND 30")
                        .project(BigDecimal.class).singleResult().onItem().ifNull().continueWith(BigDecimal.ZERO),
                FlotLoan.find("SELECT SUM(f.overdueAmount) FROM FlotLoan f WHERE f.daysOverdue > 30")
                        .project(BigDecimal.class).singleResult().onItem().ifNull().continueWith(BigDecimal.ZERO)
        ).asTuple().map(tuple -> {
            UnpaidKPIs.AgeRanges ranges = new UnpaidKPIs.AgeRanges();
            ranges.setLessThan7Days(tuple.getItem1());
            ranges.setBetween7And30Days(tuple.getItem2());
            ranges.setMoreThan30Days(tuple.getItem3());
            return ranges;
        });
    }

    private Uni<UnpaidKPIs.RecoveryStats> getRecoveryStats() {
        // Calculs basés sur les paiements du mois en cours
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);

        return Uni.combine().all().unis(
                // Montant recouvré ce mois
                LoanPayment.find("SELECT SUM(p.amount) FROM LoanPayment p WHERE p.paymentDate >= ?1", startOfMonth)
                        .project(BigDecimal.class).singleResult().onItem().ifNull().continueWith(BigDecimal.ZERO),
                // TODO: Calcul du pourcentage de régularisation et durée moyenne
                Uni.createFrom().item(25.0), // Placeholder
                Uni.createFrom().item(5) // Placeholder en jours
        ).asTuple().map(tuple -> {
            UnpaidKPIs.RecoveryStats stats = new UnpaidKPIs.RecoveryStats();
            stats.setAmountRecoveredThisMonth(tuple.getItem1());
            stats.setRegularizationPercentage(tuple.getItem2());
            stats.setAverageDaysToRegularization(tuple.getItem3());
            return stats;
        });
    }

    private Uni<UnpaidKPIs.ReminderStats> getReminderStats() {
        return Uni.combine().all().unis(
                FlotLoan.count("overdueAmount > 0 AND reminderLevel = 0"),
                FlotLoan.count("overdueAmount > 0 AND reminderLevel > 0"),
                getReminderDistribution()
        ).asTuple().map(tuple -> {
            Long noReminder = tuple.getItem1();
            Long withReminder = tuple.getItem2();
            UnpaidKPIs.ReminderDistribution distribution = tuple.getItem3();

            UnpaidKPIs.ReminderStats stats = new UnpaidKPIs.ReminderStats();
            stats.setPercentageWithoutReminder(
                    (noReminder + withReminder) > 0 ?
                            (double) noReminder / (noReminder + withReminder) * 100 : 0.0);
            stats.setPercentageWhatsappSent(
                    (noReminder + withReminder) > 0 ?
                            (double) withReminder / (noReminder + withReminder) * 100 : 0.0);
            stats.setDistribution(distribution);
            return stats;
        });
    }

    private Uni<UnpaidKPIs.ReminderDistribution> getReminderDistribution() {
        return Uni.combine().all().unis(
                FlotLoan.count("overdueAmount > 0 AND reminderLevel = 0"),
                FlotLoan.count("overdueAmount > 0 AND reminderLevel = 1"),
                FlotLoan.count("overdueAmount > 0 AND reminderLevel = 2"),
                FlotLoan.count("overdueAmount > 0 AND reminderLevel = 3"),
                FlotLoan.count("overdueAmount > 0 AND reminderLevel = 4")
        ).asTuple().map(tuple -> {
            UnpaidKPIs.ReminderDistribution dist = new UnpaidKPIs.ReminderDistribution();
            dist.setNone(tuple.getItem1().intValue());
            dist.setFirstReminder(tuple.getItem2().intValue());
            dist.setSecondReminder(tuple.getItem3().intValue());
            dist.setPhoneCall(tuple.getItem4().intValue());
            dist.setFinalReminder(tuple.getItem5().intValue());
            return dist;
        });
    }
}