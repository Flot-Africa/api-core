package africa.flot.application.job;

import africa.flot.application.service.FlotLoanService;
import africa.flot.application.service.UnpaidManagementService;
import africa.flot.domain.model.FlotLoan;
import africa.flot.domain.model.LoanReminder;
import africa.flot.domain.model.enums.LoanStatus;
import africa.flot.domain.model.enums.ReminderStatus;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;

@ApplicationScoped
public class LoanMaintenanceJobs {

    private static final Logger LOG = Logger.getLogger(LoanMaintenanceJobs.class);

    @Inject
    FlotLoanService flotLoanService;

    @Inject
    UnpaidManagementService unpaidManagementService;

    // Tous les jours à 8h - Mise à jour des prêts en retard
    @Scheduled(cron = "0 0 8 * * ?", identity = "update-overdue-loans")
    public Uni<Void> updateOverdueLoans(ScheduledExecution execution) {
        LOG.info("Démarrage du job de mise à jour des prêts en retard");

        return flotLoanService.processOverdueLoans()
                .onItem().invoke(() ->
                        LOG.info("Job de mise à jour des prêts en retard terminé avec succès"))
                .onFailure().invoke(throwable ->
                        LOG.errorf(throwable, "Erreur dans le job de mise à jour des prêts en retard"));
    }

    // Tous les jours à 9h - Envoi des relances automatiques
    @Scheduled(cron = "0 0 9 * * ?", identity = "send-automatic-reminders")
    public Uni<Void> sendAutomaticReminders(ScheduledExecution execution) {
        LOG.info("Démarrage du job d'envoi des relances automatiques");

        return unpaidManagementService.sendAutomaticReminders()
                .onItem().invoke(() ->
                        LOG.info("Job d'envoi des relances automatiques terminé avec succès"))
                .onFailure().invoke(throwable ->
                        LOG.errorf(throwable, "Erreur dans le job d'envoi des relances automatiques"));
    }

    // Tous les lundis à 10h - Calcul des KPIs hebdomadaires
    @Scheduled(cron = "0 0 10 ? * MON", identity = "generate-weekly-reports")
    public Uni<Void> generateWeeklyReports(ScheduledExecution execution) {
        LOG.info("Démarrage du job de génération des rapports hebdomadaires");

        return unpaidManagementService.calculateUnpaidKPIs()
                .map(kpis -> {
                    LOG.infof("KPIs hebdomadaires calculés:");
                    LOG.infof("- Montant total impayé: %.2f€", kpis.getTotalUnpaidAmount());
                    LOG.infof("- Chauffeurs en impayé: %d", kpis.getUnpaidDriversCount());
                    LOG.infof("- Taux d'impayés: %.1f%%", kpis.getUnpaidRate());
                    return null;
                })
                .onItem().invoke(() ->
                        LOG.info("Job de génération des rapports hebdomadaires terminé avec succès"))
                .onFailure().invoke(throwable ->
                        LOG.errorf(throwable, "Erreur dans le job de génération des rapports hebdomadaires")).replaceWithVoid();
    }

    // Tous les dimanches à 23h - Nettoyage des anciennes données
    @Scheduled(cron = "0 0 23 ? * SUN", identity = "cleanup-old-data")
    public Uni<Void> cleanupOldData(ScheduledExecution execution) {
        LOG.info("Démarrage du job de nettoyage des anciennes données");

        // Supprimer les anciennes relances (plus de 6 mois)
        return LoanReminder.delete("sentAt < ?1",
                        LocalDateTime.now().minusMonths(6))
                .map(deleted -> {
                    LOG.infof("Supprimé %d anciennes relances", deleted);
                    return null;
                })
                .onItem().invoke(() ->
                        LOG.info("Job de nettoyage terminé avec succès"))
                .onFailure().invoke(throwable ->
                        LOG.errorf(throwable, "Erreur dans le job de nettoyage")).replaceWithVoid();
    }

    // Toutes les heures - Vérification des échéances du jour
    @Scheduled(cron = "0 0 * * * ?", identity = "check-daily-due-dates")
    public Uni<Void> checkDailyDueDates(ScheduledExecution execution) {
        LOG.debug("Vérification des échéances du jour");

        LocalDate today = LocalDate.now();

        return FlotLoan.<FlotLoan>list("nextDueDate = ?1 AND status = ?2",
                        today, LoanStatus.ACTIVE)
                .map(dueTodayLoans -> {
                    if (!dueTodayLoans.isEmpty()) {
                        LOG.infof("Trouvé %d prêts avec échéance aujourd'hui", dueTodayLoans.size());
                    }
                    return null;
                })
                .onFailure().invoke(throwable ->
                        LOG.errorf(throwable, "Erreur lors de la vérification des échéances")).replaceWithVoid();
    }

    // Toutes les 6 heures - Synchronisation des statuts de relances
    @Scheduled(cron = "0 0 */6 * * ?", identity = "sync-reminder-status")
    public Uni<Void> syncReminderStatus(ScheduledExecution execution) {
        LOG.debug("Synchronisation des statuts de relances");

        // Marquer comme expirées les relances sans réponse
        LocalDateTime expiredThreshold = LocalDateTime.now().minusHours(72);

        return LoanReminder.update(
                        "status = ?1 WHERE status = ?2 AND sentAt < ?3 AND acknowledged = false",
                        ReminderStatus.EXPIRED,
                        ReminderStatus.SENT,
                        expiredThreshold)
                .map(updated -> {
                    if (updated > 0) {
                        LOG.infof("Marqué %d relances comme expirées", updated);
                    }
                    return null;
                })
                .onFailure().invoke(throwable ->
                        LOG.errorf(throwable, "Erreur lors de la synchronisation des statuts")).replaceWithVoid();
    }

    // Tous les premiers du mois à 6h - Rapport mensuel
    @Scheduled(cron = "0 0 6 1 * ?", identity = "generate-monthly-report")
    public Uni<Void> generateMonthlyReport(ScheduledExecution execution) {
        LOG.info("Génération du rapport mensuel");

        return Uni.combine().all().unis(
                        flotLoanService.getTotalOutstanding(),
                        flotLoanService.getTotalOverdueAmount(),
                        flotLoanService.countActiveLoans(),
                        flotLoanService.countOverdueLoans(),
                        flotLoanService.calculateDefaultRate()
                ).asTuple().map(tuple -> {
                    LOG.infof("=== RAPPORT MENSUEL ===");
                    LOG.infof("Total en cours: %.2f€", tuple.getItem1());
                    LOG.infof("Total impayés: %.2f€", tuple.getItem2());
                    LOG.infof("Prêts actifs: %d", tuple.getItem3());
                    LOG.infof("Prêts en retard: %d", tuple.getItem4());
                    LOG.infof("Taux de défaut: %.2f%%", tuple.getItem5());
                    LOG.infof("========================");
                    return null;
                })
                .onFailure().invoke(throwable ->
                        LOG.errorf(throwable, "Erreur lors de la génération du rapport mensuel")).replaceWithVoid();
    }

    // Toutes les 30 minutes - Vérification de la santé du système
    @Scheduled(cron = "0 */30 * * * ?", identity = "health-check")
    public Uni<Void> healthCheck(ScheduledExecution execution) {
        LOG.debug("Vérification de la santé du système");

        return Uni.combine().all().unis(
                        FlotLoan.count("status = ?1", LoanStatus.ACTIVE),
                        LoanReminder.count("status = ?1 AND sentAt > ?2",
                                ReminderStatus.FAILED, LocalDateTime.now().minusHours(1))
                ).asTuple().map(tuple -> {
                    long activeLoans = tuple.getItem1();
                    long failedReminders = tuple.getItem2();

                    // Alertes si nécessaire
                    if (activeLoans == 0) {
                        LOG.warn("ALERTE: Aucun prêt actif trouvé!");
                    }

                    if (failedReminders > 10) {
                        LOG.warnf("ALERTE: %d relances ont échoué dans la dernière heure", failedReminders);
                    }

                    return null;
                })
                .onFailure().invoke(throwable ->
                        LOG.errorf(throwable, "Erreur lors de la vérification de santé")).replaceWithVoid();
    }

    // Toutes les 4 heures - Optimisation des performances
    @Scheduled(cron = "0 0 */4 * * ?", identity = "performance-optimization")
    public Uni<Void> performanceOptimization(ScheduledExecution execution) {
        LOG.debug("Optimisation des performances");

        // Nettoyage des données temporaires et optimisations
        // Par exemple, supprimer les anciens logs de session, caches expirés, etc.

        return Uni.createFrom().voidItem()
                .onItem().invoke(() -> LOG.debug("Optimisation des performances terminée"))
                .onFailure().invoke(throwable ->
                        LOG.errorf(throwable, "Erreur lors de l'optimisation des performances"));
    }
}